import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";
import { createResource, deleteResource, listResource, updateResource } from "../api/schoolApi";

const control = "mt-2 min-h-11 w-full border border-slate-300 bg-white px-3 text-school-ink focus:border-school-blue";
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
      const [currentAccounts, teachers, guardians] = await Promise.all([
        listResource("comptes", { signal }), listResource("enseignants", { signal }), listResource("responsables", { signal }),
      ]);
      if (signal?.aborted) return;
      setAccounts(currentAccounts);
      setPeople([...teachers.map((person) => ({ ...person, kind: "ENSEIGNANT" })), ...guardians.map((person) => ({ ...person, kind: "RESPONSABLE" }))]);
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

  const candidates = people.filter((person) => !accounts.some((account) => account.idPersonne === person.idPersonne) && (!form?.role || form.role === "ADMIN" || person.kind === form.role));
  return <div className="min-h-screen bg-school-paper px-4 py-8 sm:px-6"><main className="mx-auto max-w-5xl">
    <Link to="/espace" className="font-semibold text-school-blue underline">← Tableau de bord</Link>
    <div className="mt-6 flex flex-wrap items-center justify-between gap-4"><h1 className="font-display text-4xl font-bold uppercase text-school-blue">Comptes utilisateurs</h1><button type="button" disabled={busy || loading} onClick={() => open()} className="bg-school-blue px-5 py-3 font-bold text-white disabled:opacity-50">Ajouter</button></div>
    {error && <p role="alert" className="mt-6 border border-red-300 bg-red-50 p-4 text-red-800">{error}</p>}
    {success && <p role="status" className="mt-6 border border-green-300 bg-green-50 p-4 text-green-800">{success}</p>}
    {loading && <p role="status" className="mt-6">Chargement…</p>}
    {form && !loading && <form onSubmit={save} className="mt-6 bg-white p-6 shadow-sm" autoComplete="off">
      <h2 className="font-display text-2xl font-bold uppercase text-school-blue">{editing ? "Modifier le compte" : "Créer un compte"}</h2>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <label className="font-semibold text-school-ink">Rôle<select className={control} required disabled={busy} value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value, idPersonne: "" })}><option value="">Choisir…</option>{Object.entries(roles).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        {!editing && <label className="font-semibold text-school-ink">Personne<select className={control} required disabled={busy} value={form.idPersonne} onChange={(event) => setForm({ ...form, idPersonne: event.target.value })}><option value="">Choisir…</option>{candidates.map((person) => <option key={person.idPersonne} value={person.idPersonne}>{person.prenom} {person.nom} · {roles[person.kind]}{person.emailContact ? ` · ${person.emailContact}` : ""}</option>)}</select></label>}
        <label className="font-semibold text-school-ink">E-mail de connexion<input className={control} type="email" maxLength={255} required disabled={busy} autoComplete="off" value={form.emailConnexion} onChange={(event) => setForm({ ...form, emailConnexion: event.target.value })} /></label>
        <label className="font-semibold text-school-ink">{editing ? "Nouveau mot de passe (facultatif)" : "Mot de passe"}<input className={control} type="password" minLength={8} maxLength={100} required={!editing} disabled={busy} autoComplete="new-password" value={editing ? form.nouveauMotDePasse : form.motDePasse} onChange={(event) => setForm({ ...form, [editing ? "nouveauMotDePasse" : "motDePasse"]: event.target.value })} /></label>
        {editing && <label className="flex items-center gap-3 font-semibold text-school-ink"><input type="checkbox" checked={form.actif} disabled={busy} onChange={(event) => setForm({ ...form, actif: event.target.checked })} />Compte actif</label>}
      </div>
      {!editing && candidates.length === 0 && <p className="mt-4 text-school-muted">Aucune personne disponible pour ce rôle. Créez d’abord un enseignant ou un responsable sans compte.</p>}
      <div className="mt-5 flex flex-wrap gap-3"><button disabled={busy || (!editing && !candidates.length)} className="bg-school-blue px-5 py-3 font-bold text-white disabled:opacity-50">{busy ? "Enregistrement…" : "Enregistrer"}</button><button type="button" disabled={busy} onClick={() => setForm(null)} className="border border-slate-300 px-5 py-3 font-bold disabled:opacity-50">Annuler</button></div>
    </form>}
    {!loading && accounts.length === 0 && <p className="mt-6 bg-white p-6 text-school-muted">Aucun compte utilisateur.</p>}
    {!loading && accounts.length > 0 && <div className="mt-6 grid gap-4 sm:grid-cols-2">{accounts.map((account) => <article key={account.idUtilisateur} className="bg-white p-5 shadow-sm"><h2 className="font-semibold text-school-ink">{account.prenom} {account.nom}</h2><p className="mt-2 break-all text-school-muted">{account.emailConnexion}</p><p className="mt-2 text-school-muted">{roles[account.role]} · {account.actif ? "Actif" : "Inactif"}</p><div className="mt-4 flex gap-4 font-semibold"><button type="button" disabled={busy} onClick={() => open(account)} className="text-school-blue underline disabled:opacity-50">Modifier</button><button type="button" disabled={busy} onClick={() => remove(account)} className="text-red-700 underline disabled:opacity-50">Supprimer</button></div></article>)}</div>}
  </main></div>;
}
