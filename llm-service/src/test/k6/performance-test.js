import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

const errorRate = new Rate("errors");

export let options = {
  stages: [
    { duration: "30s", target: 10 }, // Ramp up to 10 users
    { duration: "1m", target: 10 }, // Stay at 10 users for 1 minute
    { duration: "30s", target: 0 }, // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ["p(95)<3000"], // 95% of requests should complete within 3s
    "http_req_duration{type:analyze}": ["p(95)<5000"], // Analysis is allowed to take longer
    "http_req_duration{type:suggestions}": ["p(95)<3000"],
    "http_req_duration{type:history}": ["p(95)<1000"],
    errors: ["rate<0.1"], // Error rate should be less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const TEST_URL = __ENV.TEST_URL || "https://www.example.com";

export default function () {
  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  // Performance Analysis
  let analyzeResponse = http.post(
    `${BASE_URL}/api/performance/analyze`,
    JSON.stringify({ url: TEST_URL }),
    { ...params, tags: { type: "analyze" } }
  );

  check(analyzeResponse, {
    "Analyze status is 200": (r) => r.status === 200,
    "Analyze returns performance score": (r) => {
      const body = JSON.parse(r.body);
      return typeof body.performance === "number";
    },
  }) || errorRate.add(1);

  if (analyzeResponse.status === 200) {
    const analysisData = JSON.parse(analyzeResponse.body);

    // Generate Suggestions
    let suggestionsResponse = http.post(
      `${BASE_URL}/api/performance/suggestions`,
      JSON.stringify({ analysisData: analysisData }),
      { ...params, tags: { type: "suggestions" } }
    );

    check(suggestionsResponse, {
      "Suggestions status is 200": (r) => r.status === 200,
      "Suggestions returns content": (r) => {
        const body = JSON.parse(r.body);
        return typeof body.content === "string" && body.content.length > 0;
      },
    }) || errorRate.add(1);

    // Performance History
    let historyResponse = http.post(
      `${BASE_URL}/api/performance/history`,
      JSON.stringify({ url: TEST_URL }),
      { ...params, tags: { type: "history" } }
    );

    check(historyResponse, {
      "History status is 200": (r) => r.status === 200,
      "History returns array": (r) => {
        const body = JSON.parse(r.body);
        return Array.isArray(body.history);
      },
    }) || errorRate.add(1);
  }

  sleep(1);
}
