import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// 커스텀 메트릭
const errorRate = new Rate('bid_errors');
const bidDuration = new Trend('bid_duration');
const successCounter = new Counter('bid_success');
const failureCounter = new Counter('bid_failure');

// 테스트 설정 - 1000명 동시 입찰 시뮬레이션
export const options = {
    stages: [
        { duration: '10s', target: 100 },   // 워밍업: 10초 동안 100명까지 증가
        { duration: '20s', target: 500 },   // 20초 동안 500명까지 증가
        { duration: '30s', target: 1000 },  // 30초 동안 1000명까지 증가
        { duration: '1m', target: 1000 },   // 1분 동안 1000명 유지 (실제 부하 테스트)
        { duration: '30s', target: 0 },     // 30초 동안 0명으로 감소
    ],
    thresholds: {
        'http_req_duration': ['p(95)<3000', 'p(99)<5000'], // 95%는 3초, 99%는 5초 이내
        'bid_errors': ['rate<0.1'],                         // 에러율 10% 미만
        'http_req_failed': ['rate<0.1'],                    // HTTP 실패율 10% 미만
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

// 설정 변수
const BASE_URL = 'http://host.docker.internal:8080';  // Docker에서 로컬 접근
const PRODUCT_ID = __ENV.PRODUCT_ID || '1';           // 환경변수로 상품 ID 받기
const BASE_PRICE = 1000000;                           // 기본 입찰가
const PRICE_INCREMENT = 100;                          // 가격 증가 단위

// 테스트용 사용자 토큰 배열 (실제로는 로그인 API를 통해 얻어야 함)
// 여기서는 간단히 하기 위해 미리 생성된 토큰을 사용한다고 가정
const userTokens = new SharedArray('tokens', function () {
    const tokens = [];
    for (let i = 1; i <= 1000; i++) {
        // 실제로는 여기서 각 사용자별 JWT 토큰을 준비해야 함
        // 지금은 테스트를 위해 더미 토큰 사용
        tokens.push(`Bearer test_token_user_${i}`);
    }
    return tokens;
});

// VU(Virtual User) ID를 기반으로 고유한 입찰가 생성
function generateBidPrice() {
    const vuId = __VU || 1;  // Virtual User ID (1부터 시작)
    const iterationId = __ITER || 0;  // 반복 횟수
    
    // 각 VU마다 고유한 가격을 생성하여 동시성 테스트
    // VU ID와 반복 횟수를 조합하여 유니크한 가격 생성
    const uniquePrice = BASE_PRICE + (vuId * PRICE_INCREMENT) + (iterationId * 10);
    
    return uniquePrice;
}

// 메인 테스트 함수
export default function () {
    const vuId = __VU || 1;
    const bidPrice = generateBidPrice();
    
    // 입찰 요청 데이터
    const payload = JSON.stringify({
        bidPrice: bidPrice
    });
    
    // HTTP 헤더 설정
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': userTokens[vuId % userTokens.length], // 순환하여 토큰 사용
        },
        tags: { 
            name: 'bid_create',
            vu_id: vuId.toString(),
            bid_price: bidPrice.toString()
        },
        timeout: '10s',
    };
    
    // 입찰 API 호출
    const url = `${BASE_URL}/api/v1/bids/products/${PRODUCT_ID}`;
    const startTime = new Date();
    
    const response = http.post(url, payload, params);
    
    const duration = new Date() - startTime;
    
    // 응답 검증
    const success = check(response, {
        '상태 201 또는 202': (r) => r.status === 201 || r.status === 202,
        '응답시간 < 3초': (r) => r.timings.duration < 3000,
        '유효한 JSON 응답': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.resultCode && body.data;
            } catch {
                return false;
            }
        },
    });
    
    // 상세 로깅 (선택적)
    if (!success || response.status >= 400) {
        console.log(`❌ 입찰 실패 [VU: ${vuId}]`);
        console.log(`   상태 코드: ${response.status}`);
        console.log(`   입찰가: ${bidPrice}원`);
        console.log(`   응답 시간: ${duration}ms`);
        
        if (response.status === 400 || response.status === 409) {
            try {
                const body = JSON.parse(response.body);
                console.log(`   실패 사유: ${body.msg || '알 수 없음'}`);
            } catch {
                console.log(`   응답 본문: ${response.body}`);
            }
        }
    } else {
        console.log(`✅ 입찰 성공 [VU: ${vuId}] - 가격: ${bidPrice}원, 응답시간: ${duration}ms`);
    }
    
    // 메트릭 기록
    errorRate.add(!success);
    bidDuration.add(duration);
    
    if (success) {
        successCounter.add(1);
    } else {
        failureCounter.add(1);
    }
    
    // 실제 사용자 행동 시뮬레이션 (입찰 후 잠시 대기)
    sleep(Math.random() * 2 + 1);  // 1~3초 대기
}

// 테스트 시작 전 실행
export function setup() {
    console.log('===================================');
    console.log('🏁 1000명 동시 입찰 테스트 시작');
    console.log(`📦 상품 ID: ${PRODUCT_ID}`);
    console.log(`💰 기본 입찰가: ${BASE_PRICE.toLocaleString()}원`);
    console.log(`📈 가격 증가 단위: ${PRICE_INCREMENT}원`);
    console.log('===================================\n');
    
    // 상품 상태 확인 (선택적)
    const checkUrl = `${BASE_URL}/api/v1/bids/products/${PRODUCT_ID}`;
    const response = http.get(checkUrl);
    
    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            if (body.data) {
                console.log('📊 현재 입찰 현황:');
                console.log(`   현재가: ${body.data.currentPrice?.toLocaleString() || 'N/A'}원`);
                console.log(`   입찰 수: ${body.data.bidCount || 0}건`);
                console.log(`   최고 입찰자: ${body.data.highestBidderNickname || 'N/A'}\n`);
            }
        } catch (e) {
            console.log('⚠️ 상품 정보를 파싱할 수 없습니다.\n');
        }
    } else {
        console.log('⚠️ 상품 정보를 가져올 수 없습니다. 계속 진행합니다.\n');
    }
    
    return { startTime: new Date().toISOString() };
}

// 테스트 종료 후 실행
export function teardown(data) {
    console.log('\n===================================');
    console.log('🏁 테스트 종료');
    console.log(`⏱️ 시작 시간: ${data.startTime}`);
    console.log(`⏱️ 종료 시간: ${new Date().toISOString()}`);
    
    // 최종 상품 상태 확인
    const checkUrl = `${BASE_URL}/api/v1/bids/products/${PRODUCT_ID}`;
    const response = http.get(checkUrl);
    
    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            if (body.data) {
                console.log('\n📊 최종 입찰 현황:');
                console.log(`   최종가: ${body.data.currentPrice?.toLocaleString() || 'N/A'}원`);
                console.log(`   총 입찰 수: ${body.data.bidCount || 0}건`);
                console.log(`   최종 낙찰자: ${body.data.highestBidderNickname || 'N/A'}`);
            }
        } catch (e) {
            console.log('⚠️ 최종 상태를 파싱할 수 없습니다.');
        }
    }
    
    console.log('===================================\n');
}

// 테스트 결과 요약
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
    
    const duration = data.metrics.http_req_duration?.values || {};
    const reqs = data.metrics.http_reqs?.values || {};
    const errors = data.metrics.bid_errors?.values || {};
    const success = data.metrics.bid_success?.values || {};
    const failure = data.metrics.bid_failure?.values || {};
    
    console.log('\n=== 🎯 1000명 동시 입찰 테스트 결과 ===');
    console.log('\n📊 성능 지표:');
    console.log(`   평균 응답시간: ${duration.avg?.toFixed(2) || 'N/A'}ms`);
    console.log(`   중간값 응답시간: ${duration.med?.toFixed(2) || 'N/A'}ms`);
    console.log(`   P90 응답시간: ${duration['p(90)']?.toFixed(2) || 'N/A'}ms`);
    console.log(`   P95 응답시간: ${duration['p(95)']?.toFixed(2) || 'N/A'}ms`);
    console.log(`   P99 응답시간: ${duration['p(99)']?.toFixed(2) || 'N/A'}ms`);
    console.log(`   최대 응답시간: ${duration.max?.toFixed(2) || 'N/A'}ms`);
    
    console.log('\n📈 처리량:');
    console.log(`   총 요청 수: ${reqs.count || 0}건`);
    console.log(`   성공한 입찰: ${success.count || 0}건`);
    console.log(`   실패한 입찰: ${failure.count || 0}건`);
    console.log(`   에러율: ${errors.rate ? (errors.rate * 100).toFixed(2) : '0.00'}%`);
    
    const successRate = success.count && (success.count + failure.count) 
        ? (success.count / (success.count + failure.count) * 100).toFixed(2)
        : '0.00';
    console.log(`   성공률: ${successRate}%`);
    
    console.log('\n✅ 테스트 통과 기준:');
    console.log(`   P95 < 3초: ${duration['p(95)'] < 3000 ? '✅ PASS' : '❌ FAIL'}`);
    console.log(`   P99 < 5초: ${duration['p(99)'] < 5000 ? '✅ PASS' : '❌ FAIL'}`);
    console.log(`   에러율 < 10%: ${errors.rate < 0.1 ? '✅ PASS' : '❌ FAIL'}`);
    
    console.log('\n========================================\n');
    
    // 결과를 파일로 저장
    return {
        'stdout': JSON.stringify(data, null, 2),
        [`results/bid-concurrent-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
        'results/bid-concurrent-test-latest.json': JSON.stringify(data, null, 2),
    };
}
