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

test("un administrateur peut créer, modifier et supprimer un enseignant", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const nom = `Enseignant${suffix}`;
  const nomModifie = `EnseignantModifie${suffix}`;
  const prenom = "Test";
  const numeroEmploye = `ENS-E2E-${suffix}`;

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

  await page.goto("/enseignants");

  await expect(
    page.getByRole("heading", {
      name: "Enseignants",
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
    .getByLabel("Prénom", { exact: true })
    .fill(prenom);

  await page
    .getByLabel("E-mail de contact", { exact: true })
    .fill(`enseignant-${suffix}@example.test`);

  await page
    .getByLabel("Téléphone", { exact: true })
    .fill("0600000000");

  await page
    .getByLabel("Adresse", { exact: true })
    .fill("1 rue du Test");

  await page
    .getByLabel("Numéro d'employé", { exact: true })
    .fill(numeroEmploye);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Création enregistrée."),
  ).toBeVisible();

  let enseignant = page
    .locator("article")
    .filter({ hasText: numeroEmploye });

  await expect(enseignant).toBeVisible();
  await expect(enseignant).toContainText(`${prenom} ${nom}`);
  await expect(enseignant).toContainText(numeroEmploye);

  /*
   * Modification
   */
  await enseignant
    .getByRole("button", { name: "Modifier" })
    .click();

  await page
    .getByLabel("Nom", { exact: true })
    .fill(nomModifie);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Modification enregistrée."),
  ).toBeVisible();

  enseignant = page
    .locator("article")
    .filter({ hasText: numeroEmploye });

  await expect(enseignant).toBeVisible();
  await expect(enseignant).toContainText(
    `${prenom} ${nomModifie}`,
  );

  /*
   * Suppression
   */
  page.once("dialog", async (dialog) => {
    await dialog.accept();
  });

  await enseignant
    .getByRole("button", { name: "Supprimer" })
    .click();

  await expect(
    page.getByText("Suppression effectuée."),
  ).toBeVisible();

  await expect(enseignant).toHaveCount(0);
});
