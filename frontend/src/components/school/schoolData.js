import { getEleves } from "../../api/elevesApi";
import { listResource } from "../../api/schoolApi";

export const schoolLookups = {
  eleves: getEleves,
  classes: (options) => listResource("classes", options),
  matieres: (options) => listResource("matieres", options),
  enseignants: (options) => listResource("enseignants", options),
  enseignements: (options) => listResource("enseignements", options),
  periodes: (options) => listResource("periodes", options),
};

export function schoolLabels(data) {
  const find = (key, id, idKey) => data[key]?.find((item) => String(item[idKey]) === String(id));
  const eleve = (id) => { const item = find("eleves", id, "idPersonne"); return item ? `${item.prenom} ${item.nom}` : "Élève indisponible"; };
  const classe = (id) => { const item = find("classes", id, "idClasse"); return item ? `${item.nom} · ${item.anneeScolaire}` : "Classe indisponible"; };
  const matiere = (id) => find("matieres", id, "idMatiere")?.nom ?? "Matière indisponible";
  const enseignant = (id) => { const item = find("enseignants", id, "idPersonne"); return item ? `${item.prenom} ${item.nom}` : "Enseignant"; };
  const enseignement = (id) => {
    const item = find("enseignements", id, "idEnseignement");
    return item ? `${matiere(item.idMatiere)} · ${classe(item.idClasse)}` : "Enseignement indisponible";
  };
  const enseignantEnseignement = (id) => {
    const item = find("enseignements", id, "idEnseignement");
    return item ? enseignant(item.idEnseignant) : "Enseignant indisponible";
  };
  const periode = (id) => find("periodes", id, "idPeriode")?.libelle ?? "Période indisponible";
  return { eleve, classe, matiere, enseignant, enseignement, enseignantEnseignement, periode };
}
