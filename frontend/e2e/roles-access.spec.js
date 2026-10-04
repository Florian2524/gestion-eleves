import { expect, test } from "@playwright/test";

const adminEmail =
  process.env.E2E_ADMIN_EMAIL;

const adminPassword =
  process.env.E2E_ADMIN_PASSWORD;

const apiBase =
  "http://127.0.0.1:8080";

const rolePassword =
  "RoleTest2026!";

test.beforeAll(() => {
  if (!adminEmail || !adminPassword) {
    throw new Error(
      "E2E_ADMIN_EMAIL et E2E_ADMIN_PASSWORD doivent être définis.",
    );
  }
});

async function login(
  page,
  email,
  password,
) {
  await page.goto("/connexion");

  await page
    .getByLabel(
      "Adresse électronique",
    )
    .fill(email);

  await page
    .locator("#motDePasse")
    .fill(password);

  await page
    .getByRole(
      "button",
      {
        name: "Se connecter",
      },
    )
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

  return auth;
}

async function logout(page) {
  await page.goto("/espace");

  await page
    .getByRole(
      "button",
      {
        name: "Déconnexion",
      },
    )
    .click();

  await expect(page)
    .toHaveURL(/\/connexion$/);
}

async function postJson(
  request,
  path,
  headers,
  data,
) {
  const response =
    await request.post(
      `${apiBase}/${path}`,
      {
        headers,
        data,
      },
    );

  if (!response.ok()) {
    throw new Error(
      `POST /${path} : ` +
      `${response.status()} ` +
      `${await response.text()}`,
    );
  }

  return response.json();
}

async function getJson(
  request,
  path,
  headers,
) {
  const response =
    await request.get(
      `${apiBase}/${path}`,
      {
        headers,
      },
    );

  if (!response.ok()) {
    throw new Error(
      `GET /${path} : ` +
      `${response.status()} ` +
      `${await response.text()}`,
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
      {
        headers,
      },
    );
  } catch {
    // Nettoyage best effort.
  }
}

function ids(items, key) {
  return items.map(
    (item) => item[key],
  );
}

test(
  "les rôles enseignant et responsable respectent leur périmètre métier",
  async ({ page }) => {
    test.setTimeout(60_000);

    const suffix =
      Date.now()
        .toString()
        .slice(-8);

    const teacherLogin =
      `enseignant-role-${suffix}@example.test`;

    const guardianLogin =
      `responsable-role-${suffix}@example.test`;

    const ownStudentMatricule =
      `ROLE-A-${suffix}`;

    const foreignStudentMatricule =
      `ROLE-B-${suffix}`;

    const ownNoteComment =
      `Note visible ${suffix}`;

    const foreignNoteComment =
      `Note masquée ${suffix}`;

    const teacherCreatedComment =
      `Note enseignant ${suffix}`;

    let adminHeaders;

    let teacher;
    let foreignTeacher;
    let guardian;

    let ownClass;
    let foreignClass;

    let subject;
    let period;

    let ownStudent;
    let foreignStudent;

    let ownTeaching;
    let foreignTeaching;

    let ownScolarite;
    let foreignScolarite;

    let ownEvaluation;
    let foreignEvaluation;

    let ownNote;
    let foreignNote;

    let teacherCreatedEvaluation;
    let teacherCreatedNote;

    let teacherAccount;
    let guardianAccount;

    let legalResponsibilityCreated =
      false;

    try {
      /*
       * ADMIN :
       * création de tout le jeu de données.
       */
      const adminAuth =
        await login(
          page,
          adminEmail,
          adminPassword,
        );

      expect(adminAuth.role)
        .toBe("ADMIN");

      adminHeaders = {
        Authorization:
          `Bearer ${adminAuth.token}`,
      };

      teacher =
        await postJson(
          page.request,
          "enseignants",
          adminHeaders,
          {
            nom:
              `ProfRole${suffix}`,
            prenom: "Alice",
            emailContact:
              `prof-role-${suffix}@example.test`,
            telephone: null,
            adresse: null,
            numeroEmploye:
              `ENS-ROLE-${suffix}`,
          },
        );

      foreignTeacher =
        await postJson(
          page.request,
          "enseignants",
          adminHeaders,
          {
            nom:
              `ProfExterne${suffix}`,
            prenom: "Bob",
            emailContact:
              `prof-externe-${suffix}@example.test`,
            telephone: null,
            adresse: null,
            numeroEmploye:
              `ENS-EXT-${suffix}`,
          },
        );

      guardian =
        await postJson(
          page.request,
          "responsables",
          adminHeaders,
          {
            nom:
              `ResponsableRole${suffix}`,
            prenom: "Claire",
            emailContact:
              `responsable-contact-${suffix}@example.test`,
            telephone: null,
            adresse: null,
            profession: "Test E2E",
          },
        );

      ownClass =
        await postJson(
          page.request,
          "classes",
          adminHeaders,
          {
            nom:
              `Classe Role A ${suffix}`,
            niveau: "4e",
            anneeScolaire:
              "2026-2027",
            idProfesseurPrincipal:
              null,
          },
        );

      foreignClass =
        await postJson(
          page.request,
          "classes",
          adminHeaders,
          {
            nom:
              `Classe Role B ${suffix}`,
            niveau: "3e",
            anneeScolaire:
              "2026-2027",
            idProfesseurPrincipal:
              null,
          },
        );

      subject =
        await postJson(
          page.request,
          "matieres",
          adminHeaders,
          {
            code:
              `ROL-${suffix}`,
            nom:
              `Matière Rôles ${suffix}`,
          },
        );

      period =
        await postJson(
          page.request,
          "periodes",
          adminHeaders,
          {
            libelle:
              `Période Rôles ${suffix}`,
            dateDebut:
              "2026-09-01",
            dateFin:
              "2026-12-31",
            dateDebutSaisie:
              null,
            dateFinSaisie:
              null,
            statut: "OUVERTE",
          },
        );

      ownStudent =
        await postJson(
          page.request,
          "eleves",
          adminHeaders,
          {
            nom:
              `EleveVisible${suffix}`,
            prenom: "Emma",
            matricule:
              ownStudentMatricule,
            dateNaissance:
              "2012-04-12",
            emailContact:
              `eleve-visible-${suffix}@example.test`,
            telephone: null,
            adresse: null,
            photoUrl: null,
          },
        );

      foreignStudent =
        await postJson(
          page.request,
          "eleves",
          adminHeaders,
          {
            nom:
              `EleveMasque${suffix}`,
            prenom: "Louis",
            matricule:
              foreignStudentMatricule,
            dateNaissance:
              "2011-06-08",
            emailContact:
              `eleve-masque-${suffix}@example.test`,
            telephone: null,
            adresse: null,
            photoUrl: null,
          },
        );

      ownTeaching =
        await postJson(
          page.request,
          "enseignements",
          adminHeaders,
          {
            idEnseignant:
              teacher.idPersonne,
            idClasse:
              ownClass.idClasse,
            idMatiere:
              subject.idMatiere,
            coefficientMatiere: 2,
            dateDebut:
              "2026-09-01",
            dateFin: null,
            actif: true,
          },
        );

      foreignTeaching =
        await postJson(
          page.request,
          "enseignements",
          adminHeaders,
          {
            idEnseignant:
              foreignTeacher.idPersonne,
            idClasse:
              foreignClass.idClasse,
            idMatiere:
              subject.idMatiere,
            coefficientMatiere: 2,
            dateDebut:
              "2026-09-01",
            dateFin: null,
            actif: true,
          },
        );

      ownScolarite =
        await postJson(
          page.request,
          "scolarites",
          adminHeaders,
          {
            idEleve:
              ownStudent.idPersonne,
            idClasse:
              ownClass.idClasse,
            dateDebut:
              "2026-09-01",
            dateFin: null,
            statut: "ACTIVE",
          },
        );

      foreignScolarite =
        await postJson(
          page.request,
          "scolarites",
          adminHeaders,
          {
            idEleve:
              foreignStudent.idPersonne,
            idClasse:
              foreignClass.idClasse,
            dateDebut:
              "2026-09-01",
            dateFin: null,
            statut: "ACTIVE",
          },
        );

      ownEvaluation =
        await postJson(
          page.request,
          "evaluations",
          adminHeaders,
          {
            idEnseignement:
              ownTeaching.idEnseignement,
            idPeriode:
              period.idPeriode,
            libelle:
              `Évaluation visible ${suffix}`,
            dateEvaluation:
              "2026-10-10",
            typeEvaluation:
              "Contrôle",
            coefficientEvaluation: 2,
            bareme: 20,
          },
        );

      foreignEvaluation =
        await postJson(
          page.request,
          "evaluations",
          adminHeaders,
          {
            idEnseignement:
              foreignTeaching.idEnseignement,
            idPeriode:
              period.idPeriode,
            libelle:
              `Évaluation masquée ${suffix}`,
            dateEvaluation:
              "2026-10-11",
            typeEvaluation:
              "Contrôle",
            coefficientEvaluation: 2,
            bareme: 20,
          },
        );

      ownNote =
        await postJson(
          page.request,
          "notes",
          adminHeaders,
          {
            idEvaluation:
              ownEvaluation.idEvaluation,
            idScolarite:
              ownScolarite.idScolarite,
            valeur: 14,
            commentaire:
              ownNoteComment,
            statutNote: "VALIDEE",
          },
        );

      foreignNote =
        await postJson(
          page.request,
          "notes",
          adminHeaders,
          {
            idEvaluation:
              foreignEvaluation.idEvaluation,
            idScolarite:
              foreignScolarite.idScolarite,
            valeur: 18,
            commentaire:
              foreignNoteComment,
            statutNote: "VALIDEE",
          },
        );

      await postJson(
        page.request,
        "responsabilites-legales",
        adminHeaders,
        {
          idResponsable:
            guardian.idPersonne,
          idEleve:
            ownStudent.idPersonne,
        },
      );

      legalResponsibilityCreated =
        true;

      teacherAccount =
        await postJson(
          page.request,
          "comptes-utilisateurs",
          adminHeaders,
          {
            idPersonne:
              teacher.idPersonne,
            emailConnexion:
              teacherLogin,
            motDePasse:
              rolePassword,
            role: "ENSEIGNANT",
          },
        );

      guardianAccount =
        await postJson(
          page.request,
          "comptes-utilisateurs",
          adminHeaders,
          {
            idPersonne:
              guardian.idPersonne,
            emailConnexion:
              guardianLogin,
            motDePasse:
              rolePassword,
            role: "RESPONSABLE",
          },
        );

      await logout(page);

      /*
       * ENSEIGNANT :
       * authentification réelle et périmètre.
       */
      const teacherAuth =
        await login(
          page,
          teacherLogin,
          rolePassword,
        );

      expect(teacherAuth.role)
        .toBe("ENSEIGNANT");

      expect(teacherAuth.idPersonne)
        .toBe(teacher.idPersonne);

      const teacherHeaders = {
        Authorization:
          `Bearer ${teacherAuth.token}`,
      };

      await expect(
        page.getByText(
          "Enseignant",
          { exact: true },
        ).first(),
      ).toBeVisible();

      await expect(
        page.getByRole(
          "heading",
          {
            name: "Élèves",
            exact: true,
          },
        ),
      ).toBeVisible();

      await expect(
        page.getByRole(
          "heading",
          {
            name: "Notes",
            exact: true,
          },
        ),
      ).toBeVisible();

      await expect(
        page.getByRole(
          "heading",
          {
            name: "Évaluations",
            exact: true,
          },
        ),
      ).toBeVisible();

      await expect(
        page.getByRole(
          "heading",
          {
            name:
              "Comptes utilisateurs",
            exact: true,
          },
        ),
      ).toHaveCount(0);

      /*
       * Les listes API doivent être filtrées
       * au périmètre de l'enseignant.
       */
      const teacherStudents =
        await getJson(
          page.request,
          "eleves",
          teacherHeaders,
        );

      expect(
        ids(
          teacherStudents,
          "idPersonne",
        ),
      ).toContain(
        ownStudent.idPersonne,
      );

      expect(
        ids(
          teacherStudents,
          "idPersonne",
        ),
      ).not.toContain(
        foreignStudent.idPersonne,
      );

      const teacherTeachings =
        await getJson(
          page.request,
          "enseignements",
          teacherHeaders,
        );

      expect(
        ids(
          teacherTeachings,
          "idEnseignement",
        ),
      ).toContain(
        ownTeaching.idEnseignement,
      );

      expect(
        ids(
          teacherTeachings,
          "idEnseignement",
        ),
      ).not.toContain(
        foreignTeaching.idEnseignement,
      );

      const teacherEvaluations =
        await getJson(
          page.request,
          "evaluations",
          teacherHeaders,
        );

      expect(
        ids(
          teacherEvaluations,
          "idEvaluation",
        ),
      ).toContain(
        ownEvaluation.idEvaluation,
      );

      expect(
        ids(
          teacherEvaluations,
          "idEvaluation",
        ),
      ).not.toContain(
        foreignEvaluation.idEvaluation,
      );

      const teacherNotes =
        await getJson(
          page.request,
          "notes",
          teacherHeaders,
        );

      expect(
        ids(
          teacherNotes,
          "idNote",
        ),
      ).toContain(
        ownNote.idNote,
      );

      expect(
        ids(
          teacherNotes,
          "idNote",
        ),
      ).not.toContain(
        foreignNote.idNote,
      );

      /*
       * Une route réservée ADMIN
       * doit répondre 403.
       */
      const teacherAdminRequest =
        await page.request.get(
          `${apiBase}/comptes-utilisateurs`,
          {
            headers:
              teacherHeaders,
          },
        );

      expect(
        teacherAdminRequest.status(),
      ).toBe(403);

      /*
       * L'enseignant peut créer une
       * évaluation sur SON enseignement.
       */
      teacherCreatedEvaluation =
        await postJson(
          page.request,
          "evaluations",
          teacherHeaders,
          {
            idEnseignement:
              ownTeaching.idEnseignement,
            idPeriode:
              period.idPeriode,
            libelle:
              `Évaluation enseignant ${suffix}`,
            dateEvaluation:
              "2026-11-15",
            typeEvaluation:
              "Devoir",
            coefficientEvaluation: 1,
            bareme: 20,
          },
        );

      /*
       * Mais pas sur l'enseignement
       * d'un autre professeur.
       */
      const foreignCreation =
        await page.request.post(
          `${apiBase}/evaluations`,
          {
            headers:
              teacherHeaders,
            data: {
              idEnseignement:
                foreignTeaching.idEnseignement,
              idPeriode:
                period.idPeriode,
              libelle:
                `Interdite ${suffix}`,
              dateEvaluation:
                "2026-11-16",
              typeEvaluation:
                "Devoir",
              coefficientEvaluation: 1,
              bareme: 20,
            },
          },
        );

      expect(
        foreignCreation.status(),
      ).toBe(403);

      /*
       * Et il peut saisir une note
       * pour son évaluation.
       */
      teacherCreatedNote =
        await postJson(
          page.request,
          "notes",
          teacherHeaders,
          {
            idEvaluation:
              teacherCreatedEvaluation
                .idEvaluation,
            idScolarite:
              ownScolarite
                .idScolarite,
            valeur: 16,
            commentaire:
              teacherCreatedComment,
            statutNote: "SAISIE",
          },
        );

      /*
       * Vérification UI enseignant.
       */
      await page.goto("/eleves");

      await expect(
        page.getByRole(
          "heading",
          {
            name:
              "Liste des élèves",
            exact: true,
          },
        ),
      ).toBeVisible();

      await expect(
        page.getByText(
          ownStudentMatricule,
          { exact: true },
        ).first(),
      ).toBeVisible();

      await expect(
        page.getByText(
          foreignStudentMatricule,
          { exact: true },
        ),
      ).toHaveCount(0);

      await expect(
        page.getByRole(
          "button",
          {
            name:
              "Ajouter un élève",
            exact: true,
          },
        ),
      ).toHaveCount(0);

      await page.goto("/notes");

      await expect(
        page.getByRole(
          "heading",
          {
            name: "Notes",
            exact: true,
          },
        ),
      ).toBeVisible();

      await expect(
        page.getByRole(
          "heading",
          {
            name:
              "Saisir une note",
            exact: true,
          },
        ),
      ).toBeVisible();

      await expect(
        page.getByText(
          teacherCreatedComment,
          { exact: true },
        ),
      ).toBeVisible();

      await expect(
        page.getByText(
          foreignNoteComment,
          { exact: true },
        ),
      ).toHaveCount(0);

      /*
       * Même authentifié, une route
       * frontend ADMIN doit être bloquée.
       */
      await page.goto(
        "/comptes-utilisateurs",
      );

      await expect(page)
        .toHaveURL(
          /\/$/,
        );

      await logout(page);

      /*
       * RESPONSABLE :
       * uniquement les données de l'élève lié.
       */
      const guardianAuth =
        await login(
          page,
          guardianLogin,
          rolePassword,
        );

      expect(guardianAuth.role)
        .toBe("RESPONSABLE");

      expect(guardianAuth.idPersonne)
        .toBe(guardian.idPersonne);

      const guardianHeaders = {
        Authorization:
          `Bearer ${guardianAuth.token}`,
      };

      await expect(
        page.getByText(
          "Responsable légal",
          { exact: true },
        ).first(),
      ).toBeVisible();

      await expect(
        page.getByText(
          "Consultez les informations, les notes et les bulletins de vos élèves.",
          { exact: true },
        ),
      ).toBeVisible();

      await expect(
        page.getByRole(
          "heading",
          {
            name:
              "Comptes utilisateurs",
            exact: true,
          },
        ),
      ).toHaveCount(0);

      const guardianStudents =
        await getJson(
          page.request,
          "eleves",
          guardianHeaders,
        );

      expect(
        ids(
          guardianStudents,
          "idPersonne",
        ),
      ).toContain(
        ownStudent.idPersonne,
      );

      expect(
        ids(
          guardianStudents,
          "idPersonne",
        ),
      ).not.toContain(
        foreignStudent.idPersonne,
      );

      const guardianNotes =
        await getJson(
          page.request,
          "notes",
          guardianHeaders,
        );

      expect(
        ids(
          guardianNotes,
          "idNote",
        ),
      ).toContain(
        ownNote.idNote,
      );

      expect(
        ids(
          guardianNotes,
          "idNote",
        ),
      ).toContain(
        teacherCreatedNote.idNote,
      );

      expect(
        ids(
          guardianNotes,
          "idNote",
        ),
      ).not.toContain(
        foreignNote.idNote,
      );

      /*
       * Le responsable ne peut jamais
       * saisir une note.
       */
      const guardianCreateNote =
        await page.request.post(
          `${apiBase}/notes`,
          {
            headers:
              guardianHeaders,
            data: {
              idEvaluation:
                ownEvaluation.idEvaluation,
              idScolarite:
                ownScolarite.idScolarite,
              valeur: 12,
              commentaire:
                "Interdit",
              statutNote:
                "SAISIE",
            },
          },
        );

      expect(
        guardianCreateNote.status(),
      ).toBe(403);

      const guardianAdminRequest =
        await page.request.get(
          `${apiBase}/comptes-utilisateurs`,
          {
            headers:
              guardianHeaders,
          },
        );

      expect(
        guardianAdminRequest.status(),
      ).toBe(403);

      /*
       * Bulletin de son élève autorisé.
       */
      const ownBulletin =
        await page.request.get(
          `${apiBase}/bulletins/calcul/` +
          `${ownScolarite.idScolarite}/` +
          `${period.idPeriode}`,
          {
            headers:
              guardianHeaders,
          },
        );

      expect(
        ownBulletin.status(),
      ).toBe(200);

      /*
       * Bulletin d'un autre élève interdit.
       */
      const foreignBulletin =
        await page.request.get(
          `${apiBase}/bulletins/calcul/` +
          `${foreignScolarite.idScolarite}/` +
          `${period.idPeriode}`,
          {
            headers:
              guardianHeaders,
          },
        );

      expect(
        foreignBulletin.status(),
      ).toBe(403);

      /*
       * Son PDF est également accessible.
       */
      const guardianPdf =
        await page.request.get(
          `${apiBase}/bulletins/calcul/` +
          `${ownScolarite.idScolarite}/` +
          `${period.idPeriode}/pdf`,
          {
            headers:
              guardianHeaders,
          },
        );

      expect(
        guardianPdf.status(),
      ).toBe(200);

      expect(
        guardianPdf.headers()[
          "content-type"
        ],
      ).toContain(
        "application/pdf",
      );

      const guardianPdfBody =
        await guardianPdf.body();

      expect(
        guardianPdfBody
          .subarray(0, 5)
          .toString(),
      ).toBe("%PDF-");

      /*
       * Vérification UI responsable :
       * consultation oui, écriture non.
       */
      await page.goto("/eleves");

      await expect(
        page.getByText(
          ownStudentMatricule,
          { exact: true },
        ).first(),
      ).toBeVisible();

      await expect(
        page.getByText(
          foreignStudentMatricule,
          { exact: true },
        ),
      ).toHaveCount(0);

      await expect(
        page.getByRole(
          "button",
          {
            name:
              "Ajouter un élève",
            exact: true,
          },
        ),
      ).toHaveCount(0);

      await page.goto("/notes");

      await expect(
        page.getByRole(
          "heading",
          {
            name: "Notes",
            exact: true,
          },
        ),
      ).toBeVisible();

      await expect(
        page.getByText(
          ownNoteComment,
          { exact: true },
        ),
      ).toBeVisible();

      await expect(
        page.getByText(
          teacherCreatedComment,
          { exact: true },
        ),
      ).toBeVisible();

      await expect(
        page.getByText(
          foreignNoteComment,
          { exact: true },
        ),
      ).toHaveCount(0);

      await expect(
        page.getByRole(
          "heading",
          {
            name:
              "Saisir une note",
            exact: true,
          },
        ),
      ).toHaveCount(0);

      const visibleNote =
        page.locator("article")
          .filter({
            hasText:
              ownNoteComment,
          });

      await expect(
        visibleNote
          .getByRole(
            "button",
            {
              name: "Modifier",
            },
          ),
      ).toHaveCount(0);

      await expect(
        visibleNote
          .getByRole(
            "button",
            {
              name: "Supprimer",
            },
          ),
      ).toHaveCount(0);
    } finally {
      /*
       * Nettoyage avec le token ADMIN,
       * dans l'ordre inverse des dépendances.
       */
      if (adminHeaders) {
        if (
          teacherAccount
            ?.idUtilisateur
        ) {
          await deleteBestEffort(
            page.request,
            `comptes-utilisateurs/` +
            `${teacherAccount.idUtilisateur}`,
            adminHeaders,
          );
        }

        if (
          guardianAccount
            ?.idUtilisateur
        ) {
          await deleteBestEffort(
            page.request,
            `comptes-utilisateurs/` +
            `${guardianAccount.idUtilisateur}`,
            adminHeaders,
          );
        }

        if (
          legalResponsibilityCreated &&
          guardian?.idPersonne &&
          ownStudent?.idPersonne
        ) {
          await deleteBestEffort(
            page.request,
            `responsabilites-legales/` +
            `${guardian.idPersonne}/` +
            `${ownStudent.idPersonne}`,
            adminHeaders,
          );
        }

        for (const note of [
          teacherCreatedNote,
          ownNote,
          foreignNote,
        ]) {
          if (note?.idNote) {
            await deleteBestEffort(
              page.request,
              `notes/${note.idNote}`,
              adminHeaders,
            );
          }
        }

        for (const evaluation of [
          teacherCreatedEvaluation,
          ownEvaluation,
          foreignEvaluation,
        ]) {
          if (
            evaluation
              ?.idEvaluation
          ) {
            await deleteBestEffort(
              page.request,
              `evaluations/` +
              `${evaluation.idEvaluation}`,
              adminHeaders,
            );
          }
        }

        for (const scolarite of [
          ownScolarite,
          foreignScolarite,
        ]) {
          if (
            scolarite
              ?.idScolarite
          ) {
            await deleteBestEffort(
              page.request,
              `scolarites/` +
              `${scolarite.idScolarite}`,
              adminHeaders,
            );
          }
        }

        for (const teaching of [
          ownTeaching,
          foreignTeaching,
        ]) {
          if (
            teaching
              ?.idEnseignement
          ) {
            await deleteBestEffort(
              page.request,
              `enseignements/` +
              `${teaching.idEnseignement}`,
              adminHeaders,
            );
          }
        }

        if (period?.idPeriode) {
          await deleteBestEffort(
            page.request,
            `periodes/` +
            `${period.idPeriode}`,
            adminHeaders,
          );
        }

        if (subject?.idMatiere) {
          await deleteBestEffort(
            page.request,
            `matieres/` +
            `${subject.idMatiere}`,
            adminHeaders,
          );
        }

        for (const classe of [
          ownClass,
          foreignClass,
        ]) {
          if (classe?.idClasse) {
            await deleteBestEffort(
              page.request,
              `classes/` +
              `${classe.idClasse}`,
              adminHeaders,
            );
          }
        }

        if (guardian?.idPersonne) {
          await deleteBestEffort(
            page.request,
            `responsables/` +
            `${guardian.idPersonne}`,
            adminHeaders,
          );
        }

        for (const enseignant of [
          teacher,
          foreignTeacher,
        ]) {
          if (
            enseignant?.idPersonne
          ) {
            await deleteBestEffort(
              page.request,
              `enseignants/` +
              `${enseignant.idPersonne}`,
              adminHeaders,
            );
          }
        }

        for (const student of [
          ownStudent,
          foreignStudent,
        ]) {
          if (
            student?.idPersonne
          ) {
            await deleteBestEffort(
              page.request,
              `eleves/` +
              `${student.idPersonne}`,
              adminHeaders,
            );
          }
        }
      }
    }
  },
);
