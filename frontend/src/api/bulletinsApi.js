import { apiRequest } from "./http";

function buildBulletinPath(
  idScolarite,
  idPeriode,
  suffix = "",
) {
  return (
    `/bulletins/calcul/${encodeURIComponent(idScolarite)}` +
    `/${encodeURIComponent(idPeriode)}${suffix}`
  );
}

export function getScolarites(
  options = {},
) {
  return apiRequest(
    "/scolarites",
    options,
  );
}

export function getPeriodes(
  options = {},
) {
  return apiRequest(
    "/periodes",
    options,
  );
}

export function calculateBulletin(
  idScolarite,
  idPeriode,
  options = {},
) {
  return apiRequest(
    buildBulletinPath(
      idScolarite,
      idPeriode,
    ),
    options,
  );
}

export function downloadBulletinPdf(
  idScolarite,
  idPeriode,
  options = {},
) {
  return apiRequest(
    buildBulletinPath(
      idScolarite,
      idPeriode,
      "/pdf",
    ),
    {
      ...options,
      headers: {
        Accept: "application/pdf",
      },
      responseType: "blob",
    },
  );
}
