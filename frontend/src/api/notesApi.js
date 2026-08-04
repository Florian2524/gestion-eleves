import { apiRequest } from "./http";

export function getNotes(
  options = {},
) {
  return apiRequest(
    "/notes",
    options,
  );
}

export function getEvaluations(
  options = {},
) {
  return apiRequest(
    "/evaluations",
    options,
  );
}

export function createNote(
  note,
  options = {},
) {
  return apiRequest(
    "/notes",
    {
      ...options,
      method: "POST",
      body: note,
    },
  );
}

export function updateNote(
  idNote,
  note,
  options = {},
) {
  return apiRequest(
    `/notes/${encodeURIComponent(idNote)}`,
    {
      ...options,
      method: "PUT",
      body: note,
    },
  );
}

export function deleteNote(
  idNote,
  options = {},
) {
  return apiRequest(
    `/notes/${encodeURIComponent(idNote)}`,
    {
      ...options,
      method: "DELETE",
    },
  );
}
