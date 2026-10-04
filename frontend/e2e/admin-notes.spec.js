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

async function postJson(request, path, headers, data) {
  const response = await request.post(
    `${apiBase}/${path}`,
    {
      headers,
      data,
    },
  );

  if (!response.ok()) {
    const body = await response.text();

    throw new Error(
      `POST /${path} a échoué : ${response.status()} ${body}`,
    );
  }

  return response.json();
}

async function deleteBestEffort(
  request,
  path,
  headers,
) {
  try {
    await request.delete(
      `${apiBase}/${path}`,
      { headers },
    );
  } catch {
    // Nettoyage best effort.
  }
}

test("un administrateur peut créer, modifier, rechercher et supprimer une note", async ({
  page,
}) => {
  test.setTimeout(60_000);
  const suffix = Date.now().toString().slice(-8);

  const eleveNom = `NoteEleve${suffix}`;
  const elevePrenom = "Test";
  const matricule = `NOTE-${suffix}`;

  const classeNom = `Classe Note ${suffix}`;
  const annee = "2026-2027";

  const enseignantNom = `ProfNote${suffix}`;
  const enseignantPrenom = "Test";
  const numeroEmploye = `ENS-NOTE-${suffix}`;

  const matiereNom = `Matière Note ${suffix}`;
  const matiereCode = `NOT-${suffix}`;

  const periodeLibelle = `Période Note ${suffix}`;
  const evaluationLibelle = `Évaluation Note ${suffix}`;

  const dateDebut = "2026-09-01";
  const dateFinPeriode = "2026-12-31";
  const dateEvaluation = "2026-10-20";

  const commentaireInitial = "Bon travail";
  const commentaireModifie =
    "Très bon travail après correction";

  let eleve;
  let classe;
  let enseignant;
  let matiere;
  let enseignement;
  let periode;
  let scolarite;
  let evaluation;

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
    .getByRole("button", {
      name: "Se connecter",
    })
    .click();

  await expect(page)
    .toHaveURL(/\/espace$/);

  const storedAuth = await page.evaluate(() => {
    const value =
      localStorage.getItem(
        "gestion-eleves.auth",
      );

    return value
      ? JSON.parse(value)
      : null;
  });

  expect(storedAuth?.token)
    .toBeTruthy();

  const headers = {
    Authorization:
      `Bearer ${storedAuth.token}`,
  };

  try {
    /*
     * Élève
     */
    eleve = await postJson(
      page.request,
      "eleves",
      headers,
      {
        nom: eleveNom,
        prenom: elevePrenom,
        matricule,
        dateNaissance: "2012-04-12",
        emailContact:
          `note-eleve-${suffix}@example.test`,
        telephone: null,
        adresse: null,
        photoUrl: null,
      },
    );

    /*
     * Classe
     */
    classe = await postJson(
      page.request,
      "classes",
      headers,
      {
        nom: classeNom,
        niveau: "4e",
        anneeScolaire: annee,
        idProfesseurPrincipal: null,
      },
    );

    /*
     * Enseignant
     */
    enseignant = await postJson(
      page.request,
      "enseignants",
      headers,
      {
        nom: enseignantNom,
        prenom: enseignantPrenom,
        emailContact:
          `note-prof-${suffix}@example.test`,
        telephone: null,
        adresse: null,
        numeroEmploye,
      },
    );

    /*
     * Matière
     */
    matiere = await postJson(
      page.request,
      "matieres",
      headers,
      {
        code: matiereCode,
        nom: matiereNom,
      },
    );

    /*
     * Enseignement
     */
    enseignement = await postJson(
      page.request,
      "enseignements",
      headers,
      {
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
    );

    /*
     * Période
     */
    periode = await postJson(
      page.request,
      "periodes",
      headers,
      {
        libelle: periodeLibelle,
        dateDebut,
        dateFin: dateFinPeriode,
        dateDebutSaisie: null,
        dateFinSaisie: null,
        statut: "OUVERTE",
      },
    );

    /*
     * Scolarité.
     *
     * Elle utilise exactement la même classe
     * que l'enseignement de l'évaluation.
     */
    scolarite = await postJson(
      page.request,
      "scolarites",
      headers,
      {
        idEleve:
          eleve.idPersonne,
        idClasse:
          classe.idClasse,
        dateDebut,
        dateFin: null,
        statut: "ACTIVE",
      },
    );

    /*
     * Évaluation sur 20.
     */
    evaluation = await postJson(
      page.request,
      "evaluations",
      headers,
      {
        idEnseignement:
          enseignement.idEnseignement,
        idPeriode:
          periode.idPeriode,
        libelle:
          evaluationLibelle,
        dateEvaluation,
        typeEvaluation: "Contrôle",
        coefficientEvaluation: 2,
        bareme: 20,
      },
    );

    /*
     * Accès Notes
     */
    await page.goto("/notes");

    await expect(
      page.getByRole("heading", {
        name: "Notes",
        exact: true,
      }),
    ).toBeVisible();

    await expect(
      page.getByRole("heading", {
        name: "Saisir une note",
        exact: true,
      }),
    ).toBeVisible();

    const form = page.locator("form");

    await expect(form)
      .toBeVisible();

    const selects =
      form.locator("select");

    await expect(selects)
      .toHaveCount(2);

    /*
     * Sélection de l'évaluation.
     */
    await selects
      .nth(0)
      .selectOption(
        String(evaluation.idEvaluation),
      );

    /*
     * La liste des scolarités devient alors
     * compatible avec la classe sélectionnée.
     */
    await expect(
      selects.nth(1),
    ).toBeEnabled();

    await selects
      .nth(1)
      .selectOption(
        String(scolarite.idScolarite),
      );

    /*
     * Création de la note.
     */
    await form
      .locator('input[type="number"]')
      .fill("14.5");

    const commentaire = form.locator("textarea");
    const statut = form.locator('input[maxlength="30"]');

    await expect(commentaire).toHaveCount(1);
    await expect(statut).toHaveCount(1);

    await commentaire.fill(commentaireInitial);
    await statut.fill("SAISIE");

    await form
      .getByRole("button", {
        name: "Enregistrer",
      })
      .click();

    await expect(
      page.getByText(
        "Note créée.",
        { exact: true },
      ),
    ).toBeVisible();

    const eleveLabel =
      `${elevePrenom} ${eleveNom}`;

    let note = page
      .locator("article")
      .filter({
        hasText: eleveLabel,
      })
      .filter({
        hasText: evaluationLibelle,
      });

    await expect(note)
      .toBeVisible();

    await expect(note)
      .toContainText(
        evaluationLibelle,
      );

    await expect(note)
      .toContainText(
        matiereNom,
      );

    await expect(note)
      .toContainText(
        classeNom,
      );

    await expect(note)
      .toContainText(
        commentaireInitial,
      );

    await expect(note)
      .toContainText(
        "SAISIE",
      );

    await expect(note)
      .toContainText(
        "14.5",
      );

    /*
     * Modification.
     */
    await note
      .getByRole("button", {
        name: "Modifier",
      })
      .click();

    await expect(
      page.getByRole("heading", {
        name: "Modifier une note",
        exact: true,
      }),
    ).toBeVisible();

    const editForm =
      page.locator("form");

    await editForm
      .locator('input[type="number"]')
      .fill("16.75");

    const editCommentaire =
      editForm.locator("textarea");

    const editStatut =
      editForm.locator('input[maxlength="30"]');

    await expect(editCommentaire).toHaveCount(1);
    await expect(editStatut).toHaveCount(1);

    await editCommentaire.fill(commentaireModifie);
    await editStatut.fill("VALIDEE");

    await editForm
      .getByRole("button", {
        name: "Enregistrer",
      })
      .click();

    await expect(
      page.getByText(
        "Note modifiée.",
        { exact: true },
      ),
    ).toBeVisible();

    note = page
      .locator("article")
      .filter({
        hasText: eleveLabel,
      })
      .filter({
        hasText: evaluationLibelle,
      });

    await expect(note)
      .toBeVisible();

    await expect(note)
      .toContainText(
        "16.75",
      );

    await expect(note)
      .toContainText(
        "VALIDEE",
      );

    await expect(note)
      .toContainText(
        commentaireModifie,
      );

    /*
     * Recherche par matricule.
     *
     * Le matricule n'est pas forcément affiché
     * dans la carte, mais il fait partie du
     * moteur de recherche de la page.
     */
    await page
      .getByPlaceholder(
        "Élève, évaluation, statut…",
      )
      .fill(matricule);

    await expect(note)
      .toBeVisible();

    /*
     * Suppression.
     */
    page.once(
      "dialog",
      async (dialog) => {
        await dialog.accept();
      },
    );

    await note
      .getByRole("button", {
        name: "Supprimer",
      })
      .click();

    await expect(
      page.getByText(
        "Note supprimée.",
        { exact: true },
      ),
    ).toBeVisible();

    await expect(note)
      .toHaveCount(0);
  } finally {
    /*
     * Note résiduelle éventuelle.
     */
    if (
      scolarite?.idScolarite &&
      evaluation?.idEvaluation
    ) {
      try {
        const response =
          await page.request.get(
            `${apiBase}/notes`,
            { headers },
          );

        if (response.ok()) {
          const notes =
            await response.json();

          const restante =
            notes.find(
              (item) =>
                item.idScolarite ===
                  scolarite.idScolarite &&
                item.idEvaluation ===
                  evaluation.idEvaluation,
            );

          if (restante?.idNote) {
            await deleteBestEffort(
              page.request,
              `notes/${restante.idNote}`,
              headers,
            );
          }
        }
      } catch {
        // Nettoyage best effort.
      }
    }

    /*
     * Nettoyage dans l'ordre inverse
     * des dépendances.
     */
    if (evaluation?.idEvaluation) {
      await deleteBestEffort(
        page.request,
        `evaluations/${evaluation.idEvaluation}`,
        headers,
      );
    }

    if (scolarite?.idScolarite) {
      await deleteBestEffort(
        page.request,
        `scolarites/${scolarite.idScolarite}`,
        headers,
      );
    }

    if (enseignement?.idEnseignement) {
      await deleteBestEffort(
        page.request,
        `enseignements/${enseignement.idEnseignement}`,
        headers,
      );
    }

    if (periode?.idPeriode) {
      await deleteBestEffort(
        page.request,
        `periodes/${periode.idPeriode}`,
        headers,
      );
    }

    if (matiere?.idMatiere) {
      await deleteBestEffort(
        page.request,
        `matieres/${matiere.idMatiere}`,
        headers,
      );
    }

    if (classe?.idClasse) {
      await deleteBestEffort(
        page.request,
        `classes/${classe.idClasse}`,
        headers,
      );
    }

    if (enseignant?.idPersonne) {
      await deleteBestEffort(
        page.request,
        `enseignants/${enseignant.idPersonne}`,
        headers,
      );
    }

    if (eleve?.idPersonne) {
      await deleteBestEffort(
        page.request,
        `eleves/${eleve.idPersonne}`,
        headers,
      );
    }
  }
});
