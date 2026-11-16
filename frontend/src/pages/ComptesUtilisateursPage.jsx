import Panel from "../components/ui/Panel";
import Button from "../components/ui/Button";
import StatusMessage from "../components/ui/StatusMessage";
import { lightControlClass } from "../components/ui/controlStyles";
import ConnectedHeader from "../components/layout/ConnectedHeader";
import { useCallback, useEffect, useState } from "react";
import { createResource, deleteResource, listResource, updateResource } from "../api/schoolApi";

import { profileLabels } from "../components/school/personneProfiles";

const control = lightControlClass;
const roles = { ADMIN: "Administrateur", ENSEIGNANT: "Enseignant", RESPONSABLE: "Responsable légal" };
const emptyForm = { idPersonne: "", emailConnexion: "", motDePasse: "", nouveauMotDePasse: "", role: "", actif: true };

export default function ComptesUtilisateursPage() {
  const [accounts, setAccounts] = useState([]);
  const [people, setPeople] = useState([]);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const reload = useCallback(async (signal) => {
    setLoading(true);
    try {
      const [currentAccounts, currentPeople] = await Promise.all([
        listResource("comptes", { signal }), listResource("personnes", { signal }),
      ]);
      if (signal?.aborted) return;
      setAccounts(currentAccounts);
      setPeople([...new Map(currentPeople.map((person) => [person.idPersonne, person])).values()]);
    } catch (failure) { if (!signal?.aborted) setError(failure.message || "Chargement impossible."); }
    finally { if (!signal?.aborted) setLoading(false); }
  }, []);

  useEffect(() => { const controller = new AbortController(); Promise.resolve().then(() => reload(controller.signal)); return () => controller.abort(); }, [reload]);

  function open(account = null) {
    setEditing(account);
    setForm(account ? { ...emptyForm, emailConnexion: account.emailConnexion, role: account.role, actif: account.actif } : { ...emptyForm });
    setError(""); setSuccess("");
  }

  async function save(event) {
    event.preventDefault(); setBusy(true); setError(""); setSuccess("");
    try {
      if (editing) {
        await updateResource("comptes", editing.idUtilisateur, {
          emailConnexion: form.emailConnexion, role: form.role, actif: form.actif,
          nouveauMotDePasse: form.nouveauMotDePasse || null,
        });
      } else {
        await createResource("comptes", {
          idPersonne: Number(form.idPersonne), emailConnexion: form.emailConnexion,
          motDePasse: form.motDePasse, role: form.role,
        });
      }
      setForm(null); setEditing(null); setSuccess(editing ? "Compte modifié." : "Compte créé."); await reload();
    } catch (failure) { setError(failure.message || "Enregistrement impossible."); }
    finally { setBusy(false); }
  }

  async function remove(account) {
    if (!window.confirm(`Supprimer le compte de ${account.prenom} ${account.nom} ?`)) return;
    setBusy(true); setError(""); setSuccess("");
    try { await deleteResource("comptes", account.idUtilisateur); setSuccess("Compte supprimé."); await reload(); }
    catch (failure) { setError(failure.message || "Suppression impossible."); }
    finally { setBusy(false); }
  }

  const candidates = people.filter((person) => !accounts.some((account) => account.idPersonne === person.idPersonne) && (!form?.role || form.role === "ADMIN" || person.profils.includes(form.role)));
  return (
    <div className="min-h-screen bg-school-paper">
      <ConnectedHeader />
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 w-full border-l-4 border-brand-yellow pl-4 sm:w-auto sm:flex-1">
            <h1 className="font-display text-3xl font-bold uppercase text-school-blue-dark [overflow-wrap:anywhere] sm:text-4xl">Comptes utilisateurs</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-school-muted">Gérez les accès au portail et le statut des comptes rattachés aux personnes de l’établissement.</p>
          </div>
          <Button disabled={busy || loading} onClick={() => open()} variant="primary">Ajouter</Button>
        </div>
        {error && <StatusMessage variant="error" className="mt-5">{error}</StatusMessage>}
        {success && <StatusMessage variant="success" className="mt-5">{success}</StatusMessage>}
        {loading && <StatusMessage className="mt-5">Chargement des comptes et des personnes…</StatusMessage>}
        {form && !loading && (
          <Panel as="form" onSubmit={save} aria-labelledby="account-form-title" className="mt-6 border-t-4 border-t-school-blue p-4 sm:p-6" autoComplete="off">
            <h2 id="account-form-title" className="font-display text-2xl font-semibold text-school-blue-dark">{editing ? "Modifier le compte" : "Créer un compte"}</h2>
            <p className="mt-2 text-sm leading-6 text-school-muted">Les champs marqués d’un astérisque (*) sont obligatoires.</p>
            {editing && <p className="mt-3 break-words bg-school-paper p-3 text-sm leading-6">Compte de <strong>{editing.prenom} {editing.nom}</strong>. La personne rattachée reste inchangée.</p>}
            <fieldset className="mt-5 min-w-0 border-t border-slate-200 pt-4">
              <legend className="pr-3 text-sm font-bold text-school-blue">Rôle et rattachement</legend>
              <div className="grid min-w-0 gap-4 sm:grid-cols-2">
                <div className="min-w-0">
                  <div className="flex gap-1 text-sm font-semibold"><label htmlFor="account-role">Rôle</label><span aria-hidden="true">*</span></div>
                  <select id="account-role" className={control} required disabled={busy} aria-describedby="account-role-hint" value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value, idPersonne: "" })}>
                    <option value="">Choisir…</option>{Object.entries(roles).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                  <p id="account-role-hint" className="mt-2 text-sm leading-6 text-school-muted">{editing ? "Le rôle détermine les fonctionnalités accessibles au compte." : "Choisissez le rôle pour identifier les personnes compatibles."}</p>
                </div>
                {!editing && (
                  <div className="min-w-0">
                    <div className="flex gap-1 text-sm font-semibold"><label htmlFor="account-person">Personne</label><span aria-hidden="true">*</span></div>
                    <select id="account-person" className={control} required disabled={busy} aria-describedby="account-person-hint" value={form.idPersonne} onChange={(event) => setForm({ ...form, idPersonne: event.target.value })}>
                      <option value="">Choisir…</option>{candidates.map((person) => <option key={person.idPersonne} value={person.idPersonne}>{person.prenom} {person.nom} · {profileLabels(person.profils)}{person.emailContact ? ` · ${person.emailContact}` : ""}</option>)}
                    </select>
                    <p id="account-person-hint" className="mt-2 text-sm leading-6 text-school-muted">Seules les personnes sans compte sont proposées. Les rôles Enseignant et Responsable légal nécessitent le profil correspondant ; Administrateur accepte aussi une personne sans profil.</p>
                  </div>
                )}
              </div>
              {!editing && candidates.length === 0 && <p className="mt-4 border border-slate-300 bg-school-paper p-3 text-sm leading-6 text-school-muted">Aucune personne disponible pour ce rôle. Choisissez une personne sans compte disposant du profil requis pour ce rôle.</p>}
            </fieldset>
            <fieldset className="mt-5 min-w-0 border-t border-slate-200 pt-4">
              <legend className="pr-3 text-sm font-bold text-school-blue">Identifiants de connexion</legend>
              <div className="grid min-w-0 gap-4 sm:grid-cols-2">
                <div className="min-w-0">
                  <div className="flex gap-1 text-sm font-semibold"><label htmlFor="account-email">E-mail de connexion</label><span aria-hidden="true">*</span></div>
                  <input id="account-email" className={control} type="email" maxLength={255} required disabled={busy} autoComplete="off" aria-describedby="account-email-hint" value={form.emailConnexion} onChange={(event) => setForm({ ...form, emailConnexion: event.target.value })} />
                  <p id="account-email-hint" className="mt-2 text-sm leading-6 text-school-muted">Adresse utilisée pour se connecter au portail.</p>
                </div>
                <div className="min-w-0">
                  <div className="flex gap-1 text-sm font-semibold"><label htmlFor="account-password">{editing ? "Nouveau mot de passe (facultatif)" : "Mot de passe"}</label>{!editing && <span aria-hidden="true">*</span>}</div>
                  <input id="account-password" className={control} type="password" minLength={8} maxLength={100} required={!editing} disabled={busy} autoComplete="new-password" aria-describedby="account-password-hint" value={editing ? form.nouveauMotDePasse : form.motDePasse} onChange={(event) => setForm({ ...form, [editing ? "nouveauMotDePasse" : "motDePasse"]: event.target.value })} />
                  <p id="account-password-hint" className="mt-2 text-sm leading-6 text-school-muted">{editing ? "Laissez ce champ vide pour conserver le mot de passe actuel. Un nouveau mot de passe doit contenir entre 8 et 100 caractères." : "Obligatoire à la création : entre 8 et 100 caractères."}</p>
                </div>
              </div>
            </fieldset>
            {editing && (
              <fieldset className="mt-5 min-w-0 border-t border-slate-200 pt-4">
                <legend className="pr-3 text-sm font-bold text-school-blue">Activation du compte</legend>
                <label className="flex min-h-11 items-center gap-3 text-sm font-semibold" htmlFor="account-active"><input id="account-active" className="size-5 shrink-0 accent-school-blue" type="checkbox" checked={form.actif} disabled={busy} onChange={(event) => setForm({ ...form, actif: event.target.checked })} />Compte actif</label>
              </fieldset>
            )}
            <div className="mt-6 flex flex-wrap gap-3 border-t border-slate-200 pt-5">
              <Button type="submit" loading={busy} disabled={busy || (!editing && !candidates.length)} variant="school">{busy ? "Enregistrement…" : "Enregistrer"}</Button>
              <Button disabled={busy} onClick={() => setForm(null)} variant="secondary">Annuler</Button>
            </div>
          </Panel>
        )}
        {!loading && (
          <section aria-labelledby="accounts-list-title" className="mt-8">
            <h2 id="accounts-list-title" className="font-display text-2xl font-semibold text-school-blue-dark">Comptes enregistrés</h2>
            {accounts.length === 0 && <Panel className="mt-4 p-5"><p className="font-semibold text-school-blue">Aucun compte utilisateur.</p><p className="mt-2 text-sm leading-6 text-school-muted">Utilisez Ajouter pour rattacher un compte à une personne éligible.</p></Panel>}
            {accounts.length > 0 && <div className="mt-4 grid items-start gap-4 md:grid-cols-2">{accounts.map((account) => (
              <Panel as="article" key={account.idUtilisateur} className="min-w-0 p-4 transition-colors hover:border-school-blue focus-within:border-school-blue sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <h3 className="min-w-0 flex-1 font-display text-2xl font-semibold text-school-blue-dark [overflow-wrap:anywhere]">{account.prenom} {account.nom}</h3>
                  <p className={`border px-3 py-1 text-sm font-bold ${account.actif ? "border-school-blue-light bg-school-blue-light text-school-blue-dark" : "border-slate-300 bg-school-paper text-school-ink"}`}>{account.actif ? "Actif" : "Inactif"}</p>
                </div>
                <dl className="mt-4 space-y-3 border-t border-slate-200 pt-4 text-sm leading-6">
                  <div><dt className="font-semibold text-school-muted">E-mail de connexion</dt><dd className="[overflow-wrap:anywhere]">{account.emailConnexion}</dd></div>
                  <div><dt className="font-semibold text-school-muted">Rôle</dt><dd>{roles[account.role]}</dd></div>
                </dl>
                <div className="mt-5 flex flex-wrap gap-3"><Button disabled={busy} onClick={() => open(account)} variant="secondary">Modifier</Button><Button disabled={busy} onClick={() => remove(account)} variant="danger">Supprimer</Button></div>
              </Panel>
            ))}</div>}
          </section>
        )}
      </main>
    </div>
  );
}
