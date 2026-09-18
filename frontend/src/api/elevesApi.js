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

export const createEleve = (data) => apiRequest("/eleves", { method: "POST", body: data });
export const updateEleve = (id, data) => apiRequest(`/eleves/${id}`, { method: "PUT", body: data });
export const deleteEleve = (id) => apiRequest(`/eleves/${id}`, { method: "DELETE" });
export const uploadElevePhoto = (id, file) => {
  const body = new FormData();
  body.append("file", file);
  return apiRequest(`/eleves/${id}/photo`, { method: "POST", body });
};
export const deleteElevePhoto = (id) => apiRequest(`/eleves/${id}/photo`, { method: "DELETE" });
export const getElevePhoto = (id, signal) => apiRequest(`/eleves/${id}/photo`, { signal, responseType: "blob" });
