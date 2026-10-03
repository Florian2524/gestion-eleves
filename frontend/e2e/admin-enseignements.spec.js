import { expect, test } from "@playwright/test";

const adminEmail = process.env.E2E_ADMIN_EMAIL;
const adminPassword = process.env.E2E_ADMIN_PASSWORD;

const apiBase = "http://127.0.0.1:8080";

test.beforeAll(() => {
  if (!adminEmail || !adminPassword) {
    throw new Error(
      "E2E_ADMIN_EMAIL et E2E_ADMIN_PASSWORD doivent être définis.",
    );
  }
});

test("un administrateur peut créer, modifier et supprimer un enseignement", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const enseignantNom = `EnseignantCours${suffix}`;
  const enseignantPrenom = "Prof";
  const numeroEmploye = `ENS-COUR-${suffix}`;

  const classeNom = `Classe Cours ${suffix}`;
  const annee = "2026-2027";

  const matiereNom = `Matière Cours ${suffix}`;
  const matiereCode = `MAT-${suffix}`;

  const dateDebut = "2026-09-01";
  const dateFin = "2027-06-30";

  let enseignant;
  let classe;
  let matiere;

  /*
   * Connexion ADMIN
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

  const storedAuth = await page.evaluate(() => {
    const value = localStorage.getItem("gestion-eleves.auth");
    return value ? JSON.parse(value) : null;
  });

  expect(storedAuth?.token).toBeTruthy();

  const headers = {
    Authorization: `Bearer ${storedAuth.token}`,
  };

  /*
   * Préparation des données nécessaires par API.
   */
  const enseignantResponse = await page.request.post(
    `${apiBase}/enseignants`,
    {
      headers,
      data: {
        nom: enseignantNom,
        prenom: enseignantPrenom,
        emailContact: `cours-${suffix}@example.test`,
        telephone: null,
        adresse: null,
        numeroEmploye,
      },
    },
  );

  expect(enseignantResponse.ok()).toBeTruthy();
  enseignant = await enseignantResponse.json();

  const classeResponse = await page.request.post(
    `${apiBase}/classes`,
    {
      headers,
      data: {
        nom: classeNom,
        niveau: "4e",
        anneeScolaire: annee,
        idProfesseurPrincipal: null,
      },
    },
  );

  expect(classeResponse.ok()).toBeTruthy();
  classe = await classeResponse.json();

  const matiereResponse = await page.request.post(
    `${apiBase}/matieres`,
    {
      headers,
      data: {
        code: matiereCode,
        nom: matiereNom,
      },
    },
  );

  expect(matiereResponse.ok()).toBeTruthy();
  matiere = await matiereResponse.json();

  try {
    /*
     * Accès Enseignements
     */
    await page.goto("/enseignements");

    await expect(
      page.getByRole("heading", {
        name: "Enseignements",
        exact: true,
      }),
    ).toBeVisible();

    /*
     * Création
     */
    await page
      .getByRole("button", { name: "Ajouter" })
      .click();

    const form = page.locator("form");

    await expect(form).toBeVisible();

    const selects = form.locator("select");

    await expect(selects).toHaveCount(3);

    await selects
      .nth(0)
      .selectOption(String(enseignant.idPersonne));

    await selects
      .nth(1)
      .selectOption(String(classe.idClasse));

    await selects
      .nth(2)
      .selectOption(String(matiere.idMatiere));

    await form
      .getByLabel("Coefficient", { exact: true })
      .fill("2.5");

    await form
      .getByLabel("Début", { exact: true })
      .fill(dateDebut);

    const actif = form.locator('input[type="checkbox"]');

    await expect(actif).toBeChecked();

    await form
      .getByRole("button", { name: "Enregistrer" })
      .click();

    await expect(
      page.getByText("Création enregistrée."),
    ).toBeVisible();

    let enseignement = page
      .locator("article")
      .filter({ hasText: matiereNom })
      .filter({ hasText: classeNom })
      .filter({ hasText: enseignantNom });

    await expect(enseignement).toBeVisible();
    await expect(enseignement).toContainText(matiereNom);
    await expect(enseignement).toContainText(classeNom);
    await expect(enseignement).toContainText(enseignantNom);
    await expect(enseignement).toContainText("Actif");

    /*
     * Modification
     */
    await enseignement
      .getByRole("button", { name: "Modifier" })
      .click();

    const editForm = page.locator("form");

    await expect(editForm).toBeVisible();

    await editForm
      .getByLabel("Coefficient", { exact: true })
      .fill("3");

    await editForm
      .getByLabel("Fin", { exact: true })
      .fill(dateFin);

    const editActif =
      editForm.locator('input[type="checkbox"]');

    await editActif.uncheck();

    await editForm
      .getByRole("button", { name: "Enregistrer" })
      .click();

    await expect(
      page.getByText("Modification enregistrée."),
    ).toBeVisible();

    enseignement = page
      .locator("article")
      .filter({ hasText: matiereNom })
      .filter({ hasText: classeNom })
      .filter({ hasText: enseignantNom });

    await expect(enseignement).toBeVisible();
    await expect(enseignement).toContainText("Inactif");

    /*
     * Suppression
     */
    page.once("dialog", async (dialog) => {
      await dialog.accept();
    });

    await enseignement
      .getByRole("button", { name: "Supprimer" })
      .click();

    await expect(
      page.getByText("Suppression effectuée."),
    ).toBeVisible();

    await expect(enseignement).toHaveCount(0);
  } finally {
    /*
     * Nettoyage de sécurité.
     */
    try {
      const response = await page.request.get(
        `${apiBase}/enseignements`,
        { headers },
      );

      if (response.ok()) {
        const enseignements = await response.json();

        const restant = enseignements.find(
          (item) =>
            item.idEnseignant === enseignant?.idPersonne &&
            item.idClasse === classe?.idClasse &&
            item.idMatiere === matiere?.idMatiere &&
            item.dateDebut === dateDebut,
        );

        if (restant?.idEnseignement) {
          await page.request.delete(
            `${apiBase}/enseignements/${restant.idEnseignement}`,
            { headers },
          );
        }
      }
    } catch {
      // Nettoyage best effort.
    }

    if (matiere?.idMatiere) {
      try {
        await page.request.delete(
          `${apiBase}/matieres/${matiere.idMatiere}`,
          { headers },
        );
      } catch {
        // Nettoyage best effort.
      }
    }

    if (classe?.idClasse) {
      try {
        await page.request.delete(
          `${apiBase}/classes/${classe.idClasse}`,
          { headers },
        );
      } catch {
        // Nettoyage best effort.
      }
    }

    if (enseignant?.idPersonne) {
      try {
        await page.request.delete(
          `${apiBase}/enseignants/${enseignant.idPersonne}`,
          { headers },
        );
      } catch {
        // Nettoyage best effort.
      }
    }
  }
});
