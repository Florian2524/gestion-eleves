import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { beforeEach, expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import { ResponsablesPage, EnseignantsPage } from "../pages/AdministrationPages";
import ComptesUtilisateursPage from "../pages/ComptesUtilisateursPage";
import { attachProfile, createResource, listResource } from "../api/schoolApi";

vi.mock("../api/schoolApi", () => ({ listResource: vi.fn(), createResource: vi.fn(), updateResource: vi.fn(), deleteResource: vi.fn(), attachProfile: vi.fn() }));
let people, guardians;
beforeEach(() => {
  vi.clearAllMocks();
  people = [
    { idPersonne: 1, nom: "Martin", prenom: "Alex", emailContact: "alex@example.test", profils: ["ENSEIGNANT"] },
    { idPersonne: 2, nom: "Dubois", prenom: "Camille", profils: ["ENSEIGNANT", "RESPONSABLE"] },
    { idPersonne: 3, nom: "Libre", prenom: "Personne", profils: [] },
  ];
  guardians = [{ idPersonne: 2, nom: "Dubois", prenom: "Camille", profession: "Architecte" }];
  listResource.mockImplementation((resource) => Promise.resolve({ personnes: people, responsables: guardians, enseignants: [people[0]], comptes: [] }[resource] ?? []));
  attachProfile.mockImplementation(async (id, type, body) => {
    const person = people.find((p) => p.idPersonne === id);
    person.profils = [...person.profils, type];
    if (type === "RESPONSABLE") guardians = [...guardians, { ...person, ...body }];
    return { ...person, ...body };
  });
  createResource.mockResolvedValue({});
});
function page(Component, role = "ADMIN") {
  return render(<AuthContext.Provider value={{ auth: { role } }}><MemoryRouter><Component /></MemoryRouter></AuthContext.Provider>);
}

it("rattache explicitement un responsable à un enseignant sans transmettre de coordonnées", async () => {
  page(ResponsablesPage); const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: "Ajouter" }));
  await user.selectOptions(screen.getByLabelText("Création du profil"), "existing");
  expect(screen.queryByLabelText("Nom")).not.toBeInTheDocument();
  const select = screen.getByLabelText("Personne existante");
  expect(within(select).queryByRole("option", { name: /Camille/ })).not.toBeInTheDocument();
  await user.selectOptions(select, "1");
  expect(screen.getByText("Profils actuels : Enseignant")).toBeVisible();
  await user.type(screen.getByLabelText("Profession"), "Médecin");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(attachProfile).toHaveBeenCalledWith(1, "RESPONSABLE", { profession: "Médecin" }));
  expect(createResource).not.toHaveBeenCalled();
  expect(await screen.findByText("Personne #1 · Enseignant · Responsable légal")).toBeVisible();
});

it("conserve la création historique d'une nouvelle personne avec son premier profil", async () => {
  page(EnseignantsPage); const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: "Ajouter" }));
  expect(screen.getByLabelText("Création du profil")).toHaveValue("new");
  await user.type(screen.getByLabelText("Nom"), "Nouveau");
  await user.type(screen.getByLabelText("Prénom"), "Prof");
  await user.type(screen.getByLabelText("Numéro d'employé"), "ENS-NEW");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(createResource).toHaveBeenCalledWith("enseignants", { nom: "Nouveau", prenom: "Prof", emailContact: null, telephone: null, adresse: null, numeroEmploye: "ENS-NEW" }));
  expect(attachProfile).not.toHaveBeenCalled();
});

it("affiche le conflit de rattachement et conserve la sélection et la profession", async () => {
  page(ResponsablesPage); const user = userEvent.setup();
  attachProfile.mockRejectedValue(new Error("Ce profil existe déjà."));
  await user.click(await screen.findByRole("button", { name: "Ajouter" }));
  await user.selectOptions(screen.getByLabelText("Création du profil"), "existing");
  await user.selectOptions(screen.getByLabelText("Personne existante"), "1");
  await user.type(screen.getByLabelText("Profession"), "Profession conservée");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Ce profil existe déjà.");
  expect(screen.getByLabelText("Personne existante")).toHaveValue("1");
  expect(screen.getByLabelText("Profession")).toHaveValue("Profession conservée");
});

it("dédoublonne les comptes et garde les personnes sans profil éligibles à ADMIN", async () => {
  people.push({ ...people[1] });
  page(ComptesUtilisateursPage); const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: "Ajouter" }));
  await user.selectOptions(screen.getByLabelText("Rôle"), "ADMIN");
  const select = screen.getByLabelText("Personne");
  expect(within(select).getAllByRole("option", { name: /Camille Dubois/ })).toHaveLength(1);
  expect(within(select).getByRole("option", { name: /Sans profil/ })).toBeVisible();
  await user.selectOptions(screen.getByLabelText("Rôle"), "RESPONSABLE");
  expect(within(select).queryByRole("option", { name: /Sans profil/ })).not.toBeInTheDocument();
  expect(within(select).queryByRole("option", { name: /Alex Martin/ })).not.toBeInTheDocument();
  expect(within(select).getAllByRole("option", { name: /Camille Dubois/ })).toHaveLength(1);
});
