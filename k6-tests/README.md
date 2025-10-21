# K6 부하 테스트 가이드

## 📝 개요
이 디렉토리는 경매 시스템의 성능 테스트를 위한 K6 스크립트를 포함하고 있습니다.

## 🎯 테스트 스크립트

### 1. bid-concurrent-test.js
**1000명 동시 입찰 성능 테스트**
- 분산락 동작 검증
- 데이터 정합성 확인
- 동시성 처리 능력 측정

### 2. getProducts-test.js
**상품 조회 API 성능 테스트**
- 다양한 검색 시나리오
- 페이징 성능 측정

### 3. getProductsByElasticsearch-test.js
**Elasticsearch 기반 상품 검색 성능 테스트**
- 전문 검색 성능
- 복합 쿼리 처리 능력

## 🚀 테스트 실행 방법

### 사전 준비
1. Docker와 Docker Compose 설치 확인
2. 애플리케이션이 실행 중인지 확인 (기본: localhost:8080)
3. 테스트용 데이터 준비 (상품, 사용자 등)

### 1000명 동시 입찰 테스트 실행

#### 기본 실행
```bash
# Docker Compose를 사용한 실행
docker-compose run --rm k6 run bid-concurrent-test.js

# 또는 직접 Docker 명령어 사용
docker run --rm -v $(pwd):/scripts \
  --add-host host.docker.internal:host-gateway \
  grafana/k6 run /scripts/bid-concurrent-test.js
```

#### 환경변수를 사용한 실행
```bash
# 상품 ID 지정
docker-compose run --rm k6 run \
  -e PRODUCT_ID=123 \
  bid-concurrent-test.js

# 기본 입찰가 변경
docker-compose run --rm k6 run \
  -e BASE_PRICE=2000000 \
  -e PRICE_INCREMENT=500 \
  bid-concurrent-test.js

# 상세 로그 활성화
docker-compose run --rm k6 run \
  -e DETAILED_LOG=true \
  bid-concurrent-test.js

# 모든 옵션 조합
docker-compose run --rm k6 run \
  -e PRODUCT_ID=1 \
  -e BASE_PRICE=1000000 \
  -e PRICE_INCREMENT=100 \
  -e DETAILED_LOG=true \
  -e USE_AUTH=false \
  bid-concurrent-test.js
```

### 테스트 시나리오 수정

#### 부하 패턴 조정
`bid-concurrent-test.js` 파일의 `stages` 섹션을 수정하여 부하 패턴을 조정할 수 있습니다:

```javascript
stages: [
    { duration: '10s', target: 100 },   // 10초 동안 100명까지
    { duration: '20s', target: 500 },   // 20초 동안 500명까지
    { duration: '30s', target: 1000 },  // 30초 동안 1000명까지
    { duration: '1m', target: 1000 },   // 1분 동안 1000명 유지
    { duration: '30s', target: 0 },     // 30초 동안 종료
]
```

#### 성능 기준 변경
`thresholds` 섹션을 수정하여 테스트 통과 기준을 조정:

```javascript
thresholds: {
    'http_req_duration': ['p(95)<3000', 'p(99)<5000'],
    'bid_errors': ['rate<0.1'],
    'http_req_failed': ['rate<0.1'],
}
```

## 📊 결과 분석

### 실시간 모니터링
테스트 실행 중 콘솔에서 실시간으로 진행 상황을 확인할 수 있습니다:
- VU (Virtual Users): 현재 활성 사용자 수
- 요청/초: 처리량
- 에러율: 실패한 요청 비율

### 최종 결과
테스트 완료 후 다음 정보가 표시됩니다:

#### 성능 지표
- **평균 응답시간**: 전체 요청의 평균 처리 시간
- **P95/P99 응답시간**: 상위 5%, 1%를 제외한 응답시간 (SLA 지표)
- **최대 응답시간**: 가장 오래 걸린 요청

#### 처리량 분석
- **총 요청 수**: 테스트 동안 발생한 전체 요청
- **성공/실패 건수**: 입찰 성공/실패 통계
- **에러율**: 전체 요청 대비 실패 비율

#### 분산락 동작 분석
- **중복 입찰 방지**: 동일 사용자의 중복 입찰 차단 검증
- **순차 처리율**: 입찰가가 순서대로 처리되는 비율
- **데이터 정합성**: 최종 상품 가격과 DB 상태 일치 여부

### 결과 파일
테스트 결과는 `results/` 디렉토리에 JSON 형식으로 저장됩니다:
- `bid-concurrent-test-latest.json`: 최신 테스트 결과
- `bid-concurrent-test-TIMESTAMP.json`: 타임스탬프별 결과

## 🔍 문제 해결

### 연결 오류
```
ERRO[0001] dial tcp: lookup host.docker.internal: no such host
```
**해결**: Docker Desktop 업데이트 또는 `--add-host` 옵션 확인

### 인증 오류
```
❌ 입찰 실패: 401 Unauthorized
```
**해결**: 
1. 테스트용 사용자 데이터 확인
2. `USE_AUTH=false`로 인증 비활성화
3. 로그인 API 엔드포인트 확인

### 상품을 찾을 수 없음
```
❌ 상품을 찾을 수 없습니다. 상품 ID를 확인하세요.
```
**해결**:
1. 데이터베이스에 테스트용 상품 생성
2. 올바른 PRODUCT_ID 환경변수 설정

## 🎯 테스트 목표 및 기준

### 성능 목표
- **P95 응답시간**: 3초 미만
- **P99 응답시간**: 5초 미만
- **에러율**: 10% 미만
- **동시 사용자**: 1000명 처리 가능

### 데이터 정합성
- 모든 입찰가는 고유해야 함 (중복 없음)
- 상품 최종가 = DB 최고 입찰가
- 입찰 순서 보장 (분산락 동작 확인)

## 📈 성능 개선 가이드

### 측정된 병목 지점
1. **DB 커넥션 풀**: 동시 요청 증가 시 커넥션 부족
2. **Redis 분산락**: 락 대기 시간으로 인한 지연
3. **Kafka 처리량**: Consumer 처리 속도

## 🤝 기여하기
테스트 시나리오 개선이나 버그 발견 시 이슈를 생성해주세요.

