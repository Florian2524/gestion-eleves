import Panel from "../components/ui/Panel";
import Button from "../components/ui/Button";
import StatusMessage from "../components/ui/StatusMessage";
import { lightControlClass } from "../components/ui/controlStyles";
import ConnectedHeader from "../components/layout/ConnectedHeader";
import { useCallback, useEffect, useState } from "react";
import { createLegalLink, deleteLegalLink, listLegalLinks } from "../api/adminApi";
import { getEleves } from "../api/elevesApi";
import { listResource } from "../api/schoolApi";

const control = lightControlClass;
const personName = (person) => `${person.prenom} ${person.nom}`;

export default function ResponsabilitesLegalesPage() {
  const [links, setLinks] = useState([]);
  const [eleves, setEleves] = useState([]);
  const [responsables, setResponsables] = useState([]);
  const [form, setForm] = useState({ idResponsable: "", idEleve: "" });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const reload = useCallback(async (signal) => {
    setLoading(true);
    try {
      const [currentLinks, currentEleves, currentResponsables] = await Promise.all([
        listLegalLinks({ signal }), getEleves({ signal }), listResource("responsables", { signal }),
      ]);
      if (signal?.aborted) return;
      setLinks(currentLinks); setEleves(currentEleves); setResponsables(currentResponsables);
    } catch (failure) { if (!signal?.aborted) setError(failure.message || "Chargement impossible."); }
    finally { if (!signal?.aborted) setLoading(false); }
  }, []);

  useEffect(() => { const controller = new AbortController(); Promise.resolve().then(() => reload(controller.signal)); return () => controller.abort(); }, [reload]);

  async function save(event) {
    event.preventDefault(); setBusy(true); setError(""); setSuccess("");
    try {
      await createLegalLink({ idResponsable: Number(form.idResponsable), idEleve: Number(form.idEleve) });
      setForm({ idResponsable: "", idEleve: "" }); setSuccess("Lien créé."); await reload();
    } catch (failure) { setError(failure.message || "Création impossible."); }
    finally { setBusy(false); }
  }

  async function remove(link) {
    if (!window.confirm(`Supprimer le lien entre ${link.prenomResponsable} ${link.nomResponsable} et ${link.prenomEleve} ${link.nomEleve} ?`)) return;
    setBusy(true); setError(""); setSuccess("");
    try { await deleteLegalLink(link.idResponsable, link.idEleve); setSuccess("Lien supprimé."); await reload(); }
    catch (failure) { setError(failure.message || "Suppression impossible."); }
    finally { setBusy(false); }
  }

  return (
    <div className="min-h-screen bg-school-paper">
      <ConnectedHeader />
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
        <div className="border-l-4 border-brand-yellow pl-4">
          <h1 className="font-display text-3xl font-bold uppercase text-school-blue-dark [overflow-wrap:anywhere] sm:text-4xl">Responsabilités légales</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-school-muted">Associez chaque responsable légal aux élèves dont il peut consulter les données, puis retrouvez les liens enregistrés.</p>
        </div>
        {error && <StatusMessage variant="error" className="mt-5">{error}</StatusMessage>}
        {success && <StatusMessage variant="success" className="mt-5">{success}</StatusMessage>}
        {loading && <StatusMessage className="mt-5">Chargement des responsables, des élèves et des associations…</StatusMessage>}
        {!loading && (
          <>
            <Panel as="form" onSubmit={save} aria-labelledby="legal-form-title" className="mt-6 border-t-4 border-t-school-blue p-4 sm:p-6">
              <h2 id="legal-form-title" className="font-display text-2xl font-semibold text-school-blue-dark">Créer un lien</h2>
              <p className="mt-2 text-sm leading-6 text-school-muted">Choisissez un responsable légal et un élève déjà enregistrés. Les deux sélections sont obligatoires (*).</p>
              <div className="mt-5 grid min-w-0 gap-4 sm:grid-cols-2">
                <div className="min-w-0">
                  <div className="flex gap-1 text-sm font-semibold"><label htmlFor="legal-guardian">Responsable légal</label><span aria-hidden="true">*</span></div>
                  <select id="legal-guardian" className={control} required disabled={busy} aria-describedby="legal-guardian-hint" value={form.idResponsable} onChange={(event) => setForm({ ...form, idResponsable: event.target.value })}>
                    <option value="">Choisir…</option>{responsables.map((item) => <option key={item.idPersonne} value={item.idPersonne}>{personName(item)}</option>)}
                  </select>
                  <p id="legal-guardian-hint" className="mt-2 text-sm leading-6 text-school-muted">Personne disposant d’un profil de responsable légal.</p>
                </div>
                <div className="min-w-0">
                  <div className="flex gap-1 text-sm font-semibold"><label htmlFor="legal-student">Élève</label><span aria-hidden="true">*</span></div>
                  <select id="legal-student" className={control} required disabled={busy} aria-describedby="legal-student-hint" value={form.idEleve} onChange={(event) => setForm({ ...form, idEleve: event.target.value })}>
                    <option value="">Choisir…</option>{eleves.map((item) => <option key={item.idPersonne} value={item.idPersonne}>{personName(item)}</option>)}
                  </select>
                  <p id="legal-student-hint" className="mt-2 text-sm leading-6 text-school-muted">Élève auquel ce responsable légal sera associé.</p>
                </div>
              </div>
              {(!responsables.length || !eleves.length) && <p className="mt-4 border border-slate-300 bg-school-paper p-3 text-sm leading-6 text-school-muted">Créez d’abord un responsable et un élève.</p>}
              <div className="mt-6 border-t border-slate-200 pt-5"><Button type="submit" loading={busy} disabled={busy || !responsables.length || !eleves.length} variant="school">{busy ? "Enregistrement…" : "Associer"}</Button></div>
            </Panel>
            <section aria-labelledby="legal-list-title" className="mt-8">
              <h2 id="legal-list-title" className="font-display text-2xl font-semibold text-school-blue-dark">Associations enregistrées</h2>
              {links.length === 0 ? (
                <Panel className="mt-4 p-5"><p className="font-semibold text-school-blue">Aucune responsabilité légale enregistrée.</p><p className="mt-2 text-sm leading-6 text-school-muted">Le formulaire ci-dessus permet d’associer un responsable légal à un élève.</p></Panel>
              ) : (
                <div className="mt-4 grid items-start gap-4 md:grid-cols-2">{links.map((link) => (
                  <Panel as="article" aria-label={`Lien entre ${link.prenomResponsable} ${link.nomResponsable} et ${link.prenomEleve} ${link.nomEleve}`} key={`${link.idResponsable}-${link.idEleve}`} className="min-w-0 p-4 transition-colors hover:border-school-blue focus-within:border-school-blue sm:p-5">
                    <dl className="space-y-4">
                      <div className="min-w-0 border-l-4 border-brand-yellow pl-3"><dt className="text-sm font-semibold text-school-muted">Responsable légal</dt><dd className="mt-1 font-display text-2xl font-semibold text-school-blue-dark [overflow-wrap:anywhere]">{link.prenomResponsable} {link.nomResponsable}</dd></div>
                      <div className="min-w-0 border-t border-slate-200 pt-4"><dt className="text-sm font-semibold text-school-muted">Élève associé</dt><dd className="mt-1 font-semibold text-school-ink [overflow-wrap:anywhere]">{link.prenomEleve} {link.nomEleve}</dd></div>
                    </dl>
                    <Button disabled={busy} onClick={() => remove(link)} variant="danger" className="mt-5">Supprimer le lien</Button>
                  </Panel>
                ))}</div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
