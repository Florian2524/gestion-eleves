export const schoolYearError = "L'année scolaire doit respecter le format AAAA-AAAA avec deux années consécutives (exemple : 2026-2027).";

export function validateSchoolYear(value) {
  if (value == null || value === "") return "";
  return value.length === 9
    && /^[0-9]{4}-[0-9]{4}$/.test(value)
    && Number(value.slice(5)) === Number(value.slice(0, 4)) + 1
    ? "" : schoolYearError;
}

export const schoolYearLabel = (value) => value ?? "Année scolaire non renseignée";
