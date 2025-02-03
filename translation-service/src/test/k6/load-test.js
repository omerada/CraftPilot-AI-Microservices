import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

const errorRate = new Rate("errors");

export let options = {
  stages: [
    { duration: "1m", target: 50 }, // Ramp up to 50 users
    { duration: "3m", target: 50 }, // Stay at 50 users for 3 minutes
    { duration: "1m", target: 100 }, // Ramp up to 100 users
    { duration: "3m", target: 100 }, // Stay at 100 users for 3 minutes
    { duration: "1m", target: 0 }, // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"], // 95% of requests must complete below 500ms
    "http_req_duration{type:translate}": ["p(95)<1000"], // 95% of translate requests must complete below 1s
    "http_req_duration{type:detect}": ["p(95)<200"], // 95% of detect requests must complete below 200ms
    errors: ["rate<0.1"], // Error rate must be less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8084";
const SLEEP_DURATION = __ENV.SLEEP_DURATION || "1";

const testText = "Hello, world! This is a test message.";

export function setup() {
  const detectPayload = {
    text: testText,
  };

  const detectRes = http.post(
    `${BASE_URL}/api/v1/translations/detect`,
    JSON.stringify(detectPayload),
    {
      headers: { "Content-Type": "application/json" },
      tags: { type: "detect" },
    }
  );

  check(detectRes, {
    "detect status is 200": (r) => r.status === 200,
  });

  return { sourceLanguage: detectRes.json().language };
}

export default function (data) {
  const translatePayload = {
    text: testText,
    sourceLanguage: data.sourceLanguage,
    targetLanguage: "tr",
    provider: "AUTO", // AUTO, OPENAI, GOOGLE, AZURE
  };

  const translateRes = http.post(
    `${BASE_URL}/api/v1/translations/translate`,
    JSON.stringify(translatePayload),
    {
      headers: { "Content-Type": "application/json" },
      tags: { type: "translate" },
    }
  );

  check(translateRes, {
    "translate status is 200": (r) => r.status === 200,
    "translate response has translation": (r) =>
      JSON.parse(r.body).translation !== undefined,
  });

  if (
    !check(translateRes, {
      "translate request successful": (r) => r.status === 200,
    })
  ) {
    errorRate.add(1);
  }

  sleep(Number(SLEEP_DURATION));
}

export function teardown(data) {
  // No cleanup needed
}
