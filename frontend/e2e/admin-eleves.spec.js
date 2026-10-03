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

test("un administrateur peut créer, modifier et supprimer un élève", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const nom = `Eleve${suffix}`;
  const nomModifie = `EleveModifie${suffix}`;
  const prenom = "Test";
  const matricule = `ELE-E2E-${suffix}`;
  const email = `eleve-${suffix}@example.test`;

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
   * Accès élèves
   */
  await page.goto("/eleves");

  await expect(
    page.getByRole("heading", {
      name: "Liste des élèves",
      exact: true,
    }),
  ).toBeVisible();

  /*
   * Création
   */
  await page
    .getByRole("button", { name: "Ajouter un élève" })
    .click();

  await expect(
    page.getByRole("heading", {
      name: "Ajouter un élève",
      exact: true,
    }),
  ).toBeVisible();

  await page
    .getByLabel(/^Nom/)
    .fill(nom);

  await page
    .getByLabel(/^Prénom/)
    .fill(prenom);

  await page
    .getByLabel(/^Matricule/)
    .fill(matricule);

  await page
    .getByLabel(/^Date de naissance/)
    .fill("2012-05-15");

  await page
    .getByLabel("E-mail", { exact: true })
    .fill(email);

  await page
    .getByLabel("Téléphone", { exact: true })
    .fill("0622222222");

  await page
    .getByLabel("Adresse", { exact: true })
    .fill("3 rue du Test");

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Élève enregistré.", { exact: true }),
  ).toBeVisible();

  /*
   * Vérification dans le tableau
   */
  let eleve = page
    .locator("tbody tr")
    .filter({ hasText: matricule });

  await expect(eleve).toBeVisible();
  await expect(eleve).toContainText(nom);
  await expect(eleve).toContainText(prenom);
  await expect(eleve).toContainText(matricule);

  /*
   * Modification
   */
  await eleve
    .getByRole("button", { name: "Modifier" })
    .click();

  await expect(
    page.getByRole("heading", {
      name: "Modifier l’élève",
      exact: true,
    }),
  ).toBeVisible();

  await page
    .getByLabel(/^Nom/)
    .fill(nomModifie);

  await page
    .getByRole("button", { name: "Enregistrer" })
    .click();

  await expect(
    page.getByText("Élève enregistré.", { exact: true }),
  ).toBeVisible();

  eleve = page
    .locator("tbody tr")
    .filter({ hasText: matricule });

  await expect(eleve).toBeVisible();
  await expect(eleve).toContainText(nomModifie);

  /*
   * Recherche
   */
  const recherche = page.getByPlaceholder(
    "Nom, prénom, matricule ou contact...",
  );

  await recherche.fill(matricule);

  eleve = page
    .locator("tbody tr")
    .filter({ hasText: matricule });

  await expect(eleve).toBeVisible();

  /*
   * Suppression
   */
  page.once("dialog", async (dialog) => {
    await dialog.accept();
  });

  await eleve
    .getByRole("button", { name: "Supprimer" })
    .click();

  await expect(
    page.getByText("Élève supprimé.", { exact: true }),
  ).toBeVisible();

  await expect(eleve).toHaveCount(0);
});
