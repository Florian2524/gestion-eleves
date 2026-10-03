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

test("un administrateur peut créer, modifier et supprimer une évaluation", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-8);

  const enseignantNom = `ProfEval${suffix}`;
  const enseignantPrenom = "Test";
  const numeroEmploye = `ENS-EVAL-${suffix}`;

  const classeNom = `Classe Evaluation ${suffix}`;
  const annee = "2026-2027";

  const matiereNom = `Matière Evaluation ${suffix}`;
  const matiereCode = `EVAL-${suffix}`;

  const periodeLibelle = `Période Evaluation ${suffix}`;

  const evaluationLibelle = `Contrôle E2E ${suffix}`;
  const evaluationLibelleModifie =
    `Contrôle E2E ${suffix} modifié`;

  const dateDebut = "2026-09-01";
  const dateFin = "2026-12-31";
  const dateEvaluation = "2026-10-15";

  let enseignant;
  let classe;
  let matiere;
  let enseignement;
  let periode;

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
    const value =
      localStorage.getItem("gestion-eleves.auth");

    return value
      ? JSON.parse(value)
      : null;
  });

  expect(storedAuth?.token).toBeTruthy();

  const headers = {
    Authorization: `Bearer ${storedAuth.token}`,
  };

  try {
    /*
     * Préparation : enseignant
     */
    const enseignantResponse =
      await page.request.post(
        `${apiBase}/enseignants`,
        {
          headers,
          data: {
            nom: enseignantNom,
            prenom: enseignantPrenom,
            emailContact:
              `evaluation-${suffix}@example.test`,
            telephone: null,
            adresse: null,
            numeroEmploye,
          },
        },
      );

    expect(enseignantResponse.ok()).toBeTruthy();
    enseignant = await enseignantResponse.json();

    /*
     * Préparation : classe
     */
    const classeResponse =
      await page.request.post(
        `${apiBase}/classes`,
        {
          headers,
          data: {
            nom: classeNom,
            niveau: "3e",
            anneeScolaire: annee,
            idProfesseurPrincipal: null,
          },
        },
      );

    expect(classeResponse.ok()).toBeTruthy();
    classe = await classeResponse.json();

    /*
     * Préparation : matière
     */
    const matiereResponse =
      await page.request.post(
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

    /*
     * Préparation : enseignement
     */
    const enseignementResponse =
      await page.request.post(
        `${apiBase}/enseignements`,
        {
          headers,
          data: {
            idEnseignant:
              enseignant.idPersonne,
            idClasse:
              classe.idClasse,
            idMatiere:
              matiere.idMatiere,
            coefficientMatiere: 2,
            dateDebut,
            dateFin: null,
            actif: true,
          },
        },
      );

    expect(
      enseignementResponse.ok(),
    ).toBeTruthy();

    enseignement =
      await enseignementResponse.json();

    /*
     * Préparation : période
     */
    const periodeResponse =
      await page.request.post(
        `${apiBase}/periodes`,
        {
          headers,
          data: {
            libelle: periodeLibelle,
            dateDebut,
            dateFin,
            dateDebutSaisie: null,
            dateFinSaisie: null,
            statut: "OUVERTE",
          },
        },
      );

    expect(periodeResponse.ok()).toBeTruthy();
    periode = await periodeResponse.json();

    /*
     * Accès Évaluations
     */
    await page.goto("/evaluations");

    await expect(
      page.getByRole("heading", {
        name: "Évaluations",
        exact: true,
      }),
    ).toBeVisible();

    /*
     * Création depuis l'interface
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
      .selectOption(
        String(enseignement.idEnseignement),
      );

    await selects
      .nth(1)
      .selectOption(
        String(periode.idPeriode),
      );

    await form
      .getByLabel("Libellé", { exact: true })
      .fill(evaluationLibelle);

    await form
      .getByLabel("Date", { exact: true })
      .fill(dateEvaluation);

    await form
      .getByLabel("Type", { exact: true })
      .fill("Contrôle");

    await form
      .getByLabel("Coefficient", { exact: true })
      .fill("2");

    await form
      .getByLabel("Barème", { exact: true })
      .fill("20");

    await form
      .getByRole("button", {
        name: "Enregistrer",
      })
      .click();

    await expect(
      page.getByText("Création enregistrée."),
    ).toBeVisible();

    let evaluation = page
      .locator("article")
      .filter({
        hasText: evaluationLibelle,
      });

    await expect(evaluation).toBeVisible();
    await expect(evaluation)
      .toContainText(matiereNom);

    await expect(evaluation)
      .toContainText(classeNom);

    await expect(evaluation)
      .toContainText(periodeLibelle);

    await expect(evaluation)
      .toContainText("20");

    /*
     * Modification
     */
    await evaluation
      .getByRole("button", {
        name: "Modifier",
      })
      .click();

    const editForm = page.locator("form");

    await expect(editForm).toBeVisible();

    await editForm
      .getByLabel("Libellé", { exact: true })
      .fill(evaluationLibelleModifie);

    await editForm
      .getByLabel("Coefficient", {
        exact: true,
      })
      .fill("3");

    await editForm
      .getByLabel("Barème", { exact: true })
      .fill("40");

    await editForm
      .getByRole("button", {
        name: "Enregistrer",
      })
      .click();

    await expect(
      page.getByText(
        "Modification enregistrée.",
      ),
    ).toBeVisible();

    evaluation = page
      .locator("article")
      .filter({
        hasText:
          evaluationLibelleModifie,
      });

    await expect(evaluation).toBeVisible();
    await expect(evaluation)
      .toContainText("40");

    /*
     * Suppression
     */
    page.once(
      "dialog",
      async (dialog) => {
        await dialog.accept();
      },
    );

    await evaluation
      .getByRole("button", {
        name: "Supprimer",
      })
      .click();

    await expect(
      page.getByText(
        "Suppression effectuée.",
      ),
    ).toBeVisible();

    await expect(evaluation)
      .toHaveCount(0);
  } finally {
    /*
     * Nettoyage : évaluation éventuelle
     */
    if (
      enseignement?.idEnseignement &&
      periode?.idPeriode
    ) {
      try {
        const response =
          await page.request.get(
            `${apiBase}/evaluations`,
            { headers },
          );

        if (response.ok()) {
          const evaluations =
            await response.json();

          const restante =
            evaluations.find(
              (item) =>
                item.idEnseignement ===
                  enseignement.idEnseignement &&
                item.idPeriode ===
                  periode.idPeriode &&
                (
                  item.libelle ===
                    evaluationLibelle ||
                  item.libelle ===
                    evaluationLibelleModifie
                ),
            );

          if (restante?.idEvaluation) {
            await page.request.delete(
              `${apiBase}/evaluations/${restante.idEvaluation}`,
              { headers },
            );
          }
        }
      } catch {
        // Nettoyage best effort.
      }
    }

    /*
     * Nettoyage : enseignement
     */
    if (enseignement?.idEnseignement) {
      try {
        await page.request.delete(
          `${apiBase}/enseignements/${enseignement.idEnseignement}`,
          { headers },
        );
      } catch {
        // Nettoyage best effort.
      }
    }

    /*
     * Nettoyage : période
     */
    if (periode?.idPeriode) {
      try {
        await page.request.delete(
          `${apiBase}/periodes/${periode.idPeriode}`,
          { headers },
        );
      } catch {
        // Nettoyage best effort.
      }
    }

    /*
     * Nettoyage : matière
     */
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

    /*
     * Nettoyage : classe
     */
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

    /*
     * Nettoyage : enseignant
     */
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
