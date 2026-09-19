import { apiRequest } from "./http";

export const listLegalLinks = (options = {}) => apiRequest("/responsabilites-legales", options);
export const createLegalLink = (body) => apiRequest("/responsabilites-legales", { method: "POST", body });
export const deleteLegalLink = (idResponsable, idEleve) => apiRequest(`/responsabilites-legales/${idResponsable}/${idEleve}`, { method: "DELETE" });
