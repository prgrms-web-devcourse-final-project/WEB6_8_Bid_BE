FROM docker.elastic.co/elasticsearch/elasticsearch:9.1.3

# Nori 플러그인 설치
RUN bin/elasticsearch-plugin install --batch analysis-nori