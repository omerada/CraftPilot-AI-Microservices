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
    "http_req_duration{type:sendEmail}": ["p(95)<1000"],
    "http_req_duration{type:sendPush}": ["p(95)<800"],
    "http_req_duration{type:getStatus}": ["p(95)<200"],
    errors: ["rate<0.1"], // Error rate should be less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8087";
const TEST_TOKEN = __ENV.TEST_TOKEN || "test-token";

export default function () {
  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${TEST_TOKEN}`,
    },
  };

  // Send Email Notification
  const emailRequest = {
    userId: "test-user-" + Date.now(),
    type: "EMAIL",
    template: "welcome",
    to: "test@example.com",
    data: {
      username: "testuser",
      welcomeMessage: "Welcome to CraftPilot!",
    },
  };

  let emailResponse = http.post(
    `${BASE_URL}/api/notifications/email`,
    JSON.stringify(emailRequest),
    { ...params, tags: { type: "sendEmail" } }
  );

  check(emailResponse, {
    "Send Email status is 202": (r) => r.status === 202,
  }) || errorRate.add(1);

  if (emailResponse.status === 202) {
    const notificationId = JSON.parse(emailResponse.body).id;

    // Get Notification Status
    let statusResponse = http.get(
      `${BASE_URL}/api/notifications/${notificationId}/status`,
      { ...params, tags: { type: "getStatus" } }
    );

    check(statusResponse, {
      "Get Status is 200": (r) => r.status === 200,
      "Status is valid": (r) => {
        let status = JSON.parse(r.body).status;
        return ["PENDING", "SENT", "FAILED"].includes(status);
      },
    }) || errorRate.add(1);

    // Send Push Notification
    const pushRequest = {
      userId: "test-user-" + Date.now(),
      type: "PUSH",
      title: "Test Notification",
      body: "This is a test push notification",
      data: {
        action: "OPEN_APP",
        screen: "HOME",
      },
    };

    let pushResponse = http.post(
      `${BASE_URL}/api/notifications/push`,
      JSON.stringify(pushRequest),
      { ...params, tags: { type: "sendPush" } }
    );

    check(pushResponse, {
      "Send Push status is 202": (r) => r.status === 202,
      "Push notification queued": (r) => {
        let result = JSON.parse(r.body);
        return result.status === "QUEUED";
      },
    }) || errorRate.add(1);

    // Get User Notifications
    let userNotificationsResponse = http.get(
      `${BASE_URL}/api/notifications/user/${emailRequest.userId}`,
      { ...params, tags: { type: "getUserNotifications" } }
    );

    check(userNotificationsResponse, {
      "Get User Notifications is 200": (r) => r.status === 200,
      "Notifications list is not empty": (r) => {
        let notifications = JSON.parse(r.body);
        return notifications.length > 0;
      },
    }) || errorRate.add(1);
  }

  sleep(1);
}
