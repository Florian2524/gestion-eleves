import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import App from "../App";
import { AuthProvider } from "../context/AuthContext";
import { authenticateUser } from "../api/authApi";
import { ApiError } from "../api/http";
import { getEleves } from "../api/elevesApi";
import { getScolarites, getPeriodes } from "../api/bulletinsApi";
import { getEvaluations, getNotes } from "../api/notesApi";
import { listResource } from "../api/schoolApi";

vi.mock("../api/authApi", () => ({ authenticateUser: vi.fn() }));
vi.mock("../api/elevesApi", () => ({ getEleves: vi.fn() }));
vi.mock("../api/bulletinsApi", () => ({ getScolarites: vi.fn(), getPeriodes: vi.fn() }));
vi.mock("../api/notesApi", () => ({ getEvaluations: vi.fn(), getNotes: vi.fn() }));
vi.mock("../api/schoolApi", () => ({ listResource: vi.fn() }));

const storageKey = "gestion-eleves.auth";
const fixture = (role) => ({ token: "jeton-fictif-test", role, emailConnexion: "utilisateur@example.test", expiresIn: 3600 });
function app(path = "/connexion") {
  return render(<MemoryRouter initialEntries={[path]}><AuthProvider><App /></AuthProvider></MemoryRouter>);
}
async function login(user) {
  await screen.findByRole("heading", { name: "Connexion" });
  await user.type(screen.getByLabelText(/Adresse électronique/), "utilisateur@example.test");
  await user.type(screen.getByLabelText(/^Mot de passe/), "MotDePasseFictif42!");
  await user.click(screen.getByRole("button", { name: "Se connecter" }));
}
beforeEach(() => {
  vi.resetAllMocks(); localStorage.removeItem(storageKey);
  // Any unexpected network request fails locally instead of reaching a server.
  vi.stubGlobal("fetch", vi.fn(() => Promise.reject(new Error("Accès réseau interdit dans ce test."))));
  getEleves.mockResolvedValue([]); getScolarites.mockResolvedValue([]); getPeriodes.mockResolvedValue([]);
  getEvaluations.mockResolvedValue([]); getNotes.mockResolvedValue([]); listResource.mockResolvedValue([]);
  authenticateUser.mockResolvedValue(fixture("ADMIN"));
});
afterEach(() => { expect(fetch).not.toHaveBeenCalled(); localStorage.removeItem(storageKey); vi.unstubAllGlobals(); });

it.each(["ADMIN", "ENSEIGNANT", "RESPONSABLE"])("connecte réellement le contexte et ouvre le tableau de bord %s", async (role) => {
  authenticateUser.mockResolvedValueOnce(fixture(role)); const user = userEvent.setup(); app(); await login(user);
  expect(await screen.findByRole("heading", { name: "Bienvenue dans votre espace" })).toBeInTheDocument();
  expect(authenticateUser).toHaveBeenCalledWith({ emailConnexion: "utilisateur@example.test", motDePasse: "MotDePasseFictif42!" });
  expect(JSON.parse(localStorage.getItem(storageKey))).toMatchObject({ role, token: "jeton-fictif-test" });
  expect(screen.getAllByRole("article")).toHaveLength(role === "ADMIN" ? 15 : 5);
});

it("revient au module demandé après la redirection d’un visiteur non connecté", async () => {
  authenticateUser.mockResolvedValueOnce(fixture("ENSEIGNANT")); const user = userEvent.setup(); app("/notes"); await login(user);
  expect(await screen.findByRole("heading", { level: 1, name: "Notes" })).toBeInTheDocument();
  expect(await screen.findByLabelText("Évaluation")).toBeInTheDocument();
});

it("retire la session à la déconnexion et rétablit les invitations publiques", async () => {
  const user = userEvent.setup(); app(); await login(user); await screen.findByRole("heading", { name: "Bienvenue dans votre espace" });
  await user.click(screen.getByRole("button", { name: "Déconnexion" })); expect(await screen.findByRole("heading", { name: "Connexion" })).toBeInTheDocument();
  expect(localStorage.getItem(storageKey)).toBeNull();
  await user.click(screen.getAllByRole("link", { name: "Retour à l’accueil" })[0]);
  expect(screen.getByRole("link", { name: "Connexion" })).toHaveAttribute("href", "/connexion"); expect(screen.queryByRole("link", { name: "Mon espace" })).not.toBeInTheDocument();
});

it("refuse une saisie incomplète sans appeler l’authentification", async () => {
  const user = userEvent.setup(); app(); await user.click(screen.getByRole("button", { name: "Se connecter" }));
  expect(authenticateUser).not.toHaveBeenCalled(); expect(screen.getByLabelText(/Adresse électronique/)).toHaveAttribute("aria-invalid", "true");
  await user.type(screen.getByLabelText(/Adresse électronique/), "utilisateur@example.test"); await user.type(screen.getByLabelText(/^Mot de passe/), "court"); await user.click(screen.getByRole("button", { name: "Se connecter" }));
  expect(authenticateUser).not.toHaveBeenCalled(); expect(screen.getByText("Le mot de passe doit contenir au moins 8 caractères.")).toBeInTheDocument();
});

it("annonce un refus de connexion sans créer de session et permet de réessayer", async () => {
  authenticateUser.mockRejectedValueOnce(new ApiError("Refus", 401)); const user = userEvent.setup(); app(); await login(user);
  expect(await screen.findByRole("alert")).toHaveTextContent("Adresse électronique ou mot de passe incorrect."); expect(localStorage.getItem(storageKey)).toBeNull();
  expect(screen.getByRole("button", { name: "Se connecter" })).toBeEnabled();
  await user.click(screen.getByRole("button", { name: "Se connecter" })); expect(await screen.findByRole("heading", { name: "Bienvenue dans votre espace" })).toBeInTheDocument();
});

it("présente une panne simulée et désactive le bouton pendant la connexion", async () => {
  let reject;
  authenticateUser.mockReturnValueOnce(new Promise((_, fail) => { reject = fail; }));
  const user = userEvent.setup(); app(); await login(user);
  expect(screen.getByRole("button", { name: "Connexion en cours..." })).toBeDisabled();
  await act(async () => reject(new Error("Réseau simulé indisponible")));
  expect(screen.getByRole("alert")).toHaveTextContent("Le serveur est actuellement inaccessible."); expect(localStorage.getItem(storageKey)).toBeNull();
});

it("rejette une session expirée avant d’afficher un module protégé", async () => {
  localStorage.setItem(storageKey, JSON.stringify({ ...fixture("RESPONSABLE"), expiresAt: Date.now() - 1000 })); app("/notes");
  expect(await screen.findByRole("heading", { name: "Connexion" })).toBeInTheDocument(); expect(localStorage.getItem(storageKey)).toBeNull(); expect(getNotes).not.toHaveBeenCalled();
});

it("prend en compte l’événement de session non autorisée sur une page protégée", async () => {
  localStorage.setItem(storageKey, JSON.stringify({ ...fixture("ADMIN"), expiresAt: Date.now() + 60000 })); app("/notes");
  await screen.findByRole("heading", { level: 1, name: "Notes" });
  // The HTTP layer normally clears storage before emitting this event; no real request here.
  localStorage.removeItem(storageKey); act(() => window.dispatchEvent(new Event("auth:unauthorized")));
  await waitFor(() => expect(screen.getByRole("heading", { name: "Connexion" })).toBeInTheDocument()); expect(localStorage.getItem(storageKey)).toBeNull();
});
