import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import ElevesPage from "../pages/ElevesPage";
import NotesPage from "../pages/NotesPage";
import { createEleve, deleteEleve, deleteElevePhoto, getElevePhoto, getEleves, updateEleve, uploadElevePhoto } from "../api/elevesApi";
import { getScolarites } from "../api/bulletinsApi";
import { createNote, deleteNote, getEvaluations, getNotes, updateNote } from "../api/notesApi";
import { attachProfile, listResource } from "../api/schoolApi";

vi.mock("../api/elevesApi", () => ({ getEleves: vi.fn(), getElevePhoto: vi.fn(), createEleve: vi.fn(), updateEleve: vi.fn(), deleteEleve: vi.fn(), uploadElevePhoto: vi.fn(), deleteElevePhoto: vi.fn() }));
vi.mock("../api/bulletinsApi", () => ({ getScolarites: vi.fn() }));
vi.mock("../api/notesApi", () => ({ getEvaluations: vi.fn(), getNotes: vi.fn(), createNote: vi.fn(), updateNote: vi.fn(), deleteNote: vi.fn() }));
vi.mock("../api/schoolApi", () => ({ listResource: vi.fn(), attachProfile: vi.fn() }));
const students = [
  { idPersonne: 11, nom: "Étienne", prenom: "Léa", matricule: "EL-11", dateNaissance: "2012-02-03", emailContact: "lea@example.test", telephone: "0601020304", adresse: "10 rue des Écoles" },
  { idPersonne: 12, nom: "Martin", prenom: "Noé", matricule: "EL-12", dateNaissance: "2012-04-05" },
];
const evaluations = [
  { idEvaluation: 10, idEnseignement: 20, idPeriode: 30, libelle: "Contrôle fractions", typeEvaluation: "Contrôle", dateEvaluation: "2026-10-10", bareme: 20 },
  { idEvaluation: 11, idEnseignement: 21, libelle: "Géométrie", bareme: 10 },
  { idEvaluation: 12, idEnseignement: 22, libelle: "Sans scolarité", bareme: 20 },
];
const notes = [{ idNote: 1, idEvaluation: 10, idScolarite: 1, valeur: 14, statutNote: "SAISIE", commentaire: "Progrès réguliers", dateSaisie: "2026-10-11" }];
function page(Component, role = "ADMIN") {
  return render(<AuthContext.Provider value={{ auth: { role, emailConnexion: "test@example.test" }, logout: vi.fn() }}><MemoryRouter><Component /></MemoryRouter></AuthContext.Provider>);
}
function cards() { return within(screen.getByRole("region", { name: "Cartes des élèves" })); }
function deferred() {
  let resolve, reject;
  const promise = new Promise((done, fail) => { resolve = done; reject = fail; });
  return { promise, resolve, reject };
}
async function fillStudent(user) {
  await user.click(screen.getByRole("button", { name: "Ajouter un élève" }));
  await user.type(screen.getByLabelText("Nom"), "Durand");
  await user.type(screen.getByLabelText("Prénom"), "Lou");
  await user.type(screen.getByLabelText("Matricule"), "EL-NEW");
  fireEvent.change(screen.getByLabelText("Date de naissance"), { target: { value: "2013-01-02" } });
}
async function fillNote(user, value = "16") {
  await user.selectOptions(await screen.findByLabelText("Évaluation"), "10");
  await user.selectOptions(screen.getByLabelText("Élève et scolarité"), "1");
  await user.type(screen.getByLabelText(/Note \(sur 20\)/), value);
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.spyOn(window, "scrollTo").mockImplementation(() => {});
  vi.spyOn(window, "confirm").mockReturnValue(true);
  vi.stubGlobal("URL", class extends URL {
    static createObjectURL = vi.fn(() => "blob:portrait");
    static revokeObjectURL = vi.fn();
  });
  getEleves.mockResolvedValue(students);
  getElevePhoto.mockResolvedValue(new Blob(["photo"], { type: "image/png" }));
  createEleve.mockResolvedValue({ idPersonne: 99 }); updateEleve.mockResolvedValue({ idPersonne: 11 });
  attachProfile.mockResolvedValue({ idPersonne: 50 });
  getEvaluations.mockResolvedValue(evaluations); getNotes.mockResolvedValue(notes);
  getScolarites.mockResolvedValue([{ idScolarite: 1, idClasse: 5, idEleve: 11, dateDebut: "2026-09-01" }, { idScolarite: 2, idClasse: 6, idEleve: 12, dateDebut: "2026-09-01" }]);
  createNote.mockResolvedValue({}); updateNote.mockResolvedValue({});
  listResource.mockImplementation((resource) => Promise.resolve({
    personnes: [{ idPersonne: 50, nom: "Existant", prenom: "Alex", profils: ["RESPONSABLE"] }],
    enseignements: [{ idEnseignement: 20, idClasse: 5, idMatiere: 40 }, { idEnseignement: 21, idClasse: 6, idMatiere: 40 }, { idEnseignement: 22, idClasse: 7, idMatiere: 40 }],
    classes: [{ idClasse: 5, nom: "6A", anneeScolaire: "2026-2027" }, { idClasse: 6, nom: "6B", anneeScolaire: "2026-2027" }],
    matieres: [{ idMatiere: 40, nom: "Mathématiques" }], periodes: [{ idPeriode: 30, libelle: "Trimestre 1", anneeScolaire: "2026-2027" }],
  }[resource] ?? []));
});
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

describe("élèves", () => {
  it.each(["ENSEIGNANT", "RESPONSABLE"])("conserve les dossiers et masque les actions pour %s", async (role) => {
    page(ElevesPage, role); await screen.findByRole("region", { name: "Cartes des élèves" });
    for (const value of ["0601020304", "10 rue des Écoles", "03/02/2012"]) expect(cards().getByText(value)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Ajouter un élève|Modifier|Supprimer|Retirer photo/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("form")).not.toBeInTheDocument();
  });
  it("recherche sans accents par identité, matricule et coordonnées et annonce les résultats", async () => {
    const user = userEvent.setup(); page(ElevesPage); await screen.findByRole("region", { name: "Cartes des élèves" });
    for (const query of ["etienne", "EL-11", "0601020304", "lea@example.test"]) {
      await user.clear(screen.getByLabelText("Rechercher un élève")); await user.type(screen.getByLabelText("Rechercher un élève"), query);
      expect(cards().getAllByRole("article")).toHaveLength(1); expect(screen.getByRole("status")).toHaveTextContent("1 résultat sur 2 élèves");
    }
    await user.clear(screen.getByLabelText("Rechercher un élève")); await user.type(screen.getByLabelText("Rechercher un élève"), "introuvable");
    expect(screen.getByRole("heading", { name: "Aucun élève trouvé" })).toBeInTheDocument();
  });
  it("crée un élève avec le payload existant et affiche la réussite", async () => {
    const user = userEvent.setup(); page(ElevesPage); await fillStudent(user);
    expect(screen.getByLabelText("Nom")).toBeRequired(); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(createEleve).toHaveBeenCalledWith({ nom: "Durand", prenom: "Lou", matricule: "EL-NEW", dateNaissance: "2013-01-02", emailContact: "", telephone: "", adresse: "", photoUrl: undefined }));
    expect(await screen.findByText("Élève enregistré.")).toBeInTheDocument();
  });
  it("rattache une personne existante sans transmettre de coordonnées", async () => {
    const user = userEvent.setup(); page(ElevesPage); await user.click(screen.getByRole("button", { name: "Ajouter un élève" }));
    await user.selectOptions(screen.getByLabelText("Création du profil"), "existing");
    await user.selectOptions(await screen.findByLabelText("Personne existante"), "50");
    expect(screen.queryByLabelText("Nom")).not.toBeInTheDocument(); await user.type(screen.getByLabelText("Matricule"), "EL-50");
    fireEvent.change(screen.getByLabelText("Date de naissance"), { target: { value: "2010-01-01" } });
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(attachProfile).toHaveBeenCalledWith(50, "ELEVE", { matricule: "EL-50", dateNaissance: "2010-01-01" }));
    expect(createEleve).not.toHaveBeenCalled();
  });
  it("conserve la modification, l’annulation et la suppression administratives", async () => {
    const user = userEvent.setup(); page(ElevesPage); await screen.findByRole("region", { name: "Cartes des élèves" });
    const card = within(cards().getAllByRole("article")[0]);
    await user.click(card.getByRole("button", { name: "Modifier" })); await user.click(screen.getByRole("button", { name: "Annuler" })); expect(updateEleve).not.toHaveBeenCalled();
    await user.click(card.getByRole("button", { name: "Modifier" })); await user.clear(screen.getByLabelText("Téléphone")); await user.type(screen.getByLabelText("Téléphone"), "0611111111");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(updateEleve).toHaveBeenCalledWith(11, expect.objectContaining({ telephone: "0611111111" })));
    await screen.findByText("Élève enregistré."); await screen.findByRole("region", { name: "Cartes des élèves" });
    await user.click(within(cards().getAllByRole("article")[0]).getByRole("button", { name: "Supprimer" })); await waitFor(() => expect(deleteEleve).toHaveBeenCalledWith(11));
  });
  it("conserve la photo, les initiales et le retrait de photo", async () => {
    getEleves.mockResolvedValue([{ ...students[0], photoUrl: "protected-photo" }, students[1]]);
    const user = userEvent.setup(); const view = page(ElevesPage); await screen.findByRole("region", { name: "Cartes des élèves" });
    expect(await cards().findByRole("img", { name: "Portrait de Léa Étienne" })).toHaveAttribute("src", "blob:portrait"); expect(cards().getByText("NM")).toBeInTheDocument();
    await user.click(cards().getByRole("button", { name: "Retirer photo" })); await waitFor(() => expect(deleteElevePhoto).toHaveBeenCalledWith(11));
    view.unmount(); expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:portrait");
  });
  it("transmet une photo autorisée après création", async () => {
    const user = userEvent.setup(); page(ElevesPage); await fillStudent(user);
    const photo = new File(["image"], "portrait.png", { type: "image/png" }); await user.upload(screen.getByLabelText("Photo de l’élève"), photo);
    await user.click(screen.getByRole("button", { name: "Enregistrer" })); await waitFor(() => expect(uploadElevePhoto).toHaveBeenCalledWith(99, photo));
  });
  it.each([new File(["texte"], "photo.txt", { type: "text/plain" }), new File([new Uint8Array(5 * 1024 * 1024 + 1)], "photo.png", { type: "image/png" })])("rejette une photo de format ou taille invalide sans écriture", async (photo) => {
    const user = userEvent.setup({ applyAccept: false }); page(ElevesPage); await fillStudent(user);
    await user.upload(screen.getByLabelText("Photo de l’élève"), photo); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    expect(screen.getByRole("alert")).toHaveTextContent("5 Mo maximum"); expect(createEleve).not.toHaveBeenCalled(); expect(uploadElevePhoto).not.toHaveBeenCalled();
  });
  it("affiche le chargement et l’erreur de liste puis permet de réessayer", async () => {
    const request = deferred(); getEleves.mockReturnValueOnce(request.promise); const user = userEvent.setup(); page(ElevesPage);
    expect(screen.getByRole("status")).toHaveTextContent("Chargement des élèves"); await act(async () => request.reject(new Error("Indisponible")));
    expect(screen.getByRole("alert")).toHaveTextContent("Chargement impossible"); await user.click(screen.getByRole("button", { name: "Réessayer" }));
    expect(await screen.findByRole("region", { name: "Cartes des élèves" })).toBeInTheDocument();
  });
  it("désactive le formulaire pendant l’enregistrement et préserve les valeurs après erreur", async () => {
    const request = deferred(); createEleve.mockReturnValueOnce(request.promise); const user = userEvent.setup(); page(ElevesPage); await fillStudent(user);
    await user.click(screen.getByRole("button", { name: "Enregistrer" })); expect(screen.getByRole("button", { name: "Enregistrement..." })).toBeDisabled(); expect(screen.getByRole("button", { name: "Annuler" })).toBeDisabled();
    await act(async () => request.reject(new Error("Matricule déjà utilisé.")));
    expect(screen.getByRole("alert")).toHaveTextContent("Matricule déjà utilisé."); expect(screen.getByLabelText("Matricule")).toHaveValue("EL-NEW"); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeEnabled();
  });
});

describe("notes", () => {
  it("limite RESPONSABLE à la consultation et conserve la recherche", async () => {
    const user = userEvent.setup(); page(NotesPage, "RESPONSABLE"); await screen.findByRole("heading", { name: "Consulter les notes" });
    expect(screen.queryByRole("form")).not.toBeInTheDocument(); expect(screen.queryByRole("button", { name: /Enregistrer|Modifier|Supprimer/ })).not.toBeInTheDocument(); expect(screen.getByRole("article")).toHaveTextContent("14 / 20");
    for (const query of ["etienne", "EL-11", "fractions", "progres", "SAISIE"]) {
      await user.clear(screen.getByLabelText("Rechercher une note")); await user.type(screen.getByLabelText("Rechercher une note"), query); expect(screen.getAllByRole("article")).toHaveLength(1);
    }
    await user.clear(screen.getByLabelText("Rechercher une note")); await user.type(screen.getByLabelText("Rechercher une note"), "absente"); expect(screen.getByRole("heading", { name: "Aucun résultat" })).toBeInTheDocument();
  });
  it("explique le barème et réinitialise une scolarité devenue incompatible", async () => {
    const user = userEvent.setup(); page(NotesPage); await screen.findByLabelText("Évaluation");
    expect(screen.getByLabelText("Élève et scolarité")).toBeDisabled(); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeDisabled();
    await user.selectOptions(screen.getByLabelText("Évaluation"), "10"); expect(screen.getByLabelText("Évaluation")).toHaveAccessibleDescription(/Barème : 20 points/);
    const scolarite = screen.getByLabelText("Élève et scolarité"); expect(within(scolarite).queryByRole("option", { name: /Noé/ })).not.toBeInTheDocument();
    await user.selectOptions(scolarite, "1"); await user.selectOptions(screen.getByLabelText("Évaluation"), "11"); expect(scolarite).toHaveValue(""); expect(within(scolarite).queryByRole("option", { name: /Léa/ })).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Évaluation"), "12"); expect(scolarite).toHaveAccessibleDescription("Aucune scolarité compatible avec cette évaluation."); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeDisabled();
  });
  it.each(["ADMIN", "ENSEIGNANT"])("conserve la création puis la modification pour %s", async (role) => {
    const user = userEvent.setup(); page(NotesPage, role); await fillNote(user); await user.type(screen.getByLabelText("Commentaire"), " Observation "); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(createNote).toHaveBeenCalledWith({ idEvaluation: 10, idScolarite: 1, valeur: 16, commentaire: "Observation", statutNote: "SAISIE" }));
    await screen.findByText("Note créée."); await user.click(await screen.findByRole("button", { name: "Modifier" }));
    expect(screen.getByLabelText("Évaluation")).toBeDisabled(); expect(screen.getByLabelText(/Note \(sur 20\)/)).toHaveValue(14);
    await user.clear(screen.getByLabelText(/Note \(sur 20\)/)); await user.type(screen.getByLabelText(/Note \(sur 20\)/), "15"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(updateNote).toHaveBeenCalledWith(1, { idEvaluation: 10, idScolarite: 1, valeur: 15, commentaire: "Progrès réguliers", statutNote: "SAISIE" })); expect(await screen.findByText("Note modifiée.")).toBeInTheDocument();
  });
  it("bloque une valeur hors barème sans appeler l’API", async () => {
    const user = userEvent.setup(); page(NotesPage); await fillNote(user, "21"); fireEvent.submit(screen.getByRole("form", { name: "Saisir une note" }));
    expect(screen.getByRole("alert")).toHaveTextContent("La note doit être comprise entre 0 et 20."); expect(createNote).not.toHaveBeenCalled();
  });
  it("annule la modification et conserve la suppression autorisée", async () => {
    const user = userEvent.setup(); page(NotesPage); await user.click(await screen.findByRole("button", { name: "Modifier" })); await user.click(screen.getByRole("button", { name: "Annuler" }));
    expect(screen.getByRole("form", { name: "Saisir une note" })).toBeInTheDocument(); expect(updateNote).not.toHaveBeenCalled(); await user.click(screen.getByRole("button", { name: "Supprimer" }));
    await waitFor(() => expect(deleteNote).toHaveBeenCalledWith(1)); expect(await screen.findByText("Note supprimée.")).toBeInTheDocument();
  });
  it("présente le chargement et une erreur de consultation", async () => {
    const request = deferred(); getNotes.mockReturnValueOnce(request.promise); page(NotesPage, "RESPONSABLE");
    expect(screen.getByRole("status")).toHaveTextContent("Chargement des notes"); await act(async () => request.reject(new Error("Consultation indisponible."))); expect(screen.getByRole("alert")).toHaveTextContent("Consultation indisponible.");
  });
  it("désactive les contrôles pendant l’enregistrement et conserve la saisie après erreur", async () => {
    const request = deferred(); createNote.mockReturnValueOnce(request.promise); const user = userEvent.setup(); page(NotesPage); await fillNote(user); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    expect(screen.getByRole("button", { name: "Enregistrement…" })).toHaveAttribute("aria-busy", "true"); expect(screen.getByLabelText("Évaluation")).toBeDisabled(); expect(screen.getByLabelText(/Note \(sur 20\)/)).toBeDisabled();
    await act(async () => request.reject(new Error("Saisie indisponible."))); expect(screen.getByRole("alert")).toHaveTextContent("Saisie indisponible."); expect(screen.getByLabelText(/Note \(sur 20\)/)).toHaveValue(16); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeEnabled();
  });
});
