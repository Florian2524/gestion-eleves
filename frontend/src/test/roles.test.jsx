import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProtectedRoute from "../components/auth/ProtectedRoute";
import { AuthContext } from "../context/authContextCore";
import DashboardPage from "../pages/DashboardPage";
import ElevesPage from "../pages/ElevesPage";
import NotesPage from "../pages/NotesPage";
import { getEleves } from "../api/elevesApi";
import { getScolarites } from "../api/bulletinsApi";
import { getEvaluations, getNotes, createNote } from "../api/notesApi";
import { listResource } from "../api/schoolApi";

vi.mock("../api/elevesApi", () => ({ getEleves: vi.fn(), getElevePhoto: vi.fn(), createEleve: vi.fn(), updateEleve: vi.fn(), deleteEleve: vi.fn(), uploadElevePhoto: vi.fn(), deleteElevePhoto: vi.fn() }));
vi.mock("../api/bulletinsApi", () => ({ getScolarites: vi.fn() }));
vi.mock("../api/notesApi", () => ({ getEvaluations: vi.fn(), getNotes: vi.fn(), createNote: vi.fn(), updateNote: vi.fn(), deleteNote: vi.fn() }));
vi.mock("../api/schoolApi", () => ({ listResource: vi.fn() }));

function page(component, role = "ADMIN") {
  const auth = role ? { role, token: "fixture", emailConnexion: "test@example.com" } : null;
  return render(<AuthContext.Provider value={{ auth, isAuthenticated: Boolean(auth), logout: vi.fn() }}><MemoryRouter>{component}</MemoryRouter></AuthContext.Provider>);
}

beforeEach(() => {
  getEleves.mockResolvedValue([]);
  getScolarites.mockResolvedValue([]);
  getEvaluations.mockResolvedValue([]);
  getNotes.mockResolvedValue([]);
  listResource.mockResolvedValue([]);
  createNote.mockReset();
});

describe("routes et rôles", () => {
  const routes = <Routes><Route path="/" element={<span>Accueil</span>} /><Route path="/connexion" element={<span>Connexion</span>} /><Route path="/prive" element={<ProtectedRoute allowedRoles={["ADMIN"]}><span>Privé</span></ProtectedRoute>} /></Routes>;
  function route(role) {
    const auth = role ? { role, token: "fixture" } : null;
    render(<AuthContext.Provider value={{ auth, isAuthenticated: Boolean(auth) }}><MemoryRouter initialEntries={["/prive"]}>{routes}</MemoryRouter></AuthContext.Provider>);
  }
  it("redirige sans authentification", () => { route(null); expect(screen.getByText("Connexion")).toBeInTheDocument(); });
  it("refuse un rôle non autorisé", () => { route("RESPONSABLE"); expect(screen.getByText("Accueil")).toBeInTheDocument(); });
  it("accepte le rôle autorisé", () => { route("ADMIN"); expect(screen.getByText("Privé")).toBeInTheDocument(); });
});

it.each(["ENSEIGNANT", "RESPONSABLE"])("masque l'administration au rôle %s", (role) => {
  page(<DashboardPage />, role);
  expect(screen.queryByRole("heading", { name: "Comptes utilisateurs" })).not.toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "Classes" })).not.toBeInTheDocument();
});

it("affiche les modules d'administration à ADMIN", () => {
  page(<DashboardPage />);
  expect(screen.getByRole("heading", { name: "Comptes utilisateurs" })).toBeInTheDocument();
});

it("laisse l'élève en lecture seule hors ADMIN", async () => {
  page(<ElevesPage />, "RESPONSABLE");
  await waitFor(() => expect(getEleves).toHaveBeenCalled());
  expect(screen.queryByRole("button", { name: "Ajouter un élève" })).not.toBeInTheDocument();
});

it("affiche l'ajout d'élève à ADMIN", async () => {
  page(<ElevesPage />);
  expect(await screen.findByRole("button", { name: "Ajouter un élève" })).toBeInTheDocument();
});

it("ne propose aucune saisie de note à RESPONSABLE", async () => {
  page(<NotesPage />, "RESPONSABLE");
  await waitFor(() => expect(screen.queryByRole("status", { name: /Chargement/ })).not.toBeInTheDocument());
  expect(screen.queryByRole("form")).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Enregistrer" })).not.toBeInTheDocument();
});

it("filtre les scolarités par classe et bloque une note au-dessus du barème", async () => {
  getEvaluations.mockResolvedValue([{ idEvaluation: 10, idEnseignement: 20, libelle: "Contrôle", bareme: 20 }]);
  getScolarites.mockResolvedValue([{ idScolarite: 1, idClasse: 5, idEleve: 11, dateDebut: "2026-09-01" }, { idScolarite: 2, idClasse: 6, idEleve: 12, dateDebut: "2026-09-01" }]);
  listResource.mockImplementation((resource) => Promise.resolve(resource === "enseignements" ? [{ idEnseignement: 20, idClasse: 5 }] : []));
  page(<NotesPage />);
  const user = userEvent.setup();
  await user.selectOptions(await screen.findByLabelText("Évaluation"), "10");
  const students = screen.getByLabelText("Élève et scolarité");
  expect(within(students).getAllByRole("option")).toHaveLength(2);
  expect(within(students).queryByRole("option", { name: /classe 6/i })).not.toBeInTheDocument();
  await user.selectOptions(students, "1");
  await user.type(screen.getByLabelText(/Note \(sur 20\)/), "21");
  fireEvent.submit(screen.getByRole("button", { name: "Enregistrer" }).closest("form"));
  expect(screen.getByRole("alert")).toHaveTextContent("La note doit être comprise entre 0 et 20.");
  expect(createNote).not.toHaveBeenCalled();
});
