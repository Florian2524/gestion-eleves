const labels = { ELEVE: "Élève", ENSEIGNANT: "Enseignant", RESPONSABLE: "Responsable légal" };

export function profileLabels(profils = []) {
  return profils.length ? profils.map((profil) => labels[profil] ?? profil).join(" · ") : "Sans profil";
}
