import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { beforeEach, expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import { PeriodesPage } from "../pages/AdministrationPages";
import { createResource, listResource, updateResource } from "../api/schoolApi";
import { schoolYearError, validateSchoolYear } from "../components/school/schoolYear";
import { schoolLabels } from "../components/school/schoolData";

vi.mock("../api/schoolApi", () => ({ listResource: vi.fn(), createResource: vi.fn(), updateResource: vi.fn(), deleteResource: vi.fn(), attachProfile: vi.fn() }));
const historical = { idPeriode: 17, libelle: "Historique", anneeScolaire: null, dateDebut: "2026-09-01", dateFin: "2026-12-31", dateDebutSaisie: null, dateFinSaisie: null, statut: "OUVERTE" };
beforeEach(() => {
  vi.clearAllMocks();
  listResource.mockResolvedValue([historical]);
  createResource.mockResolvedValue({});
  updateResource.mockResolvedValue({});
});
function page() {
  return render(<AuthContext.Provider value={{ auth: { role: "ADMIN" } }}><MemoryRouter><PeriodesPage /></MemoryRouter></AuthContext.Provider>);
}
async function openCreation() {
  page();
  const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: "Ajouter" }));
  await user.type(screen.getByLabelText("Libellé"), "Trimestre");
  await user.type(screen.getByLabelText("Début", { exact: true }), "2026-09-01");
  await user.type(screen.getByLabelText("Fin", { exact: true }), "2026-12-31");
  await user.type(screen.getByLabelText("Statut", { exact: true }), "OUVERTE");
  return user;
}

it("affiche une année renseignée et rend explicite le NULL historique", async () => {
  listResource.mockResolvedValue([historical, { ...historical, idPeriode: 20, libelle: "Année renseignée", anneeScolaire: "2026-2027" }]);
  page();
  expect(await screen.findByText("Année scolaire non renseignée")).toBeVisible();
  expect(screen.getByText("2026-2027")).toBeVisible();
});

it("crée avec une année chaîne sans déduire les dates", async () => {
  const user = await openCreation();
  await user.type(screen.getByLabelText("Année scolaire"), "2027-2028");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(createResource).toHaveBeenCalledWith("periodes", { libelle: "Trimestre", anneeScolaire: "2027-2028", dateDebut: "2026-09-01", dateFin: "2026-12-31", dateDebutSaisie: null, dateFinSaisie: null, statut: "OUVERTE" }));
});

it("envoie NULL quand aucune année n’est saisie", async () => {
  const user = await openCreation();
  expect(screen.getByLabelText("Année scolaire")).toHaveValue("");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(createResource).toHaveBeenCalledWith("periodes", expect.objectContaining({ anneeScolaire: null })));
});

it("modifie puis efface volontairement l’année sans changer les dates", async () => {
  listResource.mockResolvedValueOnce([{ ...historical, anneeScolaire: "2026-2027" }])
    .mockResolvedValue([{ ...historical, anneeScolaire: "2027-2028" }]);
  page();
  const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: "Modifier" }));
  expect(screen.getByLabelText("Année scolaire")).toHaveValue("2026-2027");
  await user.clear(screen.getByLabelText("Année scolaire"));
  await user.type(screen.getByLabelText("Année scolaire"), "2027-2028");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(updateResource).toHaveBeenCalledWith("periodes", 17, expect.objectContaining({ anneeScolaire: "2027-2028", dateDebut: historical.dateDebut, dateFin: historical.dateFin })));
  await user.click(await screen.findByRole("button", { name: "Modifier" }));
  expect(screen.getByLabelText("Année scolaire")).toHaveValue("2027-2028");
  await user.clear(screen.getByLabelText("Année scolaire"));
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(updateResource).toHaveBeenLastCalledWith("periodes", 17, expect.objectContaining({ anneeScolaire: null })));
});

it.each(["2026/2027", "2026-2028", "2026-2026", "9999-0000"])("refuse %s avant l’appel API", async (value) => {
  const user = await openCreation();
  await user.type(screen.getByLabelText("Année scolaire"), value);
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(schoolYearError);
  expect(createResource).not.toHaveBeenCalled();
});

it("affiche les détails de validation backend et conserve le formulaire", async () => {
  createResource.mockRejectedValue(Object.assign(new Error("Les données envoyées sont invalides."), { details: { validationErrors: { anneeScolaireValide: "Année refusée par le serveur" } } }));
  const user = await openCreation();
  await user.type(screen.getByLabelText("Année scolaire"), "2026-2027");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Année refusée par le serveur");
  expect(screen.getByLabelText("Année scolaire")).toHaveValue("2026-2027");
});

it("conserve les labels de période et complète leur année sans modifier les identifiants", () => {
  const labels = schoolLabels({ periodes: [historical, { ...historical, idPeriode: 20, anneeScolaire: "2026-2027" }] });
  expect(labels.periode(17)).toBe("Historique · Année scolaire non renseignée");
  expect(labels.periode(20)).toBe("Historique · 2026-2027");
});

it("valide le même format canonique et les mêmes limites que SQL et Java", () => {
  for (const value of [null, "", "0000-0001", "2026-2027", "9998-9999"]) expect(validateSchoolYear(value)).toBe("");
  for (const value of [" ", "26-27", "2026/2027", "2026-2028", "2026-2027 ", "2026-2027\n", "２０２６-２０２７", "9999-0000"]) expect(validateSchoolYear(value)).toBe(schoolYearError);
});
