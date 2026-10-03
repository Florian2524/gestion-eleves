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

test("un administrateur peut créer, modifier et supprimer une scolarité", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const eleveNom = `Scolarite${suffix}`;
  const elevePrenom = "Eleve";
  const matricule = `SCOL-E2E-${suffix}`;

  const classeNom = `Classe Scolarite ${suffix}`;
  const annee = "2026-2027";

  const dateDebut = "2026-09-01";
  const dateFin = "2027-06-30";

  let eleve;
  let classe;

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
   * Création des prérequis par API.
   *
   * Le comportement réellement testé reste la gestion
   * de la scolarité depuis l'interface utilisateur.
   */
  const eleveResponse = await page.request.post(
    `${apiBase}/eleves`,
    {
      headers,
      data: {
        nom: eleveNom,
        prenom: elevePrenom,
        matricule,
        dateNaissance: "2012-05-15",
        emailContact: `scolarite-${suffix}@example.test`,
        telephone: null,
        adresse: null,
        photoUrl: null,
      },
    },
  );

  expect(eleveResponse.ok()).toBeTruthy();
  eleve = await eleveResponse.json();

  const classeResponse = await page.request.post(
    `${apiBase}/classes`,
    {
      headers,
      data: {
        nom: classeNom,
        niveau: "5e",
        anneeScolaire: annee,
        idProfesseurPrincipal: null,
      },
    },
  );

  expect(classeResponse.ok()).toBeTruthy();
  classe = await classeResponse.json();

  try {
    /*
     * Accès Scolarités
     */
    await page.goto("/scolarites");

    await expect(
      page.getByRole("heading", {
        name: "Scolarités et affectations",
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

    await expect(selects).toHaveCount(2);

    await selects
      .nth(0)
      .selectOption(String(eleve.idPersonne));

    await selects
      .nth(1)
      .selectOption(String(classe.idClasse));

    await page
      .getByLabel("Début", { exact: true })
      .fill(dateDebut);

    await page
      .getByLabel("Statut", { exact: true })
      .fill("ACTIVE");

    await page
      .getByRole("button", { name: "Enregistrer" })
      .click();

    await expect(
      page.getByText("Création enregistrée."),
    ).toBeVisible();

    const eleveLabel = `${elevePrenom} ${eleveNom}`;

    let scolarite = page
      .locator("article")
      .filter({ hasText: eleveLabel })
      .filter({ hasText: classeNom });

    await expect(scolarite).toBeVisible();
    await expect(scolarite).toContainText("ACTIVE");
    await expect(scolarite).toContainText(classeNom);

    /*
     * Modification
     */
    await scolarite
      .getByRole("button", { name: "Modifier" })
      .click();

    await page
      .getByLabel("Fin", { exact: true })
      .fill(dateFin);

    await page
      .getByLabel("Statut", { exact: true })
      .fill("TERMINEE");

    await page
      .getByRole("button", { name: "Enregistrer" })
      .click();

    await expect(
      page.getByText("Modification enregistrée."),
    ).toBeVisible();

    scolarite = page
      .locator("article")
      .filter({ hasText: eleveLabel })
      .filter({ hasText: classeNom });

    await expect(scolarite).toBeVisible();
    await expect(scolarite).toContainText("TERMINEE");

    /*
     * Suppression
     */
    page.once("dialog", async (dialog) => {
      await dialog.accept();
    });

    await scolarite
      .getByRole("button", { name: "Supprimer" })
      .click();

    await expect(
      page.getByText("Suppression effectuée."),
    ).toBeVisible();

    await expect(scolarite).toHaveCount(0);
  } finally {
    /*
     * Nettoyage de sécurité.
     *
     * Si le test échoue après la création de la scolarité,
     * on recherche et supprime d'abord cette relation,
     * puis les données temporaires.
     */
    try {
      const response = await page.request.get(
        `${apiBase}/scolarites`,
        { headers },
      );

      if (response.ok()) {
        const scolarites = await response.json();

        const restante = scolarites.find(
          (item) =>
            item.idEleve === eleve?.idPersonne &&
            item.idClasse === classe?.idClasse &&
            item.dateDebut === dateDebut,
        );

        if (restante?.idScolarite) {
          await page.request.delete(
            `${apiBase}/scolarites/${restante.idScolarite}`,
            { headers },
          );
        }
      }
    } catch {
      // Nettoyage best effort.
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

    if (eleve?.idPersonne) {
      try {
        await page.request.delete(
          `${apiBase}/eleves/${eleve.idPersonne}`,
          { headers },
        );
      } catch {
        // Nettoyage best effort.
      }
    }
  }
});
