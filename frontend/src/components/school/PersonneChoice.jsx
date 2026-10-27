import { profileLabels } from "./personneProfiles";

export default function PersonneChoice({ people, value, onChange, profileType, disabled }) {
  const candidates = people.filter((person) => !person.profils.includes(profileType));
  const selected = candidates.find((person) => String(person.idPersonne) === String(value));
  return <div className="mt-4 border border-school-blue bg-school-paper p-4">
    <label className="block font-semibold">Personne existante
      <select required disabled={disabled} value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 min-h-11 w-full border border-slate-300 bg-white px-3">
        <option value="">Choisir une personne…</option>
        {candidates.map((person) => <option key={person.idPersonne} value={person.idPersonne}>{person.prenom} {person.nom} · #{person.idPersonne} · {profileLabels(person.profils)}{person.emailContact ? ` · ${person.emailContact}` : ""}</option>)}
      </select>
    </label>
    <p className="mt-2 text-sm">Le profil sera rattaché à cette identité. Ses nom, prénom et coordonnées seront conservés.</p>
    {selected && <p className="mt-2 font-semibold">Profils actuels : {profileLabels(selected.profils)}</p>}
    {!candidates.length && <p className="mt-2">Aucune personne disponible sans ce profil.</p>}
  </div>;
}
