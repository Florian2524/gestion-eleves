import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "../App";
import { AuthContext } from "../context/authContextCore";
import ComptesUtilisateursPage from "../pages/ComptesUtilisateursPage";
import ResponsabilitesLegalesPage from "../pages/ResponsabilitesLegalesPage";
import { createResource, deleteResource, listResource, updateResource } from "../api/schoolApi";
import { createLegalLink, deleteLegalLink, listLegalLinks } from "../api/adminApi";
import { getEleves } from "../api/elevesApi";

vi.mock("../api/schoolApi", () => ({ listResource: vi.fn(), createResource: vi.fn(), updateResource: vi.fn(), deleteResource: vi.fn() }));
vi.mock("../api/adminApi", () => ({ listLegalLinks: vi.fn(), createLegalLink: vi.fn(), deleteLegalLink: vi.fn() }));
vi.mock("../api/elevesApi", () => ({ getEleves: vi.fn() }));

let accounts, people, guardians, links;
function page(Component, role = "ADMIN", path) {
  const auth = role ? { role, token: "fixture", emailConnexion: "admin@example.test" } : null;
  return render(<AuthContext.Provider value={{ auth, isAuthenticated: Boolean(auth), logout: vi.fn(), login: vi.fn() }}><MemoryRouter initialEntries={path ? [path] : undefined}><Component /></MemoryRouter></AuthContext.Provider>);
}
function deferred() {
  let resolve, reject;
  const promise = new Promise((done, fail) => { resolve = done; reject = fail; });
  return { promise, resolve, reject };
}
async function newAccount(user) {
  await user.click(await screen.findByRole("button", { name: "Ajouter" }));
  await user.selectOptions(screen.getByLabelText("Rôle"), "RESPONSABLE");
  await user.selectOptions(screen.getByLabelText("Personne"), "2");
  await user.type(screen.getByLabelText("E-mail de connexion"), "camille@example.test");
  await user.type(screen.getByLabelText("Mot de passe"), "Nouveau123!");
}
async function chooseLink(user, student = "12") {
  await screen.findByRole("form", { name: "Créer un lien" });
  await user.selectOptions(screen.getByLabelText("Responsable légal"), "2");
  await user.selectOptions(screen.getByLabelText("Élève"), student);
}
function firstAccount() { return within(screen.getAllByRole("article")[0]); }

beforeEach(() => {
  vi.resetAllMocks();
  vi.spyOn(window, "confirm").mockReturnValue(true);
  accounts = [
    { idUtilisateur: 9, idPersonne: 1, prenom: "Alex", nom: "Martin", emailConnexion: "alex@example.test", role: "ENSEIGNANT", actif: true, motDePasse: "NE-PAS-AFFICHER" },
    { idUtilisateur: 10, idPersonne: 5, prenom: "Nora", nom: "Dubois", emailConnexion: "nora@example.test", role: "RESPONSABLE", actif: false },
  ];
  people = [
    { idPersonne: 1, prenom: "Alex", nom: "Martin", profils: ["ENSEIGNANT"] },
    { idPersonne: 2, prenom: "Camille", nom: "Durand", profils: ["ENSEIGNANT", "RESPONSABLE"] },
    { idPersonne: 3, prenom: "Personne", nom: "Libre", profils: [] },
    { idPersonne: 4, prenom: "Sam", nom: "Prof", profils: ["ENSEIGNANT"] },
    { idPersonne: 5, prenom: "Nora", nom: "Dubois", profils: ["RESPONSABLE"] },
  ];
  guardians = [{ idPersonne: 2, prenom: "Camille", nom: "Durand" }];
  links = [{ idResponsable: 2, idEleve: 11, prenomResponsable: "Camille", nomResponsable: "Durand", prenomEleve: "Léa", nomEleve: "Étienne" }];
  listResource.mockImplementation((resource) => Promise.resolve({ comptes: accounts, personnes: people, responsables: guardians }[resource] ?? []));
  getEleves.mockResolvedValue([{ idPersonne: 11, prenom: "Léa", nom: "Étienne" }, { idPersonne: 12, prenom: "Noé", nom: "Martin" }]);
  listLegalLinks.mockImplementation(() => Promise.resolve(links));
  createResource.mockResolvedValue({}); updateResource.mockResolvedValue({}); deleteResource.mockResolvedValue({});
  createLegalLink.mockResolvedValue({}); deleteLegalLink.mockResolvedValue({});
});
afterEach(() => vi.restoreAllMocks());

describe("restrictions des routes existantes", () => {
  it.each(["/comptes-utilisateurs", "/responsabilites-legales"])("réserve %s à ADMIN", async (path) => {
    for (const role of ["ENSEIGNANT", "RESPONSABLE"]) {
      const view = page(App, role, path);
      await screen.findByRole("heading", { level: 1, name: /Un suivi scolaire/ });
      expect(screen.queryByRole("button", { name: /Ajouter|Associer/ })).not.toBeInTheDocument();
      expect(listResource).not.toHaveBeenCalled(); expect(listLegalLinks).not.toHaveBeenCalled(); view.unmount();
    }
  });
  it.each(["/comptes-utilisateurs", "/responsabilites-legales"])("redirige vers la connexion sans session pour %s", async (path) => {
    page(App, null, path); expect(await screen.findByRole("heading", { name: "Connexion" })).toBeInTheDocument(); expect(listResource).not.toHaveBeenCalled(); expect(listLegalLinks).not.toHaveBeenCalled();
  });
  it.each([["/comptes-utilisateurs", "Comptes utilisateurs"], ["/responsabilites-legales", "Responsabilités légales"]])("conserve l’accès ADMIN à %s", async (path, title) => {
    page(App, "ADMIN", path); expect(await screen.findByRole("heading", { level: 1, name: title })).toBeInTheDocument(); expect(screen.getByRole("link", { name: "Tableau de bord" })).toHaveAttribute("href", "/espace");
  });
});

describe("comptes utilisateurs", () => {
  it("distingue les comptes actifs et inactifs sans exposer de mot de passe", async () => {
    page(ComptesUtilisateursPage); await screen.findByRole("heading", { name: "Alex Martin" });
    expect(firstAccount().getByText("Actif")).toBeInTheDocument(); expect(within(screen.getAllByRole("article")[1]).getByText("Inactif")).toBeInTheDocument();
    expect(firstAccount().getByText("Enseignant")).toBeInTheDocument(); expect(screen.queryByText("NE-PAS-AFFICHER")).not.toBeInTheDocument();
  });
  it("conserve l’éligibilité, l’exclusion des personnes avec compte et le dédoublonnage", async () => {
    people.push({ ...people[1] }); const user = userEvent.setup(); page(ComptesUtilisateursPage);
    await user.click(await screen.findByRole("button", { name: "Ajouter" }));
    const select = screen.getByLabelText("Personne");
    for (const [role, ids] of [["ADMIN", ["", "2", "3", "4"]], ["ENSEIGNANT", ["", "2", "4"]], ["RESPONSABLE", ["", "2"]]]) {
      await user.selectOptions(screen.getByLabelText("Rôle"), role);
      expect(within(select).getAllByRole("option").map((option) => option.value)).toEqual(ids);
    }
    await user.selectOptions(select, "2"); await user.selectOptions(screen.getByLabelText("Rôle"), "ADMIN"); expect(select).toHaveValue("");
  });
  it("crée un compte avec les mêmes champs et le même rattachement numérique", async () => {
    const user = userEvent.setup(); page(ComptesUtilisateursPage); await newAccount(user);
    const password = screen.getByLabelText("Mot de passe"); expect(password).toBeRequired(); expect(password).toHaveAttribute("type", "password"); expect(password).toHaveAttribute("minLength", "8"); expect(password).toHaveAttribute("maxLength", "100");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(createResource).toHaveBeenCalledWith("comptes", { idPersonne: 2, emailConnexion: "camille@example.test", motDePasse: "Nouveau123!", role: "RESPONSABLE" }));
    expect(await screen.findByText("Compte créé.")).toBeInTheDocument();
  });
  it.each([true, false])("préserve la modification avec actif=%s et le mot de passe inchangé", async (active) => {
    const user = userEvent.setup(); page(ComptesUtilisateursPage); await screen.findByRole("heading", { name: "Alex Martin" }); await user.click(firstAccount().getByRole("button", { name: "Modifier" }));
    const password = screen.getByLabelText("Nouveau mot de passe (facultatif)"); expect(password).toHaveValue(""); expect(password).not.toBeRequired(); expect(password).toHaveAttribute("type", "password"); expect(password).toHaveAccessibleDescription(/conserver le mot de passe actuel/);
    expect(screen.queryByLabelText("Personne")).not.toBeInTheDocument(); expect(screen.queryByDisplayValue("NE-PAS-AFFICHER")).not.toBeInTheDocument();
    if (!active) await user.click(screen.getByLabelText("Compte actif"));
    await user.clear(screen.getByLabelText("E-mail de connexion")); await user.type(screen.getByLabelText("E-mail de connexion"), "alex2@example.test"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(updateResource).toHaveBeenCalledWith("comptes", 9, { emailConnexion: "alex2@example.test", role: "ENSEIGNANT", actif: active, nouveauMotDePasse: null }));
    expect(await screen.findByText("Compte modifié.")).toBeInTheDocument();
  });
  it("permet la réactivation et transmet uniquement le nouveau mot de passe saisi", async () => {
    const user = userEvent.setup(); page(ComptesUtilisateursPage); await screen.findByRole("heading", { name: "Nora Dubois" });
    await user.click(within(screen.getAllByRole("article")[1]).getByRole("button", { name: "Modifier" })); expect(screen.getByLabelText("Compte actif")).not.toBeChecked();
    await user.click(screen.getByLabelText("Compte actif")); await user.type(screen.getByLabelText("Nouveau mot de passe (facultatif)"), "Nouveau456!"); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    await waitFor(() => expect(updateResource).toHaveBeenCalledWith("comptes", 10, { emailConnexion: "nora@example.test", role: "RESPONSABLE", actif: true, nouveauMotDePasse: "Nouveau456!" }));
  });
  it("préserve les validations obligatoires, d’e-mail et les contraintes de longueur du mot de passe", async () => {
    const user = userEvent.setup(); page(ComptesUtilisateursPage); await newAccount(user);
    const email = screen.getByLabelText("E-mail de connexion"), password = screen.getByLabelText("Mot de passe");
    await user.clear(email); await user.click(screen.getByRole("button", { name: "Enregistrer" })); expect(email).toBeInvalid();
    await user.type(email, "adresse-invalide"); await user.click(screen.getByRole("button", { name: "Enregistrer" })); expect(email).toBeInvalid();
    await user.clear(email); await user.type(email, "valide@example.test"); await user.clear(password); await user.click(screen.getByRole("button", { name: "Enregistrer" })); expect(password).toBeInvalid();
    // jsdom does not implement the browser's tooShort validation; check the native constraints.
    expect(password).toHaveAttribute("minlength", "8"); expect(password).toHaveAttribute("maxlength", "100"); expect(createResource).not.toHaveBeenCalled();
  });
  it("annule sans enregistrement et ouvre une création sans ancien mot de passe", async () => {
    const user = userEvent.setup(); page(ComptesUtilisateursPage); await newAccount(user); await user.click(screen.getByRole("button", { name: "Annuler" }));
    expect(createResource).not.toHaveBeenCalled(); await user.click(screen.getByRole("button", { name: "Ajouter" })); expect(screen.getByLabelText("Mot de passe")).toHaveValue(""); expect(screen.getByLabelText("Rôle")).toHaveValue("");
  });
  it("explique l’absence de personnes éligibles et bloque la création", async () => {
    people = [people[0], people[4]]; const user = userEvent.setup(); page(ComptesUtilisateursPage); await user.click(await screen.findByRole("button", { name: "Ajouter" }));
    expect(screen.getByText(/Aucune personne disponible pour ce rôle/)).toBeInTheDocument(); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeDisabled(); expect(createResource).not.toHaveBeenCalled();
  });
  it("affiche le chargement puis l’erreur de consultation", async () => {
    const request = deferred(); listResource.mockReturnValueOnce(request.promise); page(ComptesUtilisateursPage);
    expect(screen.getByRole("status")).toHaveTextContent("Chargement des comptes"); expect(screen.getByRole("button", { name: "Ajouter" })).toBeDisabled(); await act(async () => request.reject(new Error("Consultation impossible.")));
    expect(screen.getByRole("alert")).toHaveTextContent("Consultation impossible.");
  });
  it("conserve la saisie après une erreur d’unicité et désactive les contrôles pendant l’enregistrement", async () => {
    const request = deferred(); createResource.mockReturnValueOnce(request.promise); const user = userEvent.setup(); page(ComptesUtilisateursPage); await newAccount(user); await user.click(screen.getByRole("button", { name: "Enregistrer" }));
    expect(screen.getByRole("button", { name: "Enregistrement…" })).toHaveAttribute("aria-busy", "true"); expect(screen.getByLabelText("Rôle")).toBeDisabled(); expect(screen.getByLabelText("Personne")).toBeDisabled(); expect(screen.getByRole("button", { name: "Annuler" })).toBeDisabled();
    await act(async () => request.reject(new Error("Cette adresse de connexion est déjà utilisée.")));
    expect(screen.getByRole("alert")).toHaveTextContent("Cette adresse de connexion est déjà utilisée."); expect(screen.getByLabelText("E-mail de connexion")).toHaveValue("camille@example.test"); expect(screen.getByLabelText("Personne")).toHaveValue("2"); expect(screen.getByRole("button", { name: "Enregistrer" })).toBeEnabled();
  });
  it("conserve la confirmation nominative avant suppression", async () => {
    const user = userEvent.setup(); page(ComptesUtilisateursPage); await screen.findByRole("heading", { name: "Alex Martin" });
    window.confirm.mockReturnValueOnce(false); await user.click(firstAccount().getByRole("button", { name: "Supprimer" })); expect(deleteResource).not.toHaveBeenCalled();
    await user.click(firstAccount().getByRole("button", { name: "Supprimer" })); expect(window.confirm).toHaveBeenCalledWith("Supprimer le compte de Alex Martin ?"); await waitFor(() => expect(deleteResource).toHaveBeenCalledWith("comptes", 9)); expect(await screen.findByText("Compte supprimé.")).toBeInTheDocument();
  });
  it("présente une liste vide sans compte fictif", async () => {
    accounts = []; page(ComptesUtilisateursPage); expect(await screen.findByText("Aucun compte utilisateur.")).toBeInTheDocument(); expect(screen.queryByRole("article")).not.toBeInTheDocument();
  });
});

describe("responsabilités légales", () => {
  it("distingue le responsable et l’élève dans les fiches et conserve la navigation", async () => {
    page(ResponsabilitesLegalesPage); const card = await screen.findByRole("article", { name: "Lien entre Camille Durand et Léa Étienne" });
    for (const text of ["Responsable légal", "Camille Durand", "Élève associé", "Léa Étienne"]) expect(within(card).getByText(text)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Tableau de bord" })).toHaveAttribute("href", "/espace");
  });
  it("crée l’association avec les mêmes identifiants numériques et réinitialise les sélections", async () => {
    const user = userEvent.setup(); page(ResponsabilitesLegalesPage); await chooseLink(user); await user.click(screen.getByRole("button", { name: "Associer" }));
    await waitFor(() => expect(createLegalLink).toHaveBeenCalledWith({ idResponsable: 2, idEleve: 12 })); expect(await screen.findByText("Lien créé.")).toBeInTheDocument();
    expect(await screen.findByLabelText("Responsable légal")).toHaveValue(""); expect(screen.getByLabelText("Élève")).toHaveValue("");
  });
  it("préserve les validations natives lorsque les sélections sont incomplètes", async () => {
    const user = userEvent.setup(); page(ResponsabilitesLegalesPage); await screen.findByRole("form", { name: "Créer un lien" });
    await user.click(screen.getByRole("button", { name: "Associer" })); expect(screen.getByLabelText("Responsable légal")).toBeInvalid(); expect(createLegalLink).not.toHaveBeenCalled();
    await user.selectOptions(screen.getByLabelText("Responsable légal"), "2"); await user.click(screen.getByRole("button", { name: "Associer" })); expect(screen.getByLabelText("Élève")).toBeInvalid(); expect(createLegalLink).not.toHaveBeenCalled();
  });
  it.each(["responsables", "élèves"])("explique l’absence de %s et désactive Associer", async (missing) => {
    if (missing === "responsables") guardians = []; else getEleves.mockResolvedValue([]);
    page(ResponsabilitesLegalesPage); await screen.findByRole("form", { name: "Créer un lien" }); expect(screen.getByText("Créez d’abord un responsable et un élève.")).toBeInTheDocument(); expect(screen.getByRole("button", { name: "Associer" })).toBeDisabled();
  });
  it("préserve le refus d’un doublon par l’API et conserve les sélections", async () => {
    createLegalLink.mockRejectedValueOnce(new Error("Cette responsabilité légale existe déjà.")); const user = userEvent.setup(); page(ResponsabilitesLegalesPage); await chooseLink(user, "11"); await user.click(screen.getByRole("button", { name: "Associer" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Cette responsabilité légale existe déjà."); expect(screen.getByLabelText("Responsable légal")).toHaveValue("2"); expect(screen.getByLabelText("Élève")).toHaveValue("11"); expect(screen.getByRole("button", { name: "Associer" })).toBeEnabled();
  });
  it("présente le chargement et une erreur de consultation", async () => {
    const request = deferred(); listLegalLinks.mockReturnValueOnce(request.promise); page(ResponsabilitesLegalesPage); expect(screen.getByRole("status")).toHaveTextContent("Chargement des responsables"); expect(screen.queryByRole("form")).not.toBeInTheDocument();
    await act(async () => request.reject(new Error("Associations inaccessibles."))); expect(screen.getByRole("alert")).toHaveTextContent("Associations inaccessibles.");
  });
  it("indique l’enregistrement et désactive sélecteurs et suppression pendant la requête", async () => {
    const request = deferred(); createLegalLink.mockReturnValueOnce(request.promise); const user = userEvent.setup(); page(ResponsabilitesLegalesPage); await chooseLink(user); await user.click(screen.getByRole("button", { name: "Associer" }));
    expect(screen.getByRole("button", { name: "Enregistrement…" })).toBeDisabled(); expect(screen.getByLabelText("Responsable légal")).toBeDisabled(); expect(screen.getByLabelText("Élève")).toBeDisabled(); expect(screen.getByRole("button", { name: "Supprimer le lien" })).toBeDisabled();
    await act(async () => request.resolve({})); expect(screen.getByText("Lien créé.")).toBeInTheDocument();
  });
  it("conserve la confirmation nominative et la clé composée de suppression", async () => {
    const user = userEvent.setup(); page(ResponsabilitesLegalesPage); const button = await screen.findByRole("button", { name: "Supprimer le lien" });
    window.confirm.mockReturnValueOnce(false); await user.click(button); expect(deleteLegalLink).not.toHaveBeenCalled();
    await user.click(button); expect(window.confirm).toHaveBeenCalledWith("Supprimer le lien entre Camille Durand et Léa Étienne ?"); await waitFor(() => expect(deleteLegalLink).toHaveBeenCalledWith(2, 11)); expect(await screen.findByText("Lien supprimé.")).toBeInTheDocument();
  });
  it("présente une erreur de suppression sans retirer la fiche", async () => {
    deleteLegalLink.mockRejectedValueOnce(new Error("Suppression refusée.")); const user = userEvent.setup(); page(ResponsabilitesLegalesPage); await user.click(await screen.findByRole("button", { name: "Supprimer le lien" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Suppression refusée."); expect(screen.getByRole("article")).toBeInTheDocument();
  });
  it("présente une liste d’associations vide", async () => {
    links = []; page(ResponsabilitesLegalesPage); expect(await screen.findByText("Aucune responsabilité légale enregistrée.")).toBeInTheDocument(); expect(screen.queryByRole("article")).not.toBeInTheDocument();
  });
});


it("conserve un parcours clavier cohérent dans la création de compte", async () => {
  const user = userEvent.setup(); page(ComptesUtilisateursPage);
  await waitFor(() => expect(screen.getByRole("button", { name: "Ajouter" })).toBeEnabled());
  await user.click(screen.getByRole("button", { name: "Ajouter" }));
  for (const label of ["Rôle", "Personne", "E-mail de connexion", "Mot de passe"]) {
    await user.tab(); expect(screen.getByLabelText(label)).toHaveFocus();
  }
});

it("permet de parcourir les sélecteurs et l’action Associer au clavier", async () => {
  const user = userEvent.setup(); page(ResponsabilitesLegalesPage);
  await screen.findByRole("form", { name: "Créer un lien" });
  await user.click(screen.getByLabelText("Responsable légal"));
  await user.tab(); expect(screen.getByLabelText("Élève")).toHaveFocus();
  await user.tab(); expect(screen.getByRole("button", { name: "Associer" })).toHaveFocus();
});
