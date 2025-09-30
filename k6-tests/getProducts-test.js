import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const searchDuration = new Trend('search_duration');

// 테스트 설정
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // 워밍업: 10명
        { duration: '1m', target: 30 },    // 30명으로 증가
        { duration: '2m', target: 30 },    // 30명 유지
        { duration: '30s', target: 0 },    // 종료
    ],
    thresholds: {
        'http_req_duration': ['p(95)<1000', 'p(99)<2000'],
        'errors': ['rate<0.05'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = 'http://host.docker.internal:8080';  // Docker에서 로컬 접근

// 다양한 검색 시나리오
const scenarios = [
    { name: '키워드_일반', params: `keyword=${encodeURIComponent('아이폰')}` },
    { name: '키워드_희소', params: `keyword=${encodeURIComponent('MacBook Pro M3')}` },
    { name: '카테고리', params: 'category=1' },
    { name: '지역', params: `location=${encodeURIComponent('서울')}` },
    { name: '복합검색', params: `keyword=${encodeURIComponent('아이폰')}&category=1&location=${encodeURIComponent('서울')}` },
    { name: '페이징', params: 'page=5&size=20' },
];

export default function() {
    // 랜덤하게 시나리오 선택
    const scenario = scenarios[Math.floor(Math.random() * scenarios.length)];
    const url = `${BASE_URL}/api/v1/products?${scenario.params}`;

    const startTime = new Date();
    const response = http.get(url, {
        tags: { scenario: scenario.name },
        timeout: '10s',
    });
    const duration = new Date() - startTime;

    // 400 에러 상세 정보 출력
    if (response.status === 400) {
        console.log(`\n❌ 400 에러 상세 정보:`);
        console.log(`시나리오: ${scenario.name}`);
        console.log(`URL: ${url}`);
        console.log(`응답 본문: ${response.body}`);
        console.log(`응답 헤더: ${JSON.stringify(response.headers)}\n`);
    }

    // 응답 검증
    const success = check(response, {
        '상태 200': (r) => r.status === 200,
        '응답시간 < 2초': (r) => r.timings.duration < 2000,
        '데이터 존재': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.content !== undefined;
            } catch {
                return false;
            }
        },
    });

    if (!success) {
        console.log(`❌ 실패: ${scenario.name} - Status: ${response.status}`);
    }

    // 메트릭 기록
    errorRate.add(!success);
    searchDuration.add(duration);

    // 사용자 행동 시뮬레이션
    sleep(Math.random() * 2 + 1);  // 1~3초 대기
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);

    const duration = data.metrics.http_req_duration?.values || {};
    const reqs = data.metrics.http_reqs?.values || {};
    const errors = data.metrics.errors?.values || {};

    console.log('\n=== 현재 상태 성능 테스트 결과 ===');
    console.log(`평균 응답시간: ${duration.avg?.toFixed(2) || 'N/A'}ms`);
    console.log(`P95 응답시간: ${duration['p(95)']?.toFixed(2) || 'N/A'}ms`);
    console.log(`P99 응답시간: ${duration['p(99)']?.toFixed(2) || 'N/A'}ms`);
    console.log(`최대 응답시간: ${duration.max?.toFixed(2) || 'N/A'}ms`);
    console.log(`총 요청 수: ${reqs.count || 0}`);
    console.log(`에러율: ${errors.rate ? (errors.rate * 100).toFixed(2) : '0.00'}%`);

    return {
        'stdout': JSON.stringify(data, null, 2),
        [`results/getProducts-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
        'results/getProducts-test-latest.json': JSON.stringify(data, null, 2),  // 항상 최신 결과
    };
}