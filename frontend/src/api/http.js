import {
  clearStoredAuth,
  getAccessToken,
} from "./authStorage";

const configuredBaseUrl =
  import.meta.env.VITE_API_URL?.trim();

const apiBaseUrl =
  (configuredBaseUrl || "/api")
    .replace(/\/+$/, "");

export class ApiError extends Error {
  constructor(
    message,
    status,
    details = null,
  ) {
    super(message);

    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

async function parseResponseBody(response) {
  if (response.status === 204) {
    return null;
  }

  const responseText =
    await response.text();

  if (!responseText) {
    return null;
  }

  const contentType =
    response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    try {
      return JSON.parse(responseText);
    } catch {
      return responseText;
    }
  }

  return responseText;
}

export async function apiRequest(
  path,
  {
    method = "GET",
    body,
    headers,
    authenticated = true,
    signal,
  } = {},
) {
  const requestHeaders =
    new Headers(headers);

  requestHeaders.set(
    "Accept",
    "application/json",
  );

  let requestBody = body;

  if (
    body !== undefined &&
    body !== null &&
    !(body instanceof FormData)
  ) {
    requestHeaders.set(
      "Content-Type",
      "application/json",
    );

    requestBody = JSON.stringify(body);
  }

  if (authenticated) {
    const token = getAccessToken();

    if (token) {
      requestHeaders.set(
        "Authorization",
        `Bearer ${token}`,
      );
    }
  }

  const response = await fetch(
    `${apiBaseUrl}${path}`,
    {
      method,
      headers: requestHeaders,
      body: requestBody,
      signal,
    },
  );

  const responseBody =
    await parseResponseBody(response);

  if (!response.ok) {
    if (
      authenticated &&
      response.status === 401
    ) {
      clearStoredAuth();

      window.dispatchEvent(
        new Event("auth:unauthorized"),
      );
    }

    const message =
      responseBody?.message ||
      `La requête a échoué avec le code ${response.status}.`;

    throw new ApiError(
      message,
      response.status,
      responseBody,
    );
  }

  return responseBody;
}
