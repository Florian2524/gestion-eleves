import Button from "../components/ui/Button";
import Panel from "../components/ui/Panel";
import StatusMessage from "../components/ui/StatusMessage";
import { lightControlClass } from "../components/ui/controlStyles";
import ConnectedHeader from "../components/layout/ConnectedHeader";
import PersonneChoice from "../components/school/PersonneChoice";
import { attachProfile, listResource } from "../api/schoolApi";
import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  RefreshCw,
  Search,
  UserRound,
  Users,
} from "lucide-react";

import { createEleve, deleteEleve, deleteElevePhoto, getElevePhoto, getEleves, updateEleve, uploadElevePhoto } from "../api/elevesApi";
import { ApiError } from "../api/http";
import { useAuth } from "../context/authContextCore";

function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function formatDate(dateValue) {
  if (!dateValue) {
    return "Non renseignée";
  }

  const date = new Date(
    `${dateValue}T00:00:00`,
  );

  if (Number.isNaN(date.getTime())) {
    return dateValue;
  }

  return new Intl.DateTimeFormat(
    "fr-FR",
    {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    },
  ).format(date);
}

function getInitials(eleve) {
  const prenomInitial =
    eleve?.prenom?.trim()?.charAt(0) ?? "";

  const nomInitial =
    eleve?.nom?.trim()?.charAt(0) ?? "";

  return (
    `${prenomInitial}${nomInitial}`
      .toUpperCase() || "É"
  );
}

function ElevePortrait({ eleve }) {
  const [url, setUrl] = useState(null);
  useEffect(() => {
    if (!eleve.photoUrl) return undefined;
    const controller = new AbortController();
    let objectUrl;
    getElevePhoto(eleve.idPersonne, controller.signal)
      .then((blob) => {
        if (!controller.signal.aborted) {
          objectUrl = URL.createObjectURL(blob);
          setUrl(objectUrl);
        }
      })
      .catch(() => {});
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [eleve.idPersonne, eleve.photoUrl]);
  return url ? <img src={url} alt={`Portrait de ${eleve.prenom} ${eleve.nom}`} className="size-12 shrink-0 object-cover" />
    : <span className="grid size-12 shrink-0 place-items-center bg-brand-yellow font-bold text-black">{getInitials(eleve)}</span>;
}

const emptyEleve = { nom: "", prenom: "", matricule: "", dateNaissance: "", emailContact: "", telephone: "", adresse: "" };

function EleveForm({ eleve, onSave, onCancel, saving }) {
  const [values, setValues] = useState(() => eleve ? { ...emptyEleve, ...eleve } : emptyEleve);
  const [photo, setPhoto] = useState(null);
  const [mode, setMode] = useState("new");
  const [people, setPeople] = useState([]);
  const [idPersonne, setIdPersonne] = useState("");
  const [peopleError, setPeopleError] = useState("");
  const attaching = !eleve && mode === "existing";
  useEffect(() => {
    if (eleve) return;
    const controller = new AbortController();
    listResource("personnes", { signal: controller.signal }).then(setPeople).catch((failure) => {
      if (!controller.signal.aborted) setPeopleError(failure.message);
    });
    return () => controller.abort();
  }, [eleve]);
  const fields = [
    ["nom", "Nom", "text", true], ["prenom", "Prénom", "text", true],
    ["matricule", "Matricule", "text", true], ["dateNaissance", "Date de naissance", "date", true],
    ["emailContact", "E-mail", "email"], ["telephone", "Téléphone", "tel"],
    ["adresse", "Adresse", "text"],
  ];
  return <Panel as="form" aria-labelledby="eleve-form-title" onSubmit={(event) => { event.preventDefault(); onSave(attaching ? { idPersonne: Number(idPersonne), matricule: values.matricule, dateNaissance: values.dateNaissance } : values, photo); }} className="mt-6 border-t-4 border-t-school-blue p-4 sm:p-6">
    <h2 id="eleve-form-title" className="font-display text-2xl font-semibold text-school-blue-dark">{eleve ? "Modifier l’élève" : "Ajouter un élève"}</h2>
    <p className="mt-2 text-sm leading-6 text-school-muted">Les champs marqués d’un astérisque (*) sont obligatoires.</p>
    {!eleve && <label className="mt-4 block text-sm font-semibold">Création du profil
      <select value={mode} onChange={(event) => setMode(event.target.value)} disabled={saving} className={lightControlClass}>
        <option value="new">Créer une nouvelle personne</option><option value="existing">Rattacher à une personne existante</option>
      </select>
    </label>}
    {attaching && <PersonneChoice people={people} value={idPersonne} onChange={setIdPersonne} profileType="ELEVE" disabled={saving} />}
    {attaching && peopleError && <StatusMessage variant="error" className="mt-4">{peopleError}</StatusMessage>}
    {eleve && <p className="mt-4 text-sm leading-6 text-school-muted">Les nom, prénom et coordonnées sont communs à tous les profils de cette personne.</p>}
    <div className="mt-5 space-y-5">
      {[
        ["Identité et coordonnées", ["nom", "prenom", "emailContact", "telephone", "adresse"]],
        ["Informations scolaires", ["matricule", "dateNaissance"]],
      ].filter(([, names]) => !attaching || names.includes("matricule")).map(([legend, names]) => (
        <fieldset key={legend} className="min-w-0 border-t border-slate-200 pt-4">
          <legend className="pr-3 text-sm font-bold text-school-blue">{legend}</legend>
          <div className="grid min-w-0 gap-4 sm:grid-cols-2">
            {fields.filter(([name]) => names.includes(name)).map(([name, label, type, required]) => (
              <div key={name} className={`min-w-0 ${name === "adresse" ? "sm:col-span-2" : ""}`}>
                <div className="flex gap-1 text-sm font-semibold text-school-ink"><label htmlFor={`eleve-${name}`}>{label}</label>{required && <span aria-hidden="true">*</span>}</div>
                <input id={`eleve-${name}`} type={type} required={required} disabled={saving} maxLength={name === "matricule" ? 50 : name === "telephone" ? 30 : name === "adresse" ? 500 : name === "emailContact" ? 255 : 100}
                  value={values[name] ?? ""} onChange={(event) => setValues({ ...values, [name]: event.target.value })} className={lightControlClass} />
              </div>
            ))}
          </div>
        </fieldset>
      ))}
      <div className="min-w-0 border-t border-slate-200 pt-4">
        <label htmlFor="eleve-photo" className="block text-sm font-semibold text-school-ink">Photo de l’élève</label>
        <input id="eleve-photo" type="file" accept="image/jpeg,image/png,image/gif,image/webp" aria-describedby="eleve-photo-hint" disabled={saving} onChange={(event) => setPhoto(event.target.files?.[0] ?? null)} className={`${lightControlClass} text-sm file:mr-3 file:border-0 file:bg-school-blue-light file:px-3 file:py-2 file:font-semibold file:text-school-blue-dark`} />
        <p id="eleve-photo-hint" className="mt-2 text-sm leading-6 text-school-muted">Facultative. Formats JPEG, PNG, GIF ou WebP, 5 Mo maximum.</p>
      </div>
    </div>
    <div className="mt-6 flex flex-wrap gap-3 border-t border-slate-200 pt-5">
      <Button loading={saving} type="submit" variant="school">{saving ? "Enregistrement..." : "Enregistrer"}</Button>
      <Button disabled={saving} onClick={onCancel} variant="secondary">Annuler</Button>
    </div>
  </Panel>;
}

export default function ElevesPage() {

  const { auth } = useAuth();

  const [eleves, setEleves] =
    useState([]);

  const [search, setSearch] =
    useState("");

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [reloadKey, setReloadKey] =
    useState(0);
  const [editing, setEditing] = useState(undefined);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");
  const isAdmin = auth?.role === "ADMIN";

  async function saveEleve(values, photo) {
    if (photo && (photo.size > 5 * 1024 * 1024 || !["image/jpeg", "image/png", "image/gif", "image/webp"].includes(photo.type))) {
      setActionError("La photo doit être une image JPEG, PNG, GIF ou WebP de 5 Mo maximum.");
      return;
    }
    setSaving(true); setActionError(""); setNotice("");
    try {
      const payload = { ...values, photoUrl: undefined };
      const saved = editing?.idPersonne ? await updateEleve(editing.idPersonne, payload)
        : values.idPersonne ? await attachProfile(values.idPersonne, "ELEVE", { matricule: values.matricule, dateNaissance: values.dateNaissance })
        : await createEleve(payload);
      if (photo) await uploadElevePhoto(saved.idPersonne, photo);
      setEditing(undefined);
      setNotice("Élève enregistré.");
      setReloadKey((key) => key + 1);
    } catch (failure) { setActionError(failure.message || "Enregistrement impossible."); }
    finally { setSaving(false); }
  }

  async function removeEleve(eleve) {
    if (!window.confirm(`Retirer le profil élève de ${eleve.prenom} ${eleve.nom} ? La personne et ses autres profils seront conservés.`)) return;
    setActionError(""); setNotice("");
    try { await deleteEleve(eleve.idPersonne); setNotice("Élève supprimé."); setReloadKey((key) => key + 1); }
    catch (failure) { setActionError(failure.message || "Suppression impossible."); }
  }

  async function removePhoto(eleve) {
    if (!window.confirm(`Supprimer la photo de ${eleve.prenom} ${eleve.nom} ?`)) return;
    try { await deleteElevePhoto(eleve.idPersonne); setNotice("Photo supprimée."); setReloadKey((key) => key + 1); }
    catch (failure) { setActionError(failure.message || "Suppression de la photo impossible."); }
  }

  function actions(eleve) {
    if (!isAdmin) return null;
    return <div className="flex flex-wrap gap-2 text-sm font-semibold">
      <Button type="button" onClick={() => { setEditing(eleve); setActionError(""); }} variant="secondary">Modifier</Button>
      {eleve.photoUrl && <Button type="button" onClick={() => removePhoto(eleve)} variant="secondary">Retirer photo</Button>}
      <Button type="button" onClick={() => removeEleve(eleve)} variant="danger">Supprimer</Button>
    </div>;
  }

  useEffect(() => {
    const controller =
      new AbortController();

    async function loadEleves() {
      setLoading(true);
      setError("");

      try {
        const response =
          await getEleves({
            signal: controller.signal,
          });

        const responseEleves =
          Array.isArray(response)
            ? response
            : Array.isArray(response?.content)
              ? response.content
              : [];

        const sortedEleves = [
          ...responseEleves,
        ].sort((firstEleve, secondEleve) => {
          const firstName =
            `${firstEleve.nom ?? ""} ${firstEleve.prenom ?? ""}`;

          const secondName =
            `${secondEleve.nom ?? ""} ${secondEleve.prenom ?? ""}`;

          return firstName.localeCompare(
            secondName,
            "fr",
            {
              sensitivity: "base",
            },
          );
        });

        setEleves(sortedEleves);
      } catch (requestError) {
        if (
          requestError?.name === "AbortError"
        ) {
          return;
        }

        if (requestError instanceof ApiError) {
          if (requestError.status === 403) {
            setError(
              "Votre rôle ne permet pas de consulter la liste des élèves.",
            );
          } else {
            setError(requestError.message);
          }
        } else {
          setError(
            "Impossible de joindre le serveur. Vérifiez que le backend est démarré.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadEleves();

    return () => {
      controller.abort();
    };
  }, [reloadKey]);

  const filteredEleves = useMemo(() => {
    const normalizedSearch =
      normalizeText(search);

    if (!normalizedSearch) {
      return eleves;
    }

    return eleves.filter((eleve) => {
      const searchableContent =
        normalizeText(
          [
            eleve.nom,
            eleve.prenom,
            eleve.matricule,
            eleve.emailContact,
            eleve.telephone,
          ].join(" "),
        );

      return searchableContent.includes(
        normalizedSearch,
      );
    });
  }, [eleves, search]);

  return (
    <div className="min-h-screen bg-school-paper">
      <ConnectedHeader />
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-[1440px] px-4 py-8 sm:px-6 lg:px-10">
        <Panel className="p-4 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-5">
            <div className="min-w-0 border-l-4 border-brand-yellow pl-4">
              <p className="text-sm font-bold uppercase tracking-widest text-school-blue">Dossiers scolaires</p>
              <h1 className="mt-1 font-display text-3xl font-bold uppercase text-school-blue-dark sm:text-4xl">Liste des élèves</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-school-muted">Consultez les dossiers accessibles à votre profil. Recherchez un élève par identité, matricule ou coordonnées.</p>
            </div>
            {isAdmin && <Button onClick={() => { setEditing(null); setActionError(""); }} variant="primary"><Users size={18} aria-hidden="true" />Ajouter un élève</Button>}
          </div>
          <div className="mt-5 border-t border-slate-200 pt-5">
            <label htmlFor="eleves-search" className="block text-sm font-semibold text-school-ink">Rechercher un élève</label>
            <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="relative w-full sm:max-w-xl">
                <Search size={20} aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-school-muted" />
                <input id="eleves-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Nom, prénom, matricule ou contact…" className={`${lightControlClass} mt-0 pl-10`} />
              </div>
              {!loading && !error && <p role="status" className="text-sm font-semibold text-school-blue">{filteredEleves.length} résultat{filteredEleves.length > 1 ? "s" : ""} sur {eleves.length} élève{eleves.length > 1 ? "s" : ""}</p>}
            </div>
          </div>
        </Panel>
        {actionError && <StatusMessage variant="error" className="mt-5">{actionError}</StatusMessage>}
        {notice && <StatusMessage variant="success" className="mt-5">{notice}</StatusMessage>}
        {isAdmin && editing !== undefined && <EleveForm key={editing?.idPersonne ?? "new"} eleve={editing} onSave={saveEleve} onCancel={() => setEditing(undefined)} saving={saving} />}
        {loading && <StatusMessage className="mt-5">Chargement des élèves...</StatusMessage>}
        {!loading && error && (
          <Panel className="mt-5 border-red-300 p-4 sm:p-6">
            <StatusMessage variant="error">Chargement impossible : {error}</StatusMessage>
            <Button variant="danger" className="mt-4" onClick={() => setReloadKey((currentKey) => currentKey + 1)}><RefreshCw size={18} aria-hidden="true" />Réessayer</Button>
          </Panel>
        )}
        {!loading && !error && filteredEleves.length === 0 && (
          <Panel className="mt-5 p-6 text-center">
            <UserRound size={32} aria-hidden="true" className="mx-auto text-school-blue" />
            <h2 className="mt-3 font-display text-2xl font-semibold text-school-blue">Aucun élève trouvé</h2>
            <p className="mt-2 text-sm leading-6 text-school-muted">{search ? "Aucun dossier ne correspond à votre recherche. Essayez un autre nom, un matricule ou des coordonnées." : "Aucun élève n’est encore enregistré."}</p>
          </Panel>
        )}
        {!loading && !error && filteredEleves.length > 0 && (
          <>
            <Panel className="mt-5 hidden min-w-0 overflow-hidden md:block">
              <div className="overflow-x-auto" role="region" aria-label="Tableau des élèves" tabIndex={0}>
                <table className="w-full min-w-[850px] border-collapse text-left text-sm">
                  <caption className="sr-only">Dossiers des élèves correspondant à la recherche</caption>
                  <thead className="bg-school-blue text-white"><tr>
                    <th scope="col" className="px-4 py-3">Élève</th>
                    <th scope="col" className="px-4 py-3">Matricule</th>
                    <th scope="col" className="px-4 py-3">Coordonnées</th>
                    <th scope="col" className="px-4 py-3">Naissance</th>
                    {isAdmin && <th scope="col" className="px-4 py-3">Actions</th>}
                  </tr></thead>
                  <tbody>
                    {filteredEleves.map((eleve) => (
                      <tr key={eleve.idPersonne} className="border-b border-slate-200 align-top transition-colors last:border-0 hover:bg-school-blue-light/25 focus-within:bg-school-blue-light/25">
                        <td className="px-4 py-4"><div className="flex items-center gap-3"><ElevePortrait eleve={eleve} /><p className="max-w-56 break-words font-semibold text-school-blue-dark">{eleve.prenom} {eleve.nom}</p></div></td>
                        <td className="max-w-44 break-words px-4 py-4 font-semibold text-school-blue">{eleve.matricule ?? "Non renseigné"}</td>
                        <td className="max-w-64 px-4 py-4 [overflow-wrap:anywhere]"><p>{eleve.emailContact ?? "E-mail non renseigné"}</p><p className="mt-1 text-school-muted">{eleve.telephone ?? "Téléphone non renseigné"}</p>{eleve.adresse && <p className="mt-1 text-school-muted">{eleve.adresse}</p>}</td>
                        <td className="px-4 py-4 text-school-muted">{formatDate(eleve.dateNaissance)}</td>
                        {isAdmin && <td className="px-4 py-4">{actions(eleve)}</td>}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Panel>
            <section aria-label="Cartes des élèves" className="mt-5 grid gap-4 md:hidden">
              {filteredEleves.map((eleve) => (
                <Panel as="article" key={eleve.idPersonne} className="min-w-0 p-4">
                  <div className="flex items-start gap-3">
                    <ElevePortrait eleve={eleve} />
                    <div className="min-w-0"><h2 className="break-words font-display text-2xl font-semibold text-school-blue-dark">{eleve.prenom} {eleve.nom}</h2><p className="mt-1 break-words text-sm font-semibold text-school-muted">Matricule : {eleve.matricule ?? "Non renseigné"}</p></div>
                  </div>
                  <dl className="mt-4 space-y-3 border-t border-slate-200 pt-4 text-sm leading-6">
                    <div className="min-w-0"><dt className="font-semibold text-school-muted">E-mail</dt><dd className="[overflow-wrap:anywhere]">{eleve.emailContact ?? "Non renseigné"}</dd></div>
                    <div><dt className="font-semibold text-school-muted">Téléphone</dt><dd className="[overflow-wrap:anywhere]">{eleve.telephone ?? "Non renseigné"}</dd></div>
                    {eleve.adresse && <div><dt className="font-semibold text-school-muted">Adresse</dt><dd className="[overflow-wrap:anywhere]">{eleve.adresse}</dd></div>}
                    <div><dt className="font-semibold text-school-muted">Date de naissance</dt><dd>{formatDate(eleve.dateNaissance)}</dd></div>
                  </dl>
                  {isAdmin && <div className="mt-4 border-t border-slate-200 pt-4">{actions(eleve)}</div>}
                </Panel>
              ))}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
