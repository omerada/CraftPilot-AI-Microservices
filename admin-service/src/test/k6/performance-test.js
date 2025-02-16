import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "1m", target: 20 },
    { duration: "3m", target: 100 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"],
  },
};

export default function () {
  const BASE_URL = __ENV.BASE_URL || "http://admin-service:8063";

  const responses = http.batch([
    ["GET", `${BASE_URL}/actuator/health`],
    ["GET", `${BASE_URL}/admin/status`],
  ]);

  check(responses[0], {
    "health check status is 200": (r) => r.status === 200,
  });

  sleep(1);
}
