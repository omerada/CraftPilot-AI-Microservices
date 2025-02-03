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
    http_req_duration: ["p(95)<500"], // 95% of requests should complete within 500ms
    "http_req_duration{type:addCredits}": ["p(95)<1000"],
    "http_req_duration{type:getCredits}": ["p(95)<200"],
    errors: ["rate<0.1"], // Error rate should be less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8086";
const TEST_TOKEN = __ENV.TEST_TOKEN || "test-token";

export default function () {
  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${TEST_TOKEN}`,
    },
  };

  // Add Credits
  const creditRequest = {
    userId: "test-user-" + Date.now(),
    amount: 100,
    currency: "USD",
    source: "PURCHASE",
  };

  let addResponse = http.post(
    `${BASE_URL}/api/credits`,
    JSON.stringify(creditRequest),
    { ...params, tags: { type: "addCredits" } }
  );

  check(addResponse, {
    "Add Credits status is 201": (r) => r.status === 201,
  }) || errorRate.add(1);

  if (addResponse.status === 201) {
    const transactionId = JSON.parse(addResponse.body).id;

    // Get Credits
    let getResponse = http.get(
      `${BASE_URL}/api/credits/${creditRequest.userId}`,
      { ...params, tags: { type: "getCredits" } }
    );

    check(getResponse, {
      "Get Credits status is 200": (r) => r.status === 200,
      "Get Credits returns correct balance": (r) => {
        let balance = JSON.parse(r.body).balance;
        return balance >= creditRequest.amount;
      },
    }) || errorRate.add(1);

    // Get Transaction
    let transactionResponse = http.get(
      `${BASE_URL}/api/credits/transactions/${transactionId}`,
      { ...params, tags: { type: "getTransaction" } }
    );

    check(transactionResponse, {
      "Get Transaction status is 200": (r) => r.status === 200,
      "Transaction amount matches": (r) => {
        let transaction = JSON.parse(r.body);
        return transaction.amount === creditRequest.amount;
      },
    }) || errorRate.add(1);

    // Use Credits
    const useRequest = {
      userId: creditRequest.userId,
      amount: 50,
      purpose: "TEST_PURCHASE",
    };

    let useResponse = http.post(
      `${BASE_URL}/api/credits/use`,
      JSON.stringify(useRequest),
      { ...params, tags: { type: "useCredits" } }
    );

    check(useResponse, {
      "Use Credits status is 200": (r) => r.status === 200,
      "Credits were deducted": (r) => {
        let result = JSON.parse(r.body);
        return result.success === true;
      },
    }) || errorRate.add(1);
  }

  sleep(1);
}
