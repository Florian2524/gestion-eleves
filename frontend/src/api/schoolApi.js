import { apiRequest } from "./http";

const paths = {
  classes: "/classes",
  matieres: "/matieres",
  periodes: "/periodes",
  enseignants: "/enseignants",
  enseignements: "/enseignements",
  evaluations: "/evaluations",
  inscriptions: "/inscriptions",
  scolarites: "/scolarites",
};

export const listResource = (resource, options = {}) => apiRequest(paths[resource], options);
export const createResource = (resource, body) => apiRequest(paths[resource], { method: "POST", body });
export const updateResource = (resource, id, body) => apiRequest(`${paths[resource]}/${id}`, { method: "PUT", body });
export const deleteResource = (resource, id) => apiRequest(`${paths[resource]}/${id}`, { method: "DELETE" });
