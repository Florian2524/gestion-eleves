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
    throw new Error(
      `POST /${path} : ${response.status()} ${await response.text()}`,
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

test(
  "un administrateur peut calculer un bulletin et télécharger son PDF",
  async ({ page }) => {
    const suffix =
      Date.now().toString().slice(-8);

    const eleveNom =
      `BulletinEleve${suffix}`;

    const elevePrenom = "Test";

    const matricule =
      `BUL-${suffix}`;

    const classeNom =
      `Classe Bulletin ${suffix}`;

    const enseignantNom =
      `ProfBulletin${suffix}`;

    const numeroEmploye =
      `ENS-BUL-${suffix}`;

    const matiereNom =
      `Matière Bulletin ${suffix}`;

    const matiereCode =
      `BUL-${suffix}`;

    const periodeLibelle =
      `Période Bulletin ${suffix}`;

    const evaluationLibelle =
      `Évaluation Bulletin ${suffix}`;

    let eleve;
    let classe;
    let enseignant;
    let matiere;
    let enseignement;
    let periode;
    let scolarite;
    let evaluation;
    let note;

    /*
     * Connexion ADMIN.
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

    const auth =
      await page.evaluate(() => {
        const raw =
          localStorage.getItem(
            "gestion-eleves.auth",
          );

        return raw
          ? JSON.parse(raw)
          : null;
      });

    expect(auth?.token).toBeTruthy();

    const headers = {
      Authorization:
        `Bearer ${auth.token}`,
    };

    try {
      /*
       * Création des données métier.
       */
      eleve = await postJson(
        page.request,
        "eleves",
        headers,
        {
          nom: eleveNom,
          prenom: elevePrenom,
          matricule,
          dateNaissance:
            "2012-05-14",
          emailContact:
            `bulletin-eleve-${suffix}@example.test`,
          telephone: null,
          adresse: null,
          photoUrl: null,
        },
      );

      classe = await postJson(
        page.request,
        "classes",
        headers,
        {
          nom: classeNom,
          niveau: "4e",
          anneeScolaire:
            "2026-2027",
          idProfesseurPrincipal:
            null,
        },
      );

      enseignant = await postJson(
        page.request,
        "enseignants",
        headers,
        {
          nom: enseignantNom,
          prenom: "Test",
          emailContact:
            `bulletin-prof-${suffix}@example.test`,
          telephone: null,
          adresse: null,
          numeroEmploye,
        },
      );

      matiere = await postJson(
        page.request,
        "matieres",
        headers,
        {
          code: matiereCode,
          nom: matiereNom,
        },
      );

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
          dateDebut:
            "2026-09-01",
          dateFin: null,
          actif: true,
        },
      );

      periode = await postJson(
        page.request,
        "periodes",
        headers,
        {
          libelle:
            periodeLibelle,
          dateDebut:
            "2026-09-01",
          dateFin:
            "2026-12-31",
          dateDebutSaisie:
            null,
          dateFinSaisie:
            null,
          statut:
            "OUVERTE",
        },
      );

      scolarite = await postJson(
        page.request,
        "scolarites",
        headers,
        {
          idEleve:
            eleve.idPersonne,
          idClasse:
            classe.idClasse,
          dateDebut:
            "2026-09-01",
          dateFin: null,
          statut:
            "ACTIVE",
        },
      );

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
          dateEvaluation:
            "2026-10-20",
          typeEvaluation:
            "Contrôle",
          coefficientEvaluation: 2,
          bareme: 20,
        },
      );

      note = await postJson(
        page.request,
        "notes",
        headers,
        {
          idEvaluation:
            evaluation.idEvaluation,
          idScolarite:
            scolarite.idScolarite,
          valeur: 15,
          commentaire:
            "Note du bulletin E2E",
          statutNote:
            "VALIDEE",
        },
      );

      /*
       * Ouverture du module Bulletins.
       */
      await page.goto("/bulletins");

      await expect(
        page.getByRole(
          "heading",
          {
            name:
              "Consulter un bulletin",
            exact: true,
          },
        ),
      ).toBeVisible();

      const bulletinForm =
        page.locator("form");

      const bulletinSelects =
        bulletinForm.locator("select");

      await expect(
        bulletinSelects,
      ).toHaveCount(2);

      const scolariteSelect =
        bulletinSelects.nth(0);

      const periodeSelect =
        bulletinSelects.nth(1);

      await scolariteSelect
        .selectOption(
          String(
            scolarite.idScolarite,
          ),
        );

      await periodeSelect
        .selectOption(
          String(
            periode.idPeriode,
          ),
        );

      /*
       * Calcul du bulletin depuis l'UI.
       */
      const calculationPath =
        `/bulletins/calcul/` +
        `${scolarite.idScolarite}/` +
        `${periode.idPeriode}`;

      const calculationResponse =
        page.waitForResponse(
          (response) =>
            response
              .url()
              .includes(
                calculationPath,
              ) &&
            !response
              .url()
              .endsWith("/pdf") &&
            response
              .request()
              .method() === "GET",
        );

      await page
        .getByRole(
          "button",
          {
            name: "Calculer",
            exact: true,
          },
        )
        .click();

      const response =
        await calculationResponse;

      expect(response.status())
        .toBe(200);

      const bulletin =
        await response.json();

      expect(
        bulletin.idScolarite,
      ).toBe(
        scolarite.idScolarite,
      );

      expect(
        bulletin.idPeriode,
      ).toBe(
        periode.idPeriode,
      );

      expect(
        Number(
          bulletin.moyenneGenerale,
        ),
      ).toBe(15);

      expect(
        bulletin.lignes,
      ).toHaveLength(1);

      expect(
        bulletin.lignes[0]
          .nomMatiere,
      ).toBe(matiereNom);

      expect(
        Number(
          bulletin.lignes[0]
            .moyenneSur20,
        ),
      ).toBe(15);

      /*
       * Vérification du rendu.
       */
      await expect(
        page.getByText(
          "Bulletin calculé",
          { exact: true },
        ),
      ).toBeVisible();

      await expect(
        page.getByRole(
          "heading",
          {
            name:
              `${elevePrenom} ${eleveNom}`,
            exact: true,
          },
        ),
      ).toBeVisible();

      const moyenneCard =
        page.locator("article")
          .filter({
            hasText:
              "Moyenne générale",
          });

      await expect(
        moyenneCard,
      ).toContainText(
        "15 / 20",
      );

      const ligneMatiere =
        page.locator("tbody tr")
          .filter({
            hasText:
              matiereNom,
          });

      await expect(
        ligneMatiere,
      ).toBeVisible();

      await expect(
        ligneMatiere,
      ).toContainText(
        matiereCode,
      );

      await expect(
        ligneMatiere,
      ).toContainText(
        "15 / 20",
      );

      /*
       * Vérification directe des octets PDF.
       */
      const pdfPath =
        `${calculationPath}/pdf`;

      const pdfApiResponse =
        await page.request.get(
          `${apiBase}${pdfPath}`,
          { headers },
        );

      expect(
        pdfApiResponse.status(),
      ).toBe(200);

      expect(
        pdfApiResponse.headers()[
          "content-type"
        ],
      ).toContain(
        "application/pdf",
      );

      const pdfBody =
        await pdfApiResponse.body();

      expect(
        pdfBody
          .subarray(0, 5)
          .toString(),
      ).toBe("%PDF-");

      expect(
        pdfBody.length,
      ).toBeGreaterThan(1000);

      /*
       * Vérification du téléchargement
       * déclenché depuis l'interface.
       */
      const pdfResponsePromise =
        page.waitForResponse(
          (pdfResponse) =>
            pdfResponse
              .url()
              .includes(pdfPath) &&
            pdfResponse
              .request()
              .method() === "GET",
        );

      const downloadPromise =
        page.waitForEvent(
          "download",
        );

      await page
        .getByRole(
          "button",
          {
            name:
              "Télécharger le PDF",
            exact: true,
          },
        )
        .click();

      const [
        uiPdfResponse,
        download,
      ] = await Promise.all([
        pdfResponsePromise,
        downloadPromise,
      ]);

      expect(
        uiPdfResponse.status(),
      ).toBe(200);

      expect(
        uiPdfResponse.headers()[
          "content-type"
        ],
      ).toContain(
        "application/pdf",
      );

      expect(
        download.suggestedFilename(),
      ).toMatch(
        /^bulletin-.*\.pdf$/,
      );

      await download.delete();
    } finally {
      /*
       * Nettoyage inverse des dépendances.
       */
      if (note?.idNote) {
        await deleteBestEffort(
          page.request,
          `notes/${note.idNote}`,
          headers,
        );
      }

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
  },
);
