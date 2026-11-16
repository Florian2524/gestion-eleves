import { useEffect } from "react";
import { act, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useNavigate } from "react-router";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import App from "../App";
import HomePage from "../pages/HomePage";
import NotesPage from "../pages/NotesPage";
import { AuthContext } from "../context/authContextCore";
import { getEleves } from "../api/elevesApi";
import { getScolarites, getPeriodes } from "../api/bulletinsApi";
import { getEvaluations, getNotes } from "../api/notesApi";
import { listResource } from "../api/schoolApi";

vi.mock("../api/elevesApi", () => ({ getEleves: vi.fn() }));
vi.mock("../api/bulletinsApi", () => ({ getScolarites: vi.fn(), getPeriodes: vi.fn() }));
vi.mock("../api/notesApi", () => ({ getEvaluations: vi.fn(), getNotes: vi.fn() }));
vi.mock("../api/schoolApi", () => ({ listResource: vi.fn() }));

let navigate;
function Controls() {
  const routeNavigate = useNavigate();
  useEffect(() => { navigate = routeNavigate; }, [routeNavigate]);
  return null;
}
function app(path, role = "ADMIN") {
  const auth = role ? { role, token: "fixture" } : null;
  return render(<AuthContext.Provider value={{ auth, isAuthenticated: Boolean(auth), logout: vi.fn(), login: vi.fn() }}><MemoryRouter initialEntries={[path]}><Controls /><App /></MemoryRouter></AuthContext.Provider>);
}
beforeEach(() => {
  vi.resetAllMocks();
  getEleves.mockResolvedValue([]); getScolarites.mockResolvedValue([]); getPeriodes.mockResolvedValue([]);
  getEvaluations.mockResolvedValue([]); getNotes.mockResolvedValue([]); listResource.mockResolvedValue([]);
  vi.spyOn(window, "scrollTo").mockImplementation(() => {});
});
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

it.each([
  ["/", null], ["/connexion", null], ["/espace", "ADMIN"], ["/eleves", "RESPONSABLE"], ["/notes", "ENSEIGNANT"], ["/bulletins", "RESPONSABLE"],
  ["/classes", "ADMIN"], ["/matieres", "ADMIN"], ["/periodes", "ADMIN"], ["/enseignants", "ADMIN"], ["/responsables", "ADMIN"], ["/enseignements", "ENSEIGNANT"],
  ["/ponderations", "ADMIN"], ["/inscriptions", "ADMIN"], ["/scolarites", "ADMIN"], ["/evaluations", "RESPONSABLE"], ["/comptes-utilisateurs", "ADMIN"], ["/responsabilites-legales", "ADMIN"],
])("propose un seul lien d’évitement et une cible existante pour %s (%s)", async (path, role) => {
  app(path, role);
  const links = screen.getAllByRole("link", { name: "Aller au contenu principal" }); expect(links).toHaveLength(1);
  const main = screen.getByRole("main"); expect(links[0]).toHaveAttribute("href", `#${main.id}`); expect(main).toHaveAttribute("tabindex", "-1");
  const user = userEvent.setup(); await user.click(links[0]); expect(main).toHaveFocus();
});

it("rend le lien d’évitement atteignable en premier au clavier sur l’accueil", async () => {
  app("/", null); const user = userEvent.setup(); await user.tab(); expect(screen.getByRole("link", { name: "Aller au contenu principal" })).toHaveFocus(); await user.keyboard("{Enter}"); expect(screen.getByRole("main")).toHaveFocus();
});

it("conserve l’autofocus initial de connexion mais focalise le contenu après navigation", async () => {
  app("/connexion", null); expect(screen.getByLabelText(/Adresse électronique/)).toHaveFocus();
  await act(async () => navigate("/")); expect(screen.getByRole("main")).toHaveFocus();
  await act(async () => navigate("/connexion")); expect(screen.getByRole("main")).toHaveFocus();
});

it("focalise la page après navigation interne et retour dans l’historique", async () => {
  app("/espace"); const user = userEvent.setup(); await user.click(screen.getByRole("link", { name: "Ouvrir le module Notes" }));
  expect(screen.getByRole("heading", { level: 1, name: "Notes" })).toBeInTheDocument(); expect(screen.getByRole("main")).toHaveFocus();
  await act(async () => navigate(-1)); expect(screen.getByRole("heading", { level: 1, name: "Bienvenue dans votre espace" })).toBeInTheDocument(); expect(screen.getByRole("main")).toHaveFocus();
});

it("ne reprend pas le focus pendant la saisie, une mise à jour de données ou un changement de recherche/hash", async () => {
  app("/notes"); const user = userEvent.setup(); const input = await screen.findByLabelText("Rechercher une note");
  await user.type(input, "maths"); expect(input).toHaveFocus();
  await act(async () => navigate("/notes?filtre=maths#notes-list-title")); expect(input).toHaveFocus(); expect(input).toHaveValue("maths");
});

it.each([["/notes", null, "Connexion"], ["/classes", "RESPONSABLE", /Un suivi scolaire/], ["/route-inconnue", "ADMIN", /Un suivi scolaire/], ["/connexion", "ADMIN", "Bienvenue dans votre espace"]])("focalise la destination de redirection depuis %s", async (path, role, heading) => {
  app(path, role); await screen.findByRole("heading", { name: heading }); expect(screen.getByRole("main")).toHaveFocus(); expect(screen.getAllByRole("link", { name: "Aller au contenu principal" })).toHaveLength(1);
});

it("gère également une tentative interdite suivie d’un retour vers la même page publique", async () => {
  app("/", "RESPONSABLE"); await act(async () => navigate("/comptes-utilisateurs"));
  await screen.findByRole("heading", { name: /Un suivi scolaire/ }); expect(screen.getByRole("main")).toHaveFocus(); expect(listResource).not.toHaveBeenCalled();
});

it.each([false, true])("ferme le menu par Échap et rend le focus au bouton (connecté : %s)", async (connected) => {
  render(<AuthContext.Provider value={{ isAuthenticated: connected }}><HomePage /></AuthContext.Provider>);
  const user = userEvent.setup(); const button = screen.getByRole("button", { name: "Ouvrir le menu" }); await user.click(button);
  const menu = within(screen.getByRole("navigation", { name: "Navigation mobile" }));
  const link = menu.getByRole("link", { name: connected ? "Mon espace" : "Connexion" }); expect(link).toHaveAttribute("href", connected ? "/espace" : "/connexion");
  link.focus(); await user.keyboard("{Escape}"); expect(screen.queryByRole("navigation", { name: "Navigation mobile" })).not.toBeInTheDocument(); expect(button).toHaveAttribute("aria-expanded", "false"); expect(button).toHaveFocus();
  await user.keyboard("{Enter}"); expect(screen.getByRole("navigation", { name: "Navigation mobile" })).toBeInTheDocument();
});

it("laisse parcourir le menu sans piège clavier et ferme l’ancre Accueil vers le contenu", async () => {
  render(<AuthContext.Provider value={{ isAuthenticated: false }}><HomePage /></AuthContext.Provider>);
  const user = userEvent.setup(); await user.click(screen.getByRole("button", { name: "Ouvrir le menu" })); await user.tab();
  const menu = within(screen.getByRole("navigation", { name: "Navigation mobile" })); expect(menu.getByRole("link", { name: "Accueil" })).toHaveFocus();
  for (let index = 0; index < 5; index += 1) await user.tab();
  expect(screen.getByRole("link", { name: "Consulter les élèves" })).toHaveFocus();
  await user.click(menu.getByRole("link", { name: "Accueil" })); expect(screen.queryByRole("navigation", { name: "Navigation mobile" })).not.toBeInTheDocument(); expect(screen.getByRole("main")).toHaveFocus();
});

it.each([true, false])("respecte le mouvement réduit dans la modification des notes (%s)", async (reduced) => {
  vi.stubGlobal("matchMedia", vi.fn(() => ({ matches: reduced })));
  getNotes.mockResolvedValue([{ idNote: 1, idEvaluation: 10, idScolarite: 1, valeur: 15, statutNote: "SAISIE" }]);
  getEvaluations.mockResolvedValue([{ idEvaluation: 10, idEnseignement: 20, bareme: 20, libelle: "Contrôle" }]);
  getScolarites.mockResolvedValue([{ idScolarite: 1, idClasse: 5, idEleve: 11 }]);
  listResource.mockImplementation((resource) => Promise.resolve(resource === "enseignements" ? [{ idEnseignement: 20, idClasse: 5 }] : []));
  render(<AuthContext.Provider value={{ auth: { role: "ENSEIGNANT" }, logout: vi.fn() }}><MemoryRouter><NotesPage /></MemoryRouter></AuthContext.Provider>);
  const user = userEvent.setup(); await user.click(await screen.findByRole("button", { name: "Modifier" }));
  expect(window.matchMedia).toHaveBeenCalledWith("(prefers-reduced-motion: reduce)"); expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, behavior: reduced ? "instant" : "smooth" });
  expect(screen.getByRole("heading", { name: "Modifier une note" })).toHaveFocus(); expect(screen.getByLabelText(/Note \(sur 20\)/)).toHaveValue(15);
  await user.click(screen.getByLabelText(/Note \(sur 20\)/)); await user.type(screen.getByLabelText(/Note \(sur 20\)/), "1"); expect(screen.getByLabelText(/Note \(sur 20\)/)).toHaveFocus();
});
