import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import { ClassesPage, MatieresPage, PeriodesPage, EnseignantsPage, ResponsablesPage, EnseignementsPage, PonderationsPage, InscriptionsPage, ScolaritesPage } from "../pages/AdministrationPages";
import { createResource, deleteResource, listResource, updateResource, attachProfile } from "../api/schoolApi";
import { getEleves } from "../api/elevesApi";

vi.mock("../api/schoolApi", () => ({ listResource: vi.fn(), createResource: vi.fn(), updateResource: vi.fn(), deleteResource: vi.fn(), attachProfile: vi.fn() }));
vi.mock("../api/elevesApi", () => ({ getEleves: vi.fn() }));
let records;
const modules = [
  ["Classes", ClassesPage, "6A"], ["Matières", MatieresPage, "Mathématiques"], ["Périodes", PeriodesPage, "Trimestre 1"],
  ["Enseignants", EnseignantsPage, "Alex Martin"], ["Responsables légaux", ResponsablesPage, "Camille Dubois"],
  ["Enseignements", EnseignementsPage, "Mathématiques"], ["Pondérations", PonderationsPage, "6A · 2026-2027"],
  ["Inscriptions", InscriptionsPage, "Léa Étienne"], ["Scolarités et affectations", ScolaritesPage, "Léa Étienne"],
];
function page(Component, role = "ADMIN") {
  return render(<AuthContext.Provider value={{ auth: { role }, logout: vi.fn() }}><MemoryRouter><Component /></MemoryRouter></AuthContext.Provider>);
}
function deferred() {
  let reject, resolve;
  const promise = new Promise((done, fail) => { resolve = done; reject = fail; });
  return { promise, resolve, reject };
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.spyOn(window, "confirm").mockReturnValue(true);
  records = {
    classes: [{ idClasse: 5, nom: "6A", niveau: "6e", anneeScolaire: "2026-2027", idProfesseurPrincipal: 1 }],
    matieres: [{ idMatiere: 40, code: "MATH", nom: "Mathématiques" }],
    periodes: [{ idPeriode: 30, libelle: "Trimestre 1", anneeScolaire: null, dateDebut: "2026-09-01", dateFin: "2026-12-01", dateDebutSaisie: null, dateFinSaisie: null, statut: "OUVERTE" }],
    enseignants: [{ idPersonne: 1, prenom: "Alex", nom: "Martin", emailContact: "alex@example.test", telephone: "0601020304", numeroEmploye: "ENS-1" }],
    responsables: [{ idPersonne: 2, prenom: "Camille", nom: "Dubois", profession: "Architecte" }],
    personnes: [{ idPersonne: 1, prenom: "Alex", nom: "Martin", profils: ["ENSEIGNANT"] }, { idPersonne: 2, prenom: "Camille", nom: "Dubois", profils: ["RESPONSABLE"] }],
    enseignements: [{ idEnseignement: 20, idEnseignant: 1, idClasse: 5, idMatiere: 40, coefficientMatiere: 2, dateDebut: "2026-09-01", dateFin: null, actif: true }],
    ponderations: [{ idClasse: 5, idMatiere: 40, coefficientMatiere: 2 }],
    inscriptions: [{ idInscription: 60, idEleve: 11, dateInscription: "2026-09-01", dateFin: null, statut: "ACTIVE" }],
    scolarites: [{ idScolarite: 70, idEleve: 11, idClasse: 5, dateDebut: "2026-09-01", dateFin: null, statut: "ACTIVE" }],
  };
  getEleves.mockResolvedValue([{ idPersonne: 11, prenom: "Léa", nom: "Étienne" }]);
  listResource.mockImplementation((resource) => Promise.resolve(records[resource] ?? []));
  createResource.mockResolvedValue({}); updateResource.mockResolvedValue({}); deleteResource.mockResolvedValue({}); attachProfile.mockResolvedValue({});
});
afterEach(() => vi.restoreAllMocks());

describe("présentation et droits", () => {
  it.each(modules)("présente %s avec un titre de fiche et les actions ADMIN", async (title, Component, heading) => {
    page(Component); expect(screen.getByRole("heading", { level: 1, name: title })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { level: 2, name: heading })).toBeInTheDocument();
    for (const action of ["Ajouter", "Modifier", "Supprimer"]) expect(screen.getByRole("button", { name: action })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Tableau de bord" })).toHaveAttribute("href", "/espace");
  });
  it.each(["ENSEIGNANT", "RESPONSABLE"])("ne propose aucune écriture dans les neuf modules pour %s", async (role) => {
    for (const [, Component, heading] of modules) {
      const view = page(Component, role); await screen.findByRole("heading", { level: 2, name: heading });
      expect(screen.queryByRole("button", { name: /^(Ajouter|Modifier|Supprimer)$/ })).not.toBeInTheDocument();
      expect(screen.queryByRole("form")).not.toBeInTheDocument(); view.unmount();
    }
  });
  it.each(["ENSEIGNANT", "RESPONSABLE"])("préserve la lecture limitée des enseignements pour %s", async (role) => {
    page(EnseignementsPage, role); await screen.findByRole("heading", { level: 2, name: "Mathématiques" });
    expect(within(screen.getByRole("article")).queryByText("Alex Martin")).not.toBeInTheDocument();
    expect(listResource).not.toHaveBeenCalledWith("enseignants", expect.anything());
    expect(screen.queryByText(/Modifiez le coefficient commun/)).not.toBeInTheDocument();
  });
  it("explicite les données manquantes sans inventer de valeurs", async () => {
    records.classes[0].idProfesseurPrincipal = null; page(ClassesPage);
    expect(await screen.findByText("Aucun")).toBeInTheDocument();
  });
});

describe("formulaires et payloads", () => {
  it("crée une classe avec son professeur principal et ses champs obligatoires", async () => {
    const user = userEvent.setup(); page(ClassesPage); await user.click(screen.getByRole("button", { name: "Ajouter" }));
    const form = await screen.findByRole("form", { name: "Classe" });
    for (const label of ["Nom", "Niveau", "Année scolaire"]) expect(within(form).getByLabelText(label)).toBeRequired();
    expect(screen.getByLabelText("Professeur principal")).not.toBeRequired();
    await user.type(screen.getByLabelText("Nom"), "5A"); await user.type(screen.getByLabelText("Niveau"), "5e"); await user.type(screen.getByLabelText("Année scolaire"), "2026-2027");
    await user.selectOptions(screen.getByLabelText("Professeur principal"), "1"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(createResource).toHaveBeenCalledWith("classes", { nom: "5A", niveau: "5e", anneeScolaire: "2026-2027", idProfesseurPrincipal: 1 }));
  });
  it("conserve la validation obligatoire et la modification d’une matière", async () => {
    const user = userEvent.setup(); page(MatieresPage); await user.click(screen.getByRole("button", { name: "Ajouter" }));
    const form = await screen.findByRole("form", { name: "Matière" }); fireEvent.submit(form);
    expect(screen.getByRole("alert")).toHaveTextContent("Renseignez tous les champs obligatoires."); expect(createResource).not.toHaveBeenCalled();
    await user.click(screen.getByRole("button", { name: "Annuler" })); await user.click(screen.getByRole("button", { name: "Modifier" }));
    await user.clear(screen.getByLabelText("Code")); await user.type(screen.getByLabelText("Code"), "MAT"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(updateResource).toHaveBeenCalledWith("matieres", 40, { code: "MAT", nom: "Mathématiques" }));
  });
  it.each([["Inscriptions", InscriptionsPage, "Date d'inscription", "inscriptions", "dateInscription"], ["Scolarité", ScolaritesPage, "Début", "scolarites", "dateDebut"]])("préserve les associations, les dates et les NULL de %s", async (_name, Component, dateLabel, resource, dateKey) => {
    const user = userEvent.setup(); page(Component); await user.click(screen.getByRole("button", { name: "Ajouter" })); await screen.findByRole("form");
    await user.selectOptions(screen.getByLabelText("Élève"), "11");
    if (resource === "scolarites") await user.selectOptions(screen.getByLabelText("Classe et année"), "5");
    fireEvent.change(screen.getByLabelText(dateLabel, { exact: true }), { target: { value: "2026-09-01" } }); await user.type(screen.getByLabelText("Statut"), "ACTIVE");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(createResource).toHaveBeenCalledWith(resource, { idEleve: 11, ...(resource === "scolarites" ? { idClasse: 5 } : {}), [dateKey]: "2026-09-01", dateFin: null, statut: "ACTIVE" }));
  });
  it("conserve les associations d’enseignement, le coefficient et le booléen actif", async () => {
    const user = userEvent.setup(); page(EnseignementsPage); await user.click(screen.getByRole("button", { name: "Ajouter" })); await screen.findByRole("form", { name: "Enseignement" });
    await user.selectOptions(screen.getByLabelText("Enseignant"), "1"); await user.selectOptions(screen.getByLabelText("Classe"), "5"); await user.selectOptions(screen.getByLabelText("Matière"), "40");
    fireEvent.change(screen.getByLabelText("Début", { exact: true }), { target: { value: "2026-09-01" } });
    expect(screen.getByLabelText("Actif")).toBeChecked(); await user.click(screen.getByLabelText("Actif")); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(createResource).toHaveBeenCalledWith("enseignements", { idEnseignant: 1, idClasse: 5, idMatiere: 40, coefficientMatiere: 1, dateDebut: "2026-09-01", dateFin: null, actif: false }));
  });
  it.each([[PeriodesPage, "Début", "Fin"], [EnseignementsPage, "Début", "Fin"], [InscriptionsPage, "Date d'inscription", "Fin"], [ScolaritesPage, "Début", "Fin"]])("conserve la validation de chronologie pour %s", async (Component, start, end) => {
    const user = userEvent.setup(); page(Component); await user.click(await screen.findByRole("button", { name: "Modifier" }));
    fireEvent.change(screen.getByLabelText(start, { exact: true }), { target: { value: "2026-12-01" } }); fireEvent.change(screen.getByLabelText(end, { exact: true }), { target: { value: "2026-09-01" } });
    await user.click(screen.getByRole("button", { name: "Enregistrer" })); expect(screen.getByRole("alert")).toHaveTextContent("La date de fin doit suivre la date de début."); expect(updateResource).not.toHaveBeenCalled();
  });
  it("explique les associations non modifiables d’une pondération sans modifier ses valeurs", async () => {
    const user = userEvent.setup(); page(PonderationsPage); await user.click(await screen.findByRole("button", { name: "Modifier" }));
    for (const label of ["Classe", "Matière"]) { expect(screen.getByLabelText(label)).toBeDisabled(); expect(screen.getByLabelText(label)).toHaveAccessibleDescription("Cette association ne peut pas être modifiée."); }
    expect(screen.getByLabelText("Coefficient")).toHaveValue(2); expect(screen.getByLabelText("Coefficient")).toHaveAttribute("step", "0.01");
  });
  it.each([[EnseignantsPage, "Profil enseignant", "2", "ENSEIGNANT", "Numéro d'employé", "ENS-NEW", { numeroEmploye: "ENS-NEW" }], [ResponsablesPage, "Profil responsable", "1", "RESPONSABLE", "Profession", "Médecin", { profession: "Médecin" }]])("préserve le rattachement du %s sans coordonnées", async (Component, formName, id, profile, label, value, body) => {
    const user = userEvent.setup(); page(Component); await user.click(screen.getByRole("button", { name: "Ajouter" })); await screen.findByRole("form", { name: formName });
    await user.selectOptions(screen.getByLabelText("Création du profil"), "existing");
    await user.selectOptions(screen.getByLabelText("Personne existante"), id); expect(screen.queryByLabelText("Nom")).not.toBeInTheDocument();
    expect(screen.queryByRole("group", { name: "Coordonnées partagées" })).not.toBeInTheDocument();
    await user.type(screen.getByLabelText(label), value); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(attachProfile).toHaveBeenCalledWith(Number(id), profile, body)); expect(createResource).not.toHaveBeenCalled();
  });
  it("conserve les coordonnées lors de la modification d’un profil", async () => {
    const user = userEvent.setup(); page(EnseignantsPage); await user.click(await screen.findByRole("button", { name: "Modifier" }));
    expect(screen.getByText(/communs à tous les profils/)).toBeInTheDocument(); expect(screen.getByLabelText("E-mail de contact")).toHaveValue("alex@example.test");
    await user.clear(screen.getByLabelText("Numéro d'employé")); await user.type(screen.getByLabelText("Numéro d'employé"), "ENS-2"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(updateResource).toHaveBeenCalledWith("enseignants", 1, { nom: "Martin", prenom: "Alex", emailContact: "alex@example.test", telephone: "0601020304", adresse: null, numeroEmploye: "ENS-2" }));
  });
});

describe("retours et suppressions", () => {
  it("affiche le chargement et l’erreur sans fiche ni faux état vide", async () => {
    const request = deferred(); listResource.mockReturnValueOnce(request.promise); page(MatieresPage);
    expect(screen.getByRole("status")).toHaveTextContent("Chargement"); await act(async () => request.reject(new Error("Consultation impossible.")));
    expect(screen.getByRole("alert")).toHaveTextContent("Consultation impossible."); expect(screen.queryByRole("article")).not.toBeInTheDocument(); expect(screen.queryByText("Aucune donnée disponible.")).not.toBeInTheDocument();
  });
  it("présente l’état vide sans statistique inventée", async () => {
    records.matieres = []; page(MatieresPage); expect(await screen.findByRole("heading", { name: "Aucune donnée disponible." })).toBeInTheDocument(); expect(screen.getByText(/Utilisez Ajouter/)).toBeInTheDocument();
  });
  it("conserve la saisie après erreur et désactive les contrôles durant l’enregistrement", async () => {
    const request = deferred(); updateResource.mockReturnValueOnce(request.promise); const user = userEvent.setup(); page(MatieresPage); await user.click(await screen.findByRole("button", { name: "Modifier" }));
    await user.clear(screen.getByLabelText("Code")); await user.type(screen.getByLabelText("Code"), "LONG-CODE"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    expect(screen.getByLabelText("Code")).toBeDisabled(); expect(screen.getByRole("button", { name: "Enregistrement…" })).toHaveAttribute("aria-busy", "true");
    await act(async () => request.reject(Object.assign(new Error("Validation"), { details: { validationErrors: { code: "Code déjà utilisé." } } })));
    expect(screen.getByRole("alert")).toHaveTextContent("Code déjà utilisé."); expect(screen.getByLabelText("Code")).toHaveValue("LONG-CODE"); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeEnabled();
  });
  it.each([[MatieresPage, "matieres", 40], [PonderationsPage, "ponderations", "5/40"], [EnseignantsPage, "enseignants", 1]])("préserve la confirmation et l’identifiant de suppression pour %s", async (Component, resource, id) => {
    const user = userEvent.setup(); page(Component); await screen.findByRole("button", { name: "Supprimer" });
    window.confirm.mockReturnValueOnce(false); await user.click(screen.getByRole("button", { name: "Supprimer" })); expect(deleteResource).not.toHaveBeenCalled();
    await user.click(screen.getByRole("button", { name: "Supprimer" })); await waitFor(() => expect(deleteResource).toHaveBeenCalledWith(resource, id)); expect(await screen.findByText("Suppression effectuée.")).toBeInTheDocument();
    if (resource === "enseignants") expect(window.confirm).toHaveBeenCalledWith("Retirer uniquement ce profil ? La personne, ses autres profils et son compte seront conservés.");
  });
});
