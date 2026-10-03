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

test("un administrateur peut créer, modifier et supprimer un responsable légal", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const nom = `Responsable${suffix}`;
  const prenom = "Test";
  const email = `responsable-${suffix}@example.test`;
  const profession = "Architecte";
  const professionModifiee = "Ingénieur";

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

  await page.goto("/responsables");

  await expect(
    page.getByRole("heading", {
      name: "Responsables légaux",
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
    .fill(email);

  await page
    .getByLabel("Téléphone", { exact: true })
    .fill("0611111111");

  await page
    .getByLabel("Adresse", { exact: true })
    .fill("2 rue du Test");

  await page
    .getByLabel("Profession", { exact: true })
    .fill(profession);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Création enregistrée."),
  ).toBeVisible();

  let responsable = page
    .locator("article")
    .filter({ hasText: email });

  await expect(responsable).toBeVisible();
  await expect(responsable).toContainText(`${prenom} ${nom}`);
  await expect(responsable).toContainText(profession);

  /*
   * Modification
   */
  await responsable
    .getByRole("button", { name: "Modifier" })
    .click();

  await page
    .getByLabel("Profession", { exact: true })
    .fill(professionModifiee);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Modification enregistrée."),
  ).toBeVisible();

  responsable = page
    .locator("article")
    .filter({ hasText: email });

  await expect(responsable).toBeVisible();
  await expect(responsable).toContainText(professionModifiee);

  /*
   * Suppression
   */
  page.once("dialog", async (dialog) => {
    await dialog.accept();
  });

  await responsable
    .getByRole("button", { name: "Supprimer" })
    .click();

  await expect(
    page.getByText("Suppression effectuée."),
  ).toBeVisible();

  await expect(responsable).toHaveCount(0);
});
