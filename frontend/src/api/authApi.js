import { apiRequest } from "./http";

export function authenticateUser(credentials) {
  return apiRequest(
    "/auth/login",
    {
      method: "POST",
      body: credentials,
      authenticated: false,
    },
  );
}
