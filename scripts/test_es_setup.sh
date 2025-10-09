#!/bin/bash

# =========================================================
# 1. 변수 설정 (Terraform 변수 대체)
# =========================================================
PASSWORD_1="1234"
# Docker network이 없으면 생성 (이미 있을 경우 에러 무시)
docker network create common 2>/dev/null || true

# =========================================================
# 2. Elasticsearch 로직 (user_data에서 발췌)
# =========================================================

# 디렉토리 생성
mkdir -p /tmp/dockerProjects/elasticsearch_1/volumes/plugins
mkdir -p /tmp/bid_es_data

# Dockerfile 생성
echo "Dockerfile 생성 중..."
cat << 'ES_DOCKERFILE' > /tmp/Dockerfile-ES
FROM docker.elastic.co/elasticsearch/elasticsearch:9.1.3
# Nori 플러그인을 설치합니다.
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch analysis-nori
ES_DOCKERFILE

# 이미지 빌드: 로컬에서 Nori가 설치된 새로운 이미지 생성
echo "Nori 플러그인 이미지를 로컬에서 빌드 중..."
# 빌드 컨텍스트를 /tmp로 설정하고 Dockerfile을 사용합니다.
docker build -t elasticsearch-nori-local:9.1.3 -f /tmp/Dockerfile-ES /tmp

# 기존 컨테이너가 있다면 삭제
docker rm -f elasticsearch_1 2>/dev/null

# Elasticsearch 설치 및 실행 (로컬 빌드 이미지 사용)
echo "Elasticsearch 컨테이너 실행 중..."
docker run -d \
  --name elasticsearch_1 \
  --restart unless-stopped \
  --network common \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=true" \
  -e "xpack.security.transport.ssl.enabled=false" \
  -e "xpack.security.http.ssl.enabled=false" \
  -e "ELASTIC_PASSWORD=${PASSWORD_1}" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -e TZ=Asia/Seoul \
  -v /tmp/bid_es_data:/usr/share/elasticsearch/data \
  -v /tmp/dockerProjects/elasticsearch_1/volumes/plugins:/usr/share/elasticsearch/plugins \
  elasticsearch-nori-local:9.1.3

# =========================================================
# 3. 상태 확인 및 Nori 플러그인 확인
# =========================================================

echo "Elasticsearch가 시작될 때까지 대기 중..."
until docker exec elasticsearch_1 curl -s -u elastic:${PASSWORD_1} http://localhost:9200/_cluster/health | grep -q '"status":"'; do
  echo "Elasticsearch 아직 준비 안됨. 10초 후 재시도..."
  sleep 10
done
echo "Elasticsearch 준비됨. 상태 확인 완료!"

# Nori 플러그인이 설치되었는지 최종 확인
echo "Nori 플러그인 설치 최종 확인:"
docker exec elasticsearch_1 bin/elasticsearch-plugin list

# 테스트 완료 후 정리
docker rm -f elasticsearch_1
docker rmi elasticsearch-nori-local:9.1.3
