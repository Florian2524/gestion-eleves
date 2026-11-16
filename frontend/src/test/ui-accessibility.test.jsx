import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, it, vi } from "vitest";
import Button from "../components/ui/Button";
import Field from "../components/ui/Field";
import HomePage from "../pages/HomePage";
import { AuthContext } from "../context/authContextCore";

it("ouvre les modules annoncés depuis les actions et les cartes publiques", () => {
  render(<AuthContext.Provider value={{ isAuthenticated: false }}><HomePage /></AuthContext.Provider>);
  for (const [name, destination] of [
    ["Consulter les élèves", "/eleves"], ["Saisir une note", "/notes"],
    ["Voir les élèves", "/eleves"], ["Gérer les notes", "/notes"],
    ["Voir les bulletins", "/bulletins"],
  ]) expect(screen.getByRole("link", { name })).toHaveAttribute("href", destination);
});

it.each(["input", "select", "textarea"])("associe les descriptions au contrôle %s et retire les erreurs résolues", (as) => {
  const content = (error) => <><p id="external">Description externe</p><Field as={as} id="example" label="Exemple" hint="Aide" error={error} aria-describedby="external" /></>;
  const { rerender } = render(content("Erreur"));
  expect(screen.getByLabelText("Exemple")).toHaveAccessibleDescription("Description externe Aide Erreur");
  expect(screen.getByLabelText("Exemple")).toHaveAttribute("aria-invalid", "true");
  expect(screen.getByRole("alert")).toHaveTextContent("Erreur");
  rerender(content(undefined));
  expect(screen.getByLabelText("Exemple")).toHaveAccessibleDescription("Description externe Aide");
  expect(screen.getByLabelText("Exemple")).toHaveAttribute("aria-invalid", "false");
});

it("ignore l’activation d’un bouton désactivé et le saute au clavier", async () => {
  const user = userEvent.setup();
  const action = vi.fn();
  render(<><Button disabled onClick={action}>Enregistrer</Button><Button>Annuler</Button></>);
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await user.tab();
  expect(screen.getByRole("button", { name: "Annuler" })).toHaveFocus();
  expect(action).not.toHaveBeenCalled();
});
