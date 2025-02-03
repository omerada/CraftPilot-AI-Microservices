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
    "http_req_duration{type:createUser}": ["p(95)<1000"],
    "http_req_duration{type:getUser}": ["p(95)<200"],
    errors: ["rate<0.1"], // Error rate should be less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const TEST_TOKEN = __ENV.TEST_TOKEN || "test-token";

export default function () {
  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${TEST_TOKEN}`,
    },
  };

  // Create User
  let createUserResponse = http.post(`${BASE_URL}/api/users`, null, {
    ...params,
    tags: { type: "createUser" },
  });

  check(createUserResponse, {
    "Create User status is 201": (r) => r.status === 201,
  }) || errorRate.add(1);

  if (createUserResponse.status === 201) {
    const userId = JSON.parse(createUserResponse.body).id;

    // Get User
    let getUserResponse = http.get(`${BASE_URL}/api/users/${userId}`, {
      ...params,
      tags: { type: "getUser" },
    });

    check(getUserResponse, {
      "Get User status is 200": (r) => r.status === 200,
      "Get User returns correct data": (r) => {
        let user = JSON.parse(r.body);
        return user.id === userId;
      },
    }) || errorRate.add(1);

    // Update User
    const updates = {
      displayName: "Updated Name",
      username: "updateduser",
    };

    let updateUserResponse = http.put(
      `${BASE_URL}/api/users/${userId}`,
      JSON.stringify(updates),
      { ...params, tags: { type: "updateUser" } }
    );

    check(updateUserResponse, {
      "Update User status is 200": (r) => r.status === 200,
      "Update User returns updated data": (r) => {
        let user = JSON.parse(r.body);
        return user.displayName === updates.displayName;
      },
    }) || errorRate.add(1);

    // Update User Status
    let updateStatusResponse = http.put(
      `${BASE_URL}/api/users/${userId}/status?status=INACTIVE`,
      null,
      { ...params, tags: { type: "updateStatus" } }
    );

    check(updateStatusResponse, {
      "Update Status is 200": (r) => r.status === 200,
      "Status is updated": (r) => {
        let user = JSON.parse(r.body);
        return user.status === "INACTIVE";
      },
    }) || errorRate.add(1);
  }

  sleep(1);
}
