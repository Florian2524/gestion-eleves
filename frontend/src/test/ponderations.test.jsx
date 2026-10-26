import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { beforeEach, expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import { PonderationsPage } from "../pages/AdministrationPages";
import { listResource, updateResource } from "../api/schoolApi";

vi.mock("../api/schoolApi", () => ({ listResource: vi.fn(), createResource: vi.fn(), updateResource: vi.fn(), deleteResource: vi.fn() }));
beforeEach(() => {
  vi.clearAllMocks();
  listResource.mockImplementation((resource) => Promise.resolve({
    ponderations: [{ idClasse: 25, idMatiere: 28, coefficientMatiere: 2 }],
    classes: [{ idClasse: 25, nom: "6A", anneeScolaire: "2026-2027" }],
    matieres: [{ idMatiere: 28, nom: "Mathématiques" }],
  }[resource] ?? []));
  updateResource.mockResolvedValue({});
});
function page(role = "ADMIN") {
  return render(<AuthContext.Provider value={{ auth: { role } }}><MemoryRouter><PonderationsPage /></MemoryRouter></AuthContext.Provider>);
}
it("modifie le coefficient avec une clé composée et protège les deux références", async () => {
  page();
  const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: "Modifier" }));
  expect(screen.getByLabelText("Classe")).toBeDisabled();
  expect(screen.getByLabelText("Matière")).toBeDisabled();
  await user.clear(screen.getByLabelText("Coefficient"));
  await user.type(screen.getByLabelText("Coefficient"), "3.25");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(updateResource).toHaveBeenCalledWith("ponderations", "25/28", { idClasse: 25, idMatiere: 28, coefficientMatiere: 3.25 }));
});
it("affiche un conflit sans perdre le formulaire", async () => {
  page();
  updateResource.mockRejectedValue(new Error("Conflit de pondération"));
  const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: "Modifier" }));
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Conflit de pondération");
  expect(screen.getByLabelText("Coefficient")).toHaveValue(2);
});
