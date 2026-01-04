# FastAPI 트리거 리스너

이 패키지는 외부 FastAPI 서버에서 Redis를 통해 전송되는 트리거 신호를 받아 순차적으로 API를 실행하는 기능을 제공합니다.

## 구조

- **FastApiTriggerDto**: 트리거 데이터를 담는 DTO
- **FastApiTriggerListener**: Redis Pub/Sub을 통해 트리거를 수신하는 리스너
- **FastApiExecutionService**: 순차적으로 API를 실행하는 서비스
- **FastApiTriggerPublisher**: 예시용 트리거 발행 클래스 (테스트/예시 목적)

## 동작 방식

1. **트리거 수신**: `FastApiTriggerListener`가 Redis Pub/Sub 토픽 `fastapi:trigger`를 구독
2. **메시지 파싱**: 수신된 JSON 메시지를 `FastApiTriggerDto`로 변환
3. **순차 실행**: `FastApiExecutionService`가 단일 스레드 ExecutorService를 사용하여 API를 순차적으로 실행

## 설정

`application.yml`에 FastAPI 서버 URL을 설정합니다:

```yaml
app:
  fastapi:
    base-url: http://localhost:8000  # FastAPI 서버 기본 URL
```

## FastAPI 서버에서 트리거 발행 방법

FastAPI 서버(Python)에서 다음과 같이 트리거를 발행할 수 있습니다:

```python
import redis
import json
import time
import uuid

# Redis 연결
r = redis.Redis(host='localhost', port=6379, decode_responses=False)

# 트리거 데이터 구성
trigger_data = {
    "triggerType": "data_received",
    "triggerId": str(uuid.uuid4()),
    "timestamp": int(time.time() * 1000),
    "data": {
        "userId": "12345",
        "dataId": "data_001",
        "action": "process_data"
    },
    "apiEndpoints": [
        "/api/v1/process",
        "/api/v1/validate",
        "/api/v1/notify"
    ]
}

# Redis Pub/Sub으로 발행
r.publish("fastapi:trigger", json.dumps(trigger_data))
```

## 트리거 메시지 형식

```json
{
  "triggerType": "트리거 타입 (예: data_received, processing_complete)",
  "triggerId": "고유 식별자 (UUID)",
  "timestamp": 1234567890123,
  "data": {
    "key1": "value1",
    "key2": "value2"
  },
  "apiEndpoints": [
    "/api/v1/endpoint1",
    "/api/v1/endpoint2",
    "/api/v1/endpoint3"
  ]
}
```

## API 실행 순서

`apiEndpoints` 배열에 지정된 순서대로 API가 실행됩니다. 각 API는 이전 API가 완료된 후에 실행되며, 실패해도 다음 API는 계속 실행됩니다.

## API 호출 시 파라미터 전달 방법

### 1. Request Body로 전달 (기본 방식)

모든 API는 POST 방식으로 호출되며, 다음 데이터가 Request Body에 포함됩니다:

```json
{
  "triggerId": "트리거 고유 ID",
  "triggerType": "트리거 타입",
  "timestamp": 1234567890123,
  "data": {
    // FastApiTriggerDto의 data 필드에 포함된 모든 데이터
    "userId": "12345",
    "dataId": "data_001",
    "customParam1": "value1",
    "customParam2": "value2"
  }
}
```

**예시:**
```python
trigger_data = {
    "triggerType": "data_received",
    "triggerId": str(uuid.uuid4()),
    "timestamp": int(time.time() * 1000),
    "data": {
        "userId": "12345",
        "productId": "prod_001",
        "action": "process"
    },
    "apiEndpoints": [
        "/api/v1/process"
    ]
}
```

FastAPI 서버에서는 다음과 같이 받을 수 있습니다:
```python
@app.post("/api/v1/process")
async def process_data(request: dict):
    trigger_id = request.get("triggerId")
    trigger_type = request.get("triggerType")
    data = request.get("data", {})
    
    user_id = data.get("userId")
    product_id = data.get("productId")
    action = data.get("action")
    
    # 처리 로직...
```

### 2. URL 쿼리 파라미터로 전달

`apiEndpoints`에 쿼리 파라미터를 포함할 수 있습니다:

```python
trigger_data = {
    "triggerType": "data_received",
    "triggerId": str(uuid.uuid4()),
    "timestamp": int(time.time() * 1000),
    "data": {
        "userId": "12345"
    },
    "apiEndpoints": [
        "/api/v1/users?userId=12345&status=active",
        "/api/v1/process?mode=async"
    ]
}
```

이 경우 FastAPI에서는 다음과 같이 받을 수 있습니다:
```python
@app.post("/api/v1/users")
async def get_users(userId: str = Query(...), status: str = Query(None)):
    # 쿼리 파라미터로 받음
    pass

@app.post("/api/v1/process")
async def process(mode: str = Query("sync")):
    # 쿼리 파라미터로 받음
    pass
```

### 3. 경로 변수(Path Variable)로 전달

`apiEndpoints`에 경로 변수를 포함할 수 있습니다:

```python
trigger_data = {
    "triggerType": "data_received",
    "triggerId": str(uuid.uuid4()),
    "timestamp": int(time.time() * 1000),
    "data": {
        "userId": "12345"
    },
    "apiEndpoints": [
        "/api/v1/users/12345",
        "/api/v1/products/prod_001/update"
    ]
}
```

FastAPI에서는 다음과 같이 받을 수 있습니다:
```python
@app.post("/api/v1/users/{user_id}")
async def get_user(user_id: str):
    # 경로 변수로 받음
    pass

@app.post("/api/v1/products/{product_id}/update")
async def update_product(product_id: str):
    # 경로 변수로 받음
    pass
```

### 4. 복합 사용 예시

Request Body + 쿼리 파라미터 + 경로 변수를 함께 사용할 수 있습니다:

```python
trigger_data = {
    "triggerType": "order_created",
    "triggerId": str(uuid.uuid4()),
    "timestamp": int(time.time() * 1000),
    "data": {
        "orderId": "order_123",
        "amount": 10000,
        "items": ["item1", "item2"]
    },
    "apiEndpoints": [
        "/api/v1/orders/order_123/process?priority=high&async=true",
        "/api/v1/notifications/send?type=email"
    ]
}
```

FastAPI에서는 다음과 같이 받을 수 있습니다:
```python
@app.post("/api/v1/orders/{order_id}/process")
async def process_order(
    order_id: str,  # 경로 변수
    priority: str = Query(...),  # 쿼리 파라미터
    async: bool = Query(False),  # 쿼리 파라미터
    request: dict = Body(...)  # Request Body
):
    # 경로 변수: order_id
    # 쿼리 파라미터: priority, async
    # Request Body: request (triggerId, triggerType, timestamp, data 포함)
    order_data = request.get("data", {})
    pass
```

## 파라미터 전달 우선순위

1. **경로 변수**: URL 경로에 포함된 변수 (예: `/api/v1/users/{userId}`)
2. **쿼리 파라미터**: URL의 `?` 뒤에 오는 파라미터 (예: `/api/v1/users?status=active`)
3. **Request Body**: POST 요청의 본문에 포함된 JSON 데이터

## 예시 사용법

### Java에서 트리거 발행 (테스트/예시)

```java
@Autowired
private FastApiTriggerPublisher triggerPublisher;

// 데이터 수신 트리거 발행
triggerPublisher.publishDataReceivedTrigger("user123", "data001");

// 처리 완료 트리거 발행
triggerPublisher.publishProcessingCompleteTrigger("task001", "success");
```

## 프로덕션 환경 개선 사항

### Redisson 설정
- **연결 풀**: 최대 64개 연결, 최소 32개 유휴 연결 유지
- **타임아웃**: 연결 타임아웃 10초, 명령 타임아웃 3초
- **재연결**: 자동 재연결 및 Keep-Alive 활성화
- **모니터링**: 30초마다 Ping으로 연결 상태 확인

### FastApiExecutionService
- **리소스 관리**: ExecutorService를 Spring Bean으로 관리, `@PreDestroy`로 안전한 종료
- **타임아웃**: 연결 타임아웃 10초, 읽기 타임아웃 30초 (설정 가능)
- **재시도**: 네트워크 오류 시 최대 3회 재시도 (설정 가능)
- **에러 처리**: 실패해도 다음 API 계속 실행

### FastApiTriggerListener
- **재연결 로직**: 구독 실패 시 최대 5회 재시도 (지수 백오프)
- **에러 처리**: 메시지 파싱 실패 시에도 리스너는 계속 동작

## 설정

### application.yml
```yaml
app:
  fastapi:
    base-url: http://localhost:8000  # FastAPI 서버 기본 URL
    timeout:
      connect: 10   # 연결 타임아웃 (초)
      read: 30       # 읽기 타임아웃 (초)
    retry:
      max-attempts: 3  # 최대 재시도 횟수
```

## 주의사항

- 트리거는 Redis Pub/Sub을 통해 전달되므로, Spring Boot 애플리케이션이 실행 중이어야 리스너가 동작합니다
- API 실행은 단일 스레드로 순차 실행되므로, 동시에 여러 트리거가 들어와도 순차적으로 처리됩니다
- 각 API 호출은 동기적으로 실행되며, 실패해도 다음 API는 계속 실행됩니다
- **프로덕션 환경**: 24시간 운영을 위해 모든 리소스가 안전하게 관리되며, 재연결 및 재시도 로직이 포함되어 있습니다
- **파라미터 전달**: Request Body, 쿼리 파라미터, 경로 변수를 모두 사용할 수 있으며, `apiEndpoints`에 직접 포함하거나 `data` 필드를 통해 전달할 수 있습니다

