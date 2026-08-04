import { apiRequest } from "./http";

export function getEleves({
  signal,
} = {}) {
  return apiRequest(
    "/eleves",
    {
      signal,
    },
  );
}
