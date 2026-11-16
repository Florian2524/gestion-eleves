import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, it } from "vitest";
import { AuthContext } from "../context/authContextCore";
import HomePage from "../pages/HomePage";

function home(isAuthenticated, role = "ADMIN") {
  return <AuthContext.Provider value={{ isAuthenticated, auth: isAuthenticated ? { role } : null }}><HomePage /></AuthContext.Provider>;
}

it.each([false, true])("adapte les liens de compte sur ordinateur et mobile (session : %s)", async (authenticated) => {
  const user = userEvent.setup();
  render(home(authenticated));
  const name = authenticated ? "Mon espace" : "Connexion";
  const href = authenticated ? "/espace" : "/connexion";
  const header = within(screen.getByRole("banner"));
  expect(header.getByRole("link", { name })).toHaveAttribute("href", href);
  await user.click(header.getByRole("button", { name: "Ouvrir le menu" }));
  const mobile = within(screen.getByRole("navigation", { name: "Navigation mobile" }));
  expect(mobile.getByRole("link", { name })).toHaveAttribute("href", href);
  for (const [module, destination] of [["Élèves", "/eleves"], ["Notes", "/notes"], ["Bulletins", "/bulletins"]]) {
    expect(mobile.getByRole("link", { name: module })).toHaveAttribute("href", destination);
    expect(within(screen.getByRole("navigation", { name: "Navigation principale" })).getByRole("link", { name: module })).toHaveAttribute("href", destination);
  }
  expect(screen.getByRole("link", { name: "Accéder à mon espace" })).toHaveAttribute("href", href);
  expect(screen.getByRole("heading", { name: authenticated ? "Retrouvez votre espace scolaire" : "Connectez-vous à votre espace" })).toBeInTheDocument();
});

it("met immédiatement à jour l’accueil et le menu ouvert quand la session change", async () => {
  const user = userEvent.setup();
  const { rerender } = render(home(false));
  await user.click(screen.getByRole("button", { name: "Ouvrir le menu" }));
  for (const authenticated of [true, false]) {
    rerender(home(authenticated));
    const name = authenticated ? "Mon espace" : "Connexion";
    const href = authenticated ? "/espace" : "/connexion";
    expect(within(screen.getByRole("banner")).getAllByRole("link", { name })).toHaveLength(2);
    for (const link of within(screen.getByRole("banner")).getAllByRole("link", { name })) expect(link).toHaveAttribute("href", href);
    expect(screen.queryByRole("link", { name: authenticated ? "Connexion" : "Mon espace" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Accéder à mon espace" })).toHaveAttribute("href", href);
    if (authenticated) {
      expect(screen.getByText(/Votre session est active/)).toBeInTheDocument();
      expect(screen.queryByRole("heading", { name: "Connectez-vous à votre espace" })).not.toBeInTheDocument();
    } else {
      expect(screen.getByRole("heading", { name: "Connectez-vous à votre espace" })).toBeInTheDocument();
      expect(screen.queryByText(/Votre session est active/)).not.toBeInTheDocument();
    }
  }
});


it("présente uniquement la consultation des notes au RESPONSABLE connecté", () => {
  render(home(true, "RESPONSABLE"));
  expect(screen.getByRole("link", { name: "Consulter les notes" })).toHaveAttribute("href", "/notes");
  expect(screen.getByRole("link", { name: "Voir les résultats" })).toHaveAttribute("href", "/notes");
  expect(screen.getByText("Consultez les résultats et suivez la progression de vos élèves.")).toBeInTheDocument();
  expect(screen.getByText(/Retrouvez les dossiers de vos élèves, consultez leurs résultats/)).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: /Saisir une note|Gérer les notes/ })).not.toBeInTheDocument();
  expect(screen.queryByText(/Saisissez les résultats|simplifiez la saisie des notes/)).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Consulter les élèves" })).toHaveAttribute("href", "/eleves");
  expect(screen.getByRole("link", { name: "Voir les bulletins" })).toHaveAttribute("href", "/bulletins");
});

it.each([[false, null], [true, "ADMIN"], [true, "ENSEIGNANT"]])("conserve les textes de saisie pour connecté=%s, rôle=%s", (authenticated, role) => {
  render(home(authenticated, role));
  expect(screen.getByRole("link", { name: "Saisir une note" })).toHaveAttribute("href", "/notes");
  expect(screen.getByRole("link", { name: "Gérer les notes" })).toHaveAttribute("href", "/notes");
  expect(screen.getByText("Saisissez les résultats et accompagnez la progression de chaque élève.")).toBeInTheDocument();
  expect(screen.getByText(/Centralisez les dossiers des élèves, simplifiez la saisie des notes/)).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Consulter les notes" })).not.toBeInTheDocument();
});

it("adapte immédiatement les textes au changement de profil sans modifier les destinations", () => {
  const { rerender } = render(home(true, "ADMIN"));
  const destinations = () => screen.getAllByRole("link").map((link) => link.getAttribute("href"));
  const before = destinations();
  rerender(home(true, "RESPONSABLE"));
  expect(screen.getByRole("link", { name: "Consulter les notes" })).toBeInTheDocument();
  expect(destinations()).toEqual(before);
  rerender(home(false));
  expect(screen.getByRole("link", { name: "Saisir une note" })).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Voir les résultats" })).not.toBeInTheDocument();
});
