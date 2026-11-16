import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import DashboardPage from "../pages/DashboardPage";

const groups = {
  "Suivi des élèves": [["Élèves", "/eleves"], ["Classes", "/classes"], ["Inscriptions", "/inscriptions"], ["Scolarités", "/scolarites"]],
  "Pédagogie et évaluations": [["Notes", "/notes"], ["Bulletins", "/bulletins"], ["Enseignements", "/enseignements"], ["Évaluations", "/evaluations"], ["Matières", "/matieres"], ["Pondérations", "/ponderations"], ["Périodes", "/periodes"]],
  Administration: [["Enseignants", "/enseignants"], ["Responsables", "/responsables"], ["Responsabilités légales", "/responsabilites-legales"], ["Comptes utilisateurs", "/comptes-utilisateurs"]],
};
const shared = ["Élèves", "Notes", "Bulletins", "Enseignements", "Évaluations"];
function dashboard(role) {
  render(<AuthContext.Provider value={{ auth: { role, emailConnexion: "utilisateur@example.fr" }, logout: vi.fn() }}><MemoryRouter><DashboardPage /></MemoryRouter></AuthContext.Provider>);
}

it.each(["ADMIN", "ENSEIGNANT", "RESPONSABLE"])("conserve exactement les modules, les destinations et les rubriques pour %s", (role) => {
  dashboard(role);
  const count = role === "ADMIN" ? 15 : 5;
  expect(screen.getAllByRole("article")).toHaveLength(count);
  expect(screen.getAllByRole("link", { name: /^Ouvrir le module / })).toHaveLength(count);
  for (const [group, entries] of Object.entries(groups)) {
    const visible = entries.filter(([name]) => role === "ADMIN" || shared.includes(name));
    if (!visible.length) {
      expect(screen.queryByRole("region", { name: group })).not.toBeInTheDocument();
      expect(screen.queryByRole("heading", { name: group })).not.toBeInTheDocument();
      continue;
    }
    const section = within(screen.getByRole("region", { name: group }));
    expect(section.getAllByRole("article")).toHaveLength(visible.length);
    for (const [name, destination] of visible) {
      expect(section.getByRole("heading", { level: 3, name })).toBeInTheDocument();
      expect(section.getByRole("link", { name: `Ouvrir le module ${name}` })).toHaveAttribute("href", destination);
    }
  }
  for (const [name] of Object.values(groups).flat()) {
    if (role !== "ADMIN" && !shared.includes(name)) expect(screen.queryByRole("heading", { name })).not.toBeInTheDocument();
  }
  expect(within(screen.getByRole("main")).getByText("utilisateur@example.fr")).toBeInTheDocument();
  expect(within(screen.getByRole("banner")).getByRole("link", { name: /Scolarité/ })).toHaveAttribute("href", "/");
});

it("présente uniquement des descriptions de consultation au responsable", () => {
  dashboard("RESPONSABLE");
  for (const description of [
    "Consulter les dossiers et les informations scolaires de vos élèves.",
    "Consulter les résultats des évaluations de vos élèves.",
    "Consulter les moyennes de vos élèves et télécharger leurs bulletins PDF.",
    "Consulter les enseignements associés aux classes et aux matières.",
    "Consulter les évaluations et leurs barèmes.",
  ]) expect(screen.getByText(description)).toBeInTheDocument();
  for (const article of screen.getAllByRole("article")) expect(article).not.toHaveTextContent(/saisir|modifier|créer|administrer|gérer|calculer/i);
  expect(within(screen.getByRole("main")).getByText("Responsable légal")).toBeInTheDocument();
});

it("présente la saisie des notes et la consultation pédagogique à l’enseignant", () => {
  dashboard("ENSEIGNANT");
  expect(screen.getByText("Saisir et consulter les résultats des évaluations.")).toBeInTheDocument();
  expect(screen.getByText("Consulter les évaluations et leurs barèmes.")).toBeInTheDocument();
  expect(within(screen.getByRole("main")).getByText("Enseignant")).toBeInTheDocument();
});
