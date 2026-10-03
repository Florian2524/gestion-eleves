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

test("un administrateur peut créer, modifier et supprimer une classe", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const nom = `Classe E2E ${suffix}`;
  const niveau = "5e";
  const niveauModifie = "4e";
  const annee = "2026-2027";

  /*
   * Connexion
   */
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

  /*
   * Accès Classes
   */
  await page.goto("/classes");

  await expect(
    page.getByRole("heading", {
      name: "Classes",
      exact: true,
    }),
  ).toBeVisible();

  /*
   * Création
   */
  await page
    .getByRole("button", { name: "Ajouter" })
    .click();

  await page
    .getByLabel("Nom", { exact: true })
    .fill(nom);

  await page
    .getByLabel("Niveau", { exact: true })
    .fill(niveau);

  await page
    .getByLabel("Année scolaire", { exact: true })
    .fill(annee);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Création enregistrée."),
  ).toBeVisible();

  let classe = page
    .locator("article")
    .filter({ hasText: nom });

  await expect(classe).toBeVisible();
  await expect(classe).toContainText(niveau);
  await expect(classe).toContainText(annee);
  await expect(classe).toContainText("Aucun");

  /*
   * Modification
   */
  await classe
    .getByRole("button", { name: "Modifier" })
    .click();

  await page
    .getByLabel("Niveau", { exact: true })
    .fill(niveauModifie);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Modification enregistrée."),
  ).toBeVisible();

  classe = page
    .locator("article")
    .filter({ hasText: nom });

  await expect(classe).toBeVisible();
  await expect(classe).toContainText(niveauModifie);

  /*
   * Suppression
   */
  page.once("dialog", async (dialog) => {
    await dialog.accept();
  });

  await classe
    .getByRole("button", { name: "Supprimer" })
    .click();

  await expect(
    page.getByText("Suppression effectuée."),
  ).toBeVisible();

  await expect(classe).toHaveCount(0);
});
