import { expect, test } from "@playwright/test";

const adminEmail = process.env.E2E_ADMIN_EMAIL;
const adminPassword = process.env.E2E_ADMIN_PASSWORD;

test.beforeAll(() => {
  if (!adminEmail || !adminPassword) {
    throw new Error(
      "E2E_ADMIN_EMAIL et E2E_ADMIN_PASSWORD doivent être définis.",
    );
  }
});

test("un administrateur peut se connecter et gérer une matière", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);
  const code = `E2E${suffix}`;
  const nom = `Matière E2E ${suffix}`;

  await page.goto("/connexion");

  await page
    .getByLabel("Adresse électronique")
    .fill(adminEmail);

  await page
    .locator("#motDePasse")
    .fill(adminPassword);

  await page
    .getByRole("button", { name: "Se connecter" })
    .click();

  await expect(page).toHaveURL(/\/espace$/);

  await expect(
    page
      .getByRole("main")
      .getByText("Administrateur", { exact: true }),
  ).toBeVisible();

  await page.goto("/matieres");

  await expect(
    page.getByRole("heading", {
      name: "Matières",
      exact: true,
    }),
  ).toBeVisible();

  await page
    .getByRole("button", { name: "Ajouter" })
    .click();

  await page
    .getByLabel("Code")
    .fill(code);

  await page
    .getByLabel("Nom")
    .fill(nom);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Création enregistrée."),
  ).toBeVisible();

  const matiere = page
    .locator("article")
    .filter({ hasText: nom });

  await expect(matiere).toBeVisible();
  await expect(matiere).toContainText(code);

  page.once("dialog", async (dialog) => {
    await dialog.accept();
  });

  await matiere
    .getByRole("button", { name: "Supprimer" })
    .click();

  await expect(
    page.getByText("Suppression effectuée."),
  ).toBeVisible();

  await expect(matiere).toHaveCount(0);
});
