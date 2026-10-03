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

test("un administrateur peut créer, modifier et supprimer une période", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const libelle = `Période E2E ${suffix}`;
  const libelleModifie = `Période E2E ${suffix} modifiée`;

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

  await page.goto("/periodes");

  await expect(
    page.getByRole("heading", {
      name: "Périodes",
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
    .getByLabel("Libellé", { exact: true })
    .fill(libelle);

  await page
    .getByLabel("Début", { exact: true })
    .fill("2026-09-01");

  await page
    .getByLabel("Fin", { exact: true })
    .fill("2026-12-18");

  await page
    .getByLabel("Début de saisie", { exact: true })
    .fill("2026-11-15");

  await page
    .getByLabel("Fin de saisie", { exact: true })
    .fill("2026-12-10");

  await page
    .getByLabel("Statut", { exact: true })
    .fill("OUVERTE");

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Création enregistrée."),
  ).toBeVisible();

  let periode = page
    .locator("article")
    .filter({ hasText: libelle });

  await expect(periode).toBeVisible();
  await expect(periode).toContainText("OUVERTE");

  /*
   * Modification
   */
  await periode
    .getByRole("button", { name: "Modifier" })
    .click();

  await page
    .getByLabel("Libellé", { exact: true })
    .fill(libelleModifie);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Modification enregistrée."),
  ).toBeVisible();

  periode = page
    .locator("article")
    .filter({ hasText: libelleModifie });

  await expect(periode).toBeVisible();

  /*
   * Suppression
   */
  page.once("dialog", async (dialog) => {
    await dialog.accept();
  });

  await periode
    .getByRole("button", { name: "Supprimer" })
    .click();

  await expect(
    page.getByText("Suppression effectuée."),
  ).toBeVisible();

  await expect(periode).toHaveCount(0);
});
