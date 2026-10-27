import { expect, test } from "@playwright/test";

const api = process.env.E2E_API_URL;
const email = process.env.E2E_ADMIN_EMAIL;
const password = process.env.E2E_ADMIN_PASSWORD;

test("rattachement explicite, identité unique, compte préservé et suppression indépendante", async ({ page, request }) => {
  const login = await request.post(`${api}/auth/login`, { data: { emailConnexion: email, motDePasse: password } });
  expect(login.ok()).toBeTruthy();
  const admin = await login.json(); const headers = { Authorization: `Bearer ${admin.token}` };
  const suffix = Date.now();
  const teacherResponse = await request.post(`${api}/enseignants`, { headers, data: {
    nom: `Profils${suffix}`, prenom: "Alex", emailContact: `profils-${suffix}@example.test`,
    numeroEmploye: `PROFILS-${suffix}`, telephone: "0102030405", adresse: "Adresse conservée",
  } });
  expect(teacherResponse.status()).toBe(201);
  const teacher = await teacherResponse.json();
  const accountResponse = await request.post(`${api}/comptes-utilisateurs`, { headers, data: {
    idPersonne: teacher.idPersonne, emailConnexion: `compte-profils-${suffix}@example.test`, motDePasse: "Password123", role: "ENSEIGNANT",
  } });
  expect(accountResponse.status()).toBe(201);
  const account = await accountResponse.json();
  await page.goto("/connexion");
  await page.getByLabel("Adresse électronique").fill(email);
  await page.locator("#motDePasse").fill(password);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(page).toHaveURL(/\/espace$/);
  await page.goto("/responsables");
  await page.getByRole("button", { name: "Ajouter" }).click();
  await page.getByLabel("Création du profil").selectOption("existing");
  await page.getByRole("combobox", { name: "Personne existante", exact: true }).selectOption(String(teacher.idPersonne));
  await expect(page.getByText("Profils actuels : Enseignant")).toBeVisible();
  await expect(page.getByLabel("Nom", { exact: true })).toHaveCount(0);
  await page.getByLabel("Profession", { exact: true }).fill("Architecte");
  await page.getByRole("button", { name: "Enregistrer" }).click();
  await expect(page.getByText("Création enregistrée.")).toBeVisible();
  const card = page.locator("article").filter({ hasText: `Profils${suffix}` });
  await expect(card).toContainText(`Personne #${teacher.idPersonne} · Enseignant · Responsable légal`);
  const peopleResponse = await request.get(`${api}/personnes`, { headers });
  const matchingPeople = (await peopleResponse.json()).filter((p) => p.idPersonne === teacher.idPersonne);
  expect(matchingPeople).toHaveLength(1);
  expect(matchingPeople[0].profils).toEqual(["ENSEIGNANT", "RESPONSABLE"]);
  const duplicate = await request.post(`${api}/personnes/${teacher.idPersonne}/responsable`, { headers, data: { profession: "Autre" } });
  expect(duplicate.status()).toBe(409);
  const protectedTeacher = await request.delete(`${api}/enseignants/${teacher.idPersonne}`, { headers });
  expect(protectedTeacher.status()).toBe(409);
  page.once("dialog", (dialog) => dialog.accept());
  await card.getByRole("button", { name: "Supprimer", exact: true }).click();
  await expect(page.getByText("Suppression effectuée.")).toBeVisible();
  await expect(card).toHaveCount(0);
  const remaining = await request.get(`${api}/enseignants/${teacher.idPersonne}`, { headers });
  expect((await remaining.json()).adresse).toBe("Adresse conservée");
  const preservedAccount = await request.get(`${api}/comptes-utilisateurs/${account.idUtilisateur}`, { headers });
  expect((await preservedAccount.json()).role).toBe("ENSEIGNANT");
  const teacherLogin = await request.post(`${api}/auth/login`, { data: { emailConnexion: account.emailConnexion, motDePasse: "Password123" } });
  expect(teacherLogin.status()).toBe(200);
});

for (const type of ["ELEVE", "ENSEIGNANT"]) {
  test(`rattachement ${type} depuis une personne responsable existante`, async ({ page, request }) => {
    const login = await request.post(`${api}/auth/login`, { data: { emailConnexion: email, motDePasse: password } });
    expect(login.status()).toBe(200);
    const headers = { Authorization: `Bearer ${(await login.json()).token}` };
    const suffix = `${type}-${Date.now()}`;
    const response = await request.post(`${api}/responsables`, { headers, data: {
      nom: `Rattachement-${suffix}`, prenom: "Camille", profession: "Profession conservée", emailContact: `r-${suffix}@example.test`,
    } });
    expect(response.status()).toBe(201);
    const person = await response.json();
    await page.goto("/connexion");
    await page.getByLabel("Adresse électronique").fill(email);
    await page.locator("#motDePasse").fill(password);
    await page.getByRole("button", { name: "Se connecter" }).click();
    await expect(page).toHaveURL(/\/espace$/);
    await page.goto(type === "ELEVE" ? "/eleves" : "/enseignants");
    await page.getByRole("button", { name: /Ajouter/ }).click();
    await page.getByRole("combobox", { name: "Création du profil", exact: true }).selectOption("existing");
    await page.getByRole("combobox", { name: "Personne existante", exact: true }).selectOption(String(person.idPersonne));
    if (type === "ELEVE") {
      await page.getByLabel("Matricule").fill(suffix);
      await page.getByLabel("Date de naissance").fill("2012-01-01");
    } else await page.getByLabel("Numéro d'employé", { exact: true }).fill(suffix);
    await page.getByRole("button", { name: "Enregistrer", exact: true }).click();
    await expect(page.getByText(type === "ELEVE" ? "Élève enregistré." : "Création enregistrée.", { exact: true })).toBeVisible();
    const resource = type === "ELEVE" ? "eleves" : "enseignants";
    const created = await request.get(`${api}/${resource}/${person.idPersonne}`, { headers });
    expect(created.status()).toBe(200);
    const profil = await created.json();
    expect(profil.idPersonne).toBe(person.idPersonne);
    expect(profil.nom).toBe(person.nom);
    const preserved = await request.get(`${api}/responsables/${person.idPersonne}`, { headers });
    expect((await preserved.json()).profession).toBe("Profession conservée");
  });
}
