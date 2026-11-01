// k6 冒烟压测脚本：对存活探针做恒定 VU 压测，用阈值卡住 P95 与失败率。
//
// 本地运行：
//   k6 run \
//     -e PERF_BASE_URL=http://127.0.0.1:8091 \
//     -e PERF_HEALTH_PATH=/actuator/health/liveness \
//     docs/assets/ci/perf/smoke.js
//
// 说明：
//   1. 默认只打探针，不写业务数据，适合每次 MR 都跑；
//   2. 需要压业务读写接口时复制本脚本为独立文件，补登录/租户头，不要改默认阈值；
//   3. 阈值来自环境变量，任一不满足 k6 以非 0 退出码结束，流水线即失败。

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = (__ENV.PERF_BASE_URL || 'http://127.0.0.1:8091').replace(/\/+$/, '');
const HEALTH_PATH = __ENV.PERF_HEALTH_PATH || '/actuator/health/liveness';
const VUS = Number(__ENV.PERF_VUS || 20);
const DURATION = __ENV.PERF_DURATION || '60s';
const P95_MS = Number(__ENV.PERF_P95_MS || 300);
const FAIL_RATE = Number(__ENV.PERF_FAIL_RATE || 0.01);
const THINK_TIME_SECONDS = Number(__ENV.PERF_THINK_TIME || 0.5);

export const options = {
    scenarios: {
        smoke: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
            gracefulStop: '10s',
        },
    },
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: [`rate<${FAIL_RATE}`],
        http_req_duration: [`p(95)<${P95_MS}`],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

export default function () {
    const res = http.get(`${BASE_URL}${HEALTH_PATH}`, {
        tags: { endpoint: 'probe' },
    });
    check(res, {
        'probe returns 200': (r) => r.status === 200,
    });
    sleep(THINK_TIME_SECONDS);
}
