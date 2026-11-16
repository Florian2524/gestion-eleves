import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import BulletinsPage from "../pages/BulletinsPage";
import { EvaluationsPage } from "../pages/AdministrationPages";
import { getEleves } from "../api/elevesApi";
import { calculateBulletin, downloadBulletinPdf, getPeriodes, getScolarites } from "../api/bulletinsApi";
import { createResource, deleteResource, listResource, updateResource } from "../api/schoolApi";
import { ApiError } from "../api/http";

vi.mock("../api/elevesApi", () => ({ getEleves: vi.fn() }));
vi.mock("../api/bulletinsApi", () => ({ getScolarites: vi.fn(), getPeriodes: vi.fn(), calculateBulletin: vi.fn(), downloadBulletinPdf: vi.fn() }));
vi.mock("../api/schoolApi", () => ({ listResource: vi.fn(), createResource: vi.fn(), updateResource: vi.fn(), deleteResource: vi.fn() }));

const bulletin = {
  idScolarite: 1, idPeriode: 30, prenomEleve: "Léa", nomEleve: "Étienne", matriculeEleve: "EL-11", nomClasse: "6A", niveauClasse: "6e", anneeScolaire: "2026-2027", libellePeriode: "Trimestre 1", dateDebutPeriode: "2026-09-01", dateFinPeriode: "2026-12-01", moyenneGenerale: 14.666,
  lignes: [
    { idMatiere: 40, nomMatiere: "Mathématiques", codeMatiere: "MATH", coefficientMatiere: 2.5, nombreNotes: 3, moyenneSur20: 15.126 },
    { idMatiere: 41, nomMatiere: "Français", codeMatiere: "FR", coefficientMatiere: 1, nombreNotes: 0, moyenneSur20: null },
  ],
};
const evaluation = { idEvaluation: 10, idEnseignement: 20, idPeriode: 30, libelle: "Contrôle fractions", typeEvaluation: "Contrôle", dateEvaluation: "2026-10-10", coefficientEvaluation: 1.5, bareme: 20 };
let resources;
function page(Component, role = "ADMIN") {
  return render(<AuthContext.Provider value={{ auth: { role, emailConnexion: "test@example.test" }, logout: vi.fn() }}><MemoryRouter><Component /></MemoryRouter></AuthContext.Provider>);
}
function deferred() {
  let resolve, reject;
  const promise = new Promise((done, fail) => { resolve = done; reject = fail; });
  return { promise, resolve, reject };
}
async function readyBulletin() { await waitFor(() => expect(screen.getByRole("button", { name: "Calculer" })).toBeEnabled()); }
async function calculate(user) { await readyBulletin(); await user.click(screen.getByRole("button", { name: "Calculer" })); await screen.findByRole("heading", { name: "Léa Étienne" }); }
async function fillEvaluation(user) {
  await user.click(await screen.findByRole("button", { name: "Ajouter" }));
  await screen.findByRole("form", { name: "Évaluation" });
  await user.selectOptions(screen.getByLabelText("Enseignement"), "20");
  await user.selectOptions(screen.getByLabelText("Période"), "30");
  await user.type(screen.getByLabelText("Libellé"), "Nouvelle évaluation");
  await user.type(screen.getByLabelText("Type"), "Contrôle");
  fireEvent.change(screen.getByLabelText("Date"), { target: { value: "2026-11-01" } });
}

beforeEach(() => {
  vi.resetAllMocks();
  vi.spyOn(window, "confirm").mockReturnValue(true);
  vi.stubGlobal("URL", class extends URL {
    static createObjectURL = vi.fn(() => "blob:bulletin");
    static revokeObjectURL = vi.fn();
  });
  getEleves.mockResolvedValue([{ idPersonne: 11, prenom: "Léa", nom: "Étienne", matricule: "EL-11" }, { idPersonne: 12, prenom: "Noé", nom: "Martin", matricule: "EL-12" }]);
  getScolarites.mockResolvedValue([{ idScolarite: 1, idEleve: 11, idClasse: 5, dateDebut: "2026-09-01" }, { idScolarite: 2, idEleve: 12, idClasse: 6, dateDebut: "2026-09-01" }]);
  // Deliberately unsorted to protect the existing initial period selection.
  getPeriodes.mockResolvedValue([{ idPeriode: 31, libelle: "Trimestre 2", dateDebut: "2027-01-01" }, { idPeriode: 30, libelle: "Trimestre 1", anneeScolaire: "2026-2027", dateDebut: "2026-09-01", dateFin: "2026-12-01" }]);
  calculateBulletin.mockResolvedValue(bulletin);
  downloadBulletinPdf.mockResolvedValue(new Blob(["pdf"], { type: "application/pdf" }));
  resources = {
    evaluations: [evaluation], enseignements: [{ idEnseignement: 20, idClasse: 5, idMatiere: 40, idEnseignant: 50 }],
    classes: [{ idClasse: 5, nom: "6A", anneeScolaire: "2026-2027" }, { idClasse: 6, nom: "6B", anneeScolaire: "2026-2027" }],
    matieres: [{ idMatiere: 40, nom: "Mathématiques" }], periodes: [{ idPeriode: 30, libelle: "Trimestre 1", anneeScolaire: "2026-2027" }], enseignants: [{ idPersonne: 50, prenom: "Alex", nom: "Martin" }],
  };
  listResource.mockImplementation((resource) => Promise.resolve(resources[resource] ?? []));
  createResource.mockResolvedValue({}); updateResource.mockResolvedValue({}); deleteResource.mockResolvedValue({});
});
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

describe("bulletins", () => {
  it("conserve les sélections initiales et transmet la scolarité et la période choisies", async () => {
    const user = userEvent.setup(); page(BulletinsPage); await readyBulletin();
    expect(screen.getByLabelText("Élève et scolarité")).toHaveValue("1"); expect(screen.getByLabelText("Période")).toHaveValue("30");
    expect(screen.getByRole("option", { name: /Léa.*6A.*2026-2027/ })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Élève et scolarité"), "2"); await user.selectOptions(screen.getByLabelText("Période"), "31");
    await user.click(screen.getByRole("button", { name: "Calculer" })); await waitFor(() => expect(calculateBulletin).toHaveBeenCalledWith("2", "31"));
  });
  it("conserve les arrondis, les coefficients, les nombres de notes et les moyennes non calculables", async () => {
    const user = userEvent.setup(); page(BulletinsPage); await calculate(user);
    expect(screen.getByText("14,67 / 20")).toBeInTheDocument();
    const table = within(screen.getByRole("region", { name: "Tableau des résultats par matière" }));
    const mobile = within(screen.getByRole("region", { name: "Cartes des résultats par matière" }));
    for (const view of [table, mobile]) {
      for (const value of ["Mathématiques", "15,13 / 20", "2,5", "3", "Non calculable"]) expect(view.getByText(value)).toBeInTheDocument();
    }
    expect(screen.getByRole("link", { name: "Tableau de bord" })).toHaveAttribute("href", "/espace");
  });
  it("affiche les valeurs absentes sans inventer de moyenne", async () => {
    calculateBulletin.mockResolvedValue({ ...bulletin, moyenneGenerale: null, lignes: [] });
    const user = userEvent.setup(); page(BulletinsPage); await calculate(user);
    expect(screen.getByText("Non calculable")).toBeInTheDocument(); expect(screen.getByText("Aucun résultat disponible pour cette période.")).toBeInTheDocument();
  });
  it.each(["Élève et scolarité", "Période"])("retire le bulletin courant lors d’un changement de %s", async (label) => {
    const user = userEvent.setup(); page(BulletinsPage); await calculate(user);
    await user.selectOptions(screen.getByLabelText(label), label === "Période" ? "31" : "2");
    expect(screen.queryByRole("heading", { name: "Léa Étienne" })).not.toBeInTheDocument(); expect(screen.queryByRole("button", { name: "Télécharger le PDF" })).not.toBeInTheDocument();
  });
  it("affiche les chargements et désactive les actions correspondantes", async () => {
    const options = deferred(); getEleves.mockReturnValueOnce(options.promise);
    const user = userEvent.setup(); page(BulletinsPage);
    expect(screen.getByRole("status")).toHaveTextContent("Chargement des élèves"); expect(screen.getByLabelText("Période")).toBeDisabled(); expect(screen.getByRole("button", { name: "Calculer" })).toBeDisabled();
    await act(async () => options.resolve([])); await readyBulletin();
    const calculation = deferred(); calculateBulletin.mockReturnValueOnce(calculation.promise); await user.click(screen.getByRole("button", { name: "Calculer" }));
    expect(screen.getByRole("status")).toHaveTextContent("Calcul du bulletin"); expect(screen.getByRole("button", { name: "Calcul en cours" })).toBeDisabled();
    await act(async () => calculation.resolve(bulletin)); expect(screen.getByText("14,67 / 20")).toBeInTheDocument();
  });
  it.each(["scolarités", "périodes"])("explique l’absence de %s sans permettre de calcul", async (missing) => {
    if (missing === "scolarités") getScolarites.mockResolvedValue([]); else getPeriodes.mockResolvedValue([]);
    page(BulletinsPage); await waitFor(() => expect(screen.getByRole("button", { name: "Actualiser" })).toBeEnabled());
    expect(screen.getByRole("status")).toHaveTextContent(missing === "scolarités" ? "Aucune scolarité disponible" : "Aucune période disponible"); expect(screen.getByRole("button", { name: "Calculer" })).toBeDisabled(); expect(calculateBulletin).not.toHaveBeenCalled();
  });
  it("préserve l’erreur d’accès au chargement puis permet d’actualiser", async () => {
    getPeriodes.mockRejectedValueOnce(new ApiError("Interdit", 403)); const user = userEvent.setup(); page(BulletinsPage, "RESPONSABLE");
    expect(await screen.findByRole("alert")).toHaveTextContent("Votre rôle ne permet pas de charger les données nécessaires aux bulletins.");
    await user.click(screen.getByRole("button", { name: "Actualiser" })); await readyBulletin(); expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
  it("préserve l’erreur de génération sans proposer un téléchargement", async () => {
    calculateBulletin.mockRejectedValueOnce(new ApiError("Interdit", 403)); const user = userEvent.setup(); page(BulletinsPage, "RESPONSABLE"); await readyBulletin(); await user.click(screen.getByRole("button", { name: "Calculer" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Votre rôle ne permet pas de consulter ce bulletin."); expect(screen.queryByRole("button", { name: "Télécharger le PDF" })).not.toBeInTheDocument();
  });
  it.each(["ADMIN", "ENSEIGNANT", "RESPONSABLE"])("préserve le calcul et le téléchargement existants pour %s", async (role) => {
    const user = userEvent.setup(); page(BulletinsPage, role); await calculate(user);
    let clickedLink;
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function () { clickedLink = { href: this.getAttribute("href"), filename: this.download }; });
    vi.useFakeTimers();
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Télécharger le PDF" })); });
    expect(calculateBulletin).toHaveBeenCalledWith("1", "30"); expect(downloadBulletinPdf).toHaveBeenCalledWith(1, 30);
    expect(clickedLink).toEqual({ href: "blob:bulletin", filename: "bulletin-el-11-trimestre-1.pdf" });
    await act(async () => vi.advanceTimersByTime(1000)); expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:bulletin");
  });
  it("présente le téléchargement en cours et son erreur sans retirer les résultats", async () => {
    const download = deferred(); downloadBulletinPdf.mockReturnValueOnce(download.promise);
    const user = userEvent.setup(); page(BulletinsPage, "RESPONSABLE"); await calculate(user); await user.click(screen.getByRole("button", { name: "Télécharger le PDF" }));
    expect(screen.getByRole("button", { name: "Téléchargement" })).toBeDisabled(); expect(screen.getByRole("button", { name: "Téléchargement" })).toHaveAttribute("aria-busy", "true");
    await act(async () => download.reject(new ApiError("Interdit", 403)));
    expect(screen.getByRole("alert")).toHaveTextContent("Votre rôle ne permet pas de télécharger ce bulletin."); expect(screen.getByText("14,67 / 20")).toBeInTheDocument(); expect(screen.getByRole("button", { name: "Télécharger le PDF" })).toBeEnabled();
  });
});

describe("évaluations", () => {
  it.each(["ADMIN", "ENSEIGNANT", "RESPONSABLE"])("respecte les actions et informations déjà autorisées pour %s", async (role) => {
    page(EvaluationsPage, role); await screen.findByRole("heading", { name: "Contrôle fractions" });
    const card = within(screen.getByRole("article"));
    expect(card.getByText("Contrôle")).toBeInTheDocument(); expect(card.getByText("Barème : 20 points")).toBeInTheDocument(); expect(card.getByText("1.5")).toBeInTheDocument(); expect(card.getByText("Mathématiques · 6A · 2026-2027")).toBeInTheDocument();
    if (role === "ADMIN") expect(card.getByText("Alex Martin")).toBeInTheDocument(); else { expect(card.queryByText("Alex Martin")).not.toBeInTheDocument(); expect(listResource).not.toHaveBeenCalledWith("enseignants", expect.anything()); }
    for (const action of ["Ajouter", "Modifier", "Supprimer"]) {
      if (role === "RESPONSABLE") expect(screen.queryByRole("button", { name: action })).not.toBeInTheDocument(); else expect(screen.getByRole("button", { name: action })).toBeInTheDocument();
    }
    expect(screen.queryByRole("form")).not.toBeInTheDocument(); expect(screen.getByRole("link", { name: "Tableau de bord" })).toHaveAttribute("href", "/espace");
  });
  it.each(["ADMIN", "ENSEIGNANT"])("préserve la création et le payload numérique pour %s", async (role) => {
    const user = userEvent.setup(); page(EvaluationsPage, role); await fillEvaluation(user);
    expect(screen.getByLabelText("Enseignement")).toBeRequired(); expect(screen.getByLabelText("Barème")).toHaveValue(20); expect(screen.getByLabelText("Coefficient")).toHaveValue(1);
    expect(screen.getByLabelText("Période")).toHaveAccessibleDescription(/année scolaire/);
    await user.click(screen.getByRole("button", { name: "Enregistrer" })); await waitFor(() => expect(createResource).toHaveBeenCalledWith("evaluations", { idEnseignement: 20, idPeriode: 30, libelle: "Nouvelle évaluation", dateEvaluation: "2026-11-01", typeEvaluation: "Contrôle", coefficientEvaluation: 1, bareme: 20 }));
    expect(await screen.findByText("Création enregistrée.")).toBeInTheDocument();
  });
  it("préserve les validations obligatoires et le minimum du barème", async () => {
    const user = userEvent.setup(); page(EvaluationsPage); await fillEvaluation(user);
    await user.clear(screen.getByLabelText("Libellé")); await user.click(screen.getByRole("button", { name: "Enregistrer" })); expect(screen.getByLabelText("Libellé")).toBeInvalid(); expect(createResource).not.toHaveBeenCalled();
    fireEvent.submit(screen.getByRole("form", { name: "Évaluation" })); expect(screen.getByRole("alert")).toHaveTextContent("Renseignez tous les champs obligatoires.");
    await user.type(screen.getByLabelText("Libellé"), "Contrôle"); await user.clear(screen.getByLabelText("Barème")); await user.type(screen.getByLabelText("Barème"), "0");
    await user.click(screen.getByRole("button", { name: "Enregistrer" })); expect(screen.getByLabelText("Barème")).toBeInvalid(); expect(createResource).not.toHaveBeenCalled();
  });
  it("préserve la modification, l’annulation et la suppression", async () => {
    const user = userEvent.setup(); page(EvaluationsPage, "ENSEIGNANT"); await user.click(await screen.findByRole("button", { name: "Modifier" }));
    expect(screen.getByLabelText("Libellé")).toHaveValue("Contrôle fractions"); await user.click(screen.getByRole("button", { name: "Annuler" })); expect(updateResource).not.toHaveBeenCalled();
    await user.click(screen.getByRole("button", { name: "Modifier" })); await user.clear(screen.getByLabelText("Barème")); await user.type(screen.getByLabelText("Barème"), "10"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(updateResource).toHaveBeenCalledWith("evaluations", 10, expect.objectContaining({ bareme: 10, coefficientEvaluation: 1.5, idEnseignement: 20, idPeriode: 30 })));
    await screen.findByText("Modification enregistrée."); await user.click(await screen.findByRole("button", { name: "Supprimer" })); await waitFor(() => expect(deleteResource).toHaveBeenCalledWith("evaluations", 10));
  });
  it("préserve les valeurs après une erreur de validation serveur et bloque les contrôles pendant l’enregistrement", async () => {
    const request = deferred(); createResource.mockReturnValueOnce(request.promise); const user = userEvent.setup(); page(EvaluationsPage); await fillEvaluation(user); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    expect(screen.getByRole("button", { name: "Enregistrement…" })).toBeDisabled(); expect(screen.getByLabelText("Enseignement")).toBeDisabled();
    await act(async () => request.reject(new ApiError("Validation", 400, { validationErrors: { bareme: "Barème refusé." } })));
    expect(screen.getByRole("alert")).toHaveTextContent("Barème refusé."); expect(screen.getByLabelText("Libellé")).toHaveValue("Nouvelle évaluation"); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeEnabled();
  });
  it("présente le chargement puis l’erreur de consultation", async () => {
    const request = deferred(); listResource.mockReturnValueOnce(request.promise); page(EvaluationsPage, "RESPONSABLE");
    expect(screen.getByRole("status")).toHaveTextContent("Chargement"); await act(async () => request.reject(new Error("Consultation indisponible.")));
    expect(screen.getByRole("alert")).toHaveTextContent("Consultation indisponible."); expect(screen.queryByRole("article")).not.toBeInTheDocument();
  });
  it("présente l’absence d’évaluations sans actions pour le responsable", async () => {
    resources.evaluations = []; page(EvaluationsPage, "RESPONSABLE"); expect(await screen.findByText("Aucune donnée disponible.")).toBeInTheDocument(); expect(screen.queryByRole("button", { name: "Ajouter" })).not.toBeInTheDocument();
  });
});
