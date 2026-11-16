import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router";
import { expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import ConnectedHeader from "../components/layout/ConnectedHeader";
import Button from "../components/ui/Button";
import StatusMessage from "../components/ui/StatusMessage";

function header(role = "ADMIN", path = "/notes", logout = vi.fn()) {
  return render(<AuthContext.Provider value={{ auth: { role, emailConnexion: "adresse-tres-longue@example.fr" }, logout }}><MemoryRouter initialEntries={[path]}><ConnectedHeader /><Routes><Route path="/espace" element={<h1>Mon tableau de bord</h1>} /><Route path="/connexion" element={<h1>Identification</h1>} /><Route path="*" element={<h1>Module</h1>} /></Routes></MemoryRouter></AuthContext.Provider>);
}

it.each([["ADMIN", "Administrateur"], ["ENSEIGNANT", "Enseignant"], ["RESPONSABLE", "Responsable légal"]])("affiche l’identité et les destinations communes pour %s", (role, label) => {
  header(role);
  const banner = within(screen.getByRole("banner"));
  expect(banner.getByText(label)).toBeInTheDocument();
  expect(banner.getByText("adresse-tres-longue@example.fr")).toBeInTheDocument();
  expect(banner.getByRole("link", { name: "Scolarité — accueil public" })).toHaveAttribute("href", "/");
  expect(banner.getByRole("link", { name: "Tableau de bord" })).toHaveAttribute("href", "/espace");
  expect(banner.getByRole("button", { name: "Déconnexion" })).toBeInTheDocument();
});

it("permet de rejoindre le tableau de bord au clavier et indique la page active", async () => {
  const user = userEvent.setup();
  header();
  await user.tab();
  expect(screen.getByRole("link", { name: "Scolarité — accueil public" })).toHaveFocus();
  await user.tab();
  expect(screen.getByRole("link", { name: "Tableau de bord" })).toHaveFocus();
  await user.keyboard("{Enter}");
  expect(screen.getByRole("heading", { name: "Mon tableau de bord" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Tableau de bord" })).toHaveAttribute("aria-current", "page");
});

it("réutilise la déconnexion existante et rejoint la connexion", async () => {
  const user = userEvent.setup();
  const logout = vi.fn();
  header("RESPONSABLE", "/notes", logout);
  await user.click(screen.getByRole("button", { name: "Déconnexion" }));
  expect(logout).toHaveBeenCalledTimes(1);
  expect(screen.getByRole("heading", { name: "Identification" })).toBeInTheDocument();
});

it("bloque un bouton en cours de traitement sans soumettre le formulaire", async () => {
  const user = userEvent.setup();
  const submit = vi.fn((event) => event.preventDefault());
  const { rerender } = render(<form onSubmit={submit}><Button type="submit" variant="school" loading>Enregistrement…</Button></form>);
  const button = screen.getByRole("button", { name: "Enregistrement…" });
  expect(button).toBeDisabled();
  expect(button).toHaveAttribute("aria-busy", "true");
  await user.click(button);
  expect(submit).not.toHaveBeenCalled();
  rerender(<form onSubmit={submit}><Button type="submit" variant="school">Enregistrer</Button></form>);
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  expect(submit).toHaveBeenCalledTimes(1);
});

it("expose les messages d’erreur et de suivi avec les rôles accessibles adaptés", () => {
  render(<><StatusMessage variant="error">Échec</StatusMessage><StatusMessage variant="success">Enregistré</StatusMessage><StatusMessage>Chargement…</StatusMessage></>);
  expect(screen.getByRole("alert")).toHaveTextContent("Échec");
  expect(screen.getAllByRole("status").map((node) => node.textContent)).toEqual(["Enregistré", "Chargement…"]);
});
