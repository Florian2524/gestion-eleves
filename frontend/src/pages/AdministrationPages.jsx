import { ManagementDetails, ManagementFields } from "../components/school/ManagementPresentation";
import { EvaluationDetails, EvaluationFields } from "../components/school/EvaluationPresentation";
import ResourcePage from "../components/school/ResourcePage";
import { useAuth } from "../context/authContextCore";
import { schoolYearLabel, validateSchoolYear } from "../components/school/schoolYear";

const personOption = (item) => `${item.prenom} ${item.nom}`;
const classOption = (item) => `${item.nom} · ${item.anneeScolaire}`;
const teachingOption = (item, labels) => `${labels.matiere(item.idMatiere)} · ${labels.classe(item.idClasse)} · ${labels.enseignant(item.idEnseignant)}`;
const select = (name, label, lookup, optionId, optionLabel, optional = false) => ({ name, label, type: "select", lookup, optionId, optionLabel, optional });
const text = (name, label, maxLength = 100, optional = false) => ({ name, label, maxLength, optional });
const date = (name, label, optional = false) => ({ name, label, type: "date", optional });
const number = (name, label, defaultValue = "1") => ({ name, label, type: "number", min: "0.01", step: "0.01", defaultValue });
const col = (label, render) => ({ label, render });
const datesValid = (start, end) => !end || end >= start ? "" : "La date de fin doit suivre la date de début.";
const formatDate = (value) => value ? new Intl.DateTimeFormat("fr-FR").format(new Date(`${value}T00:00:00`)) : "—";

const managementPresentation = { formFields: ManagementFields, rowDetails: ManagementDetails };

const matieres = {
  ...managementPresentation, formLabel: "Matière", primaryColumn: "Nom",
  fieldGroups: [["Matière", ["code", "nom"]]],
  description: "Consultez les codes et les noms des matières enseignées.",
  title: "Matières", resource: "matieres", idKey: "idMatiere",
  fields: [text("code", "Code", 30), text("nom", "Nom")],
  columns: [col("Code", (r) => r.code), col("Nom", (r) => r.nom)],
};
const classes = {
  ...managementPresentation, formLabel: "Classe", primaryColumn: "Nom",
  fieldGroups: [["Identification de la classe", ["nom", "niveau", "anneeScolaire"]], ["Référent pédagogique", ["idProfesseurPrincipal"]]],
  description: "Retrouvez les classes, leur niveau, leur année scolaire et leur professeur principal.",
  title: "Classes", resource: "classes", idKey: "idClasse", lookups: ["enseignants"],
  fields: [text("nom", "Nom"), text("niveau", "Niveau", 50), text("anneeScolaire", "Année scolaire", 20), select("idProfesseurPrincipal", "Professeur principal", "enseignants", "idPersonne", personOption, true)],
  columns: [col("Nom", (r) => r.nom), col("Niveau", (r) => r.niveau), col("Année scolaire", (r) => r.anneeScolaire), col("Professeur principal", (r, l) => r.idProfesseurPrincipal ? l.enseignant(r.idProfesseurPrincipal) : "Aucun")],
};
const periodes = {
  ...managementPresentation, formLabel: "Période", primaryColumn: "Période",
  fieldGroups: [["Identification de la période", ["libelle", "anneeScolaire", "statut"]], ["Dates de la période", ["dateDebut", "dateFin"]], ["Fenêtre de saisie des notes", ["dateDebutSaisie", "dateFinSaisie"]]],
  description: "Consultez les dates des périodes et leurs fenêtres de saisie. L’année scolaire reste facultative.",
  title: "Périodes", resource: "periodes", idKey: "idPeriode",
  fields: [text("libelle", "Libellé", 100), text("anneeScolaire", "Année scolaire", 9, true), date("dateDebut", "Début"), date("dateFin", "Fin"), date("dateDebutSaisie", "Début de saisie", true), date("dateFinSaisie", "Fin de saisie", true), text("statut", "Statut", 30)],
  columns: [col("Période", (r) => r.libelle), col("Année scolaire", (r) => schoolYearLabel(r.anneeScolaire)), col("Dates", (r) => `${formatDate(r.dateDebut)} au ${formatDate(r.dateFin)}`), col("Saisie", (r) => r.dateDebutSaisie ? `${formatDate(r.dateDebutSaisie)} au ${formatDate(r.dateFinSaisie)}` : "Non définie"), col("Statut", (r) => r.statut)],
  validate: (f) => validateSchoolYear(f.anneeScolaire) || datesValid(f.dateDebut, f.dateFin) || (f.dateDebutSaisie && f.dateFinSaisie ? datesValid(f.dateDebutSaisie, f.dateFinSaisie) : ""),
};
const ponderations = {
  ...managementPresentation, formLabel: "Pondération", primaryColumn: "Classe",
  fieldGroups: [["Association classe et matière", ["idClasse", "idMatiere"]], ["Pondération commune", ["coefficientMatiere"]]],
  title: "Pondérations", resource: "ponderations", getId: (r) => `${r.idClasse}/${r.idMatiere}`, lookups: ["classes", "matieres"],
  description: "Un coefficient commun à tous les enseignements d’une matière dans une classe. Sa modification s’applique aux bulletins calculés à nouveau.",
  fields: [{ ...select("idClasse", "Classe", "classes", "idClasse", classOption), immutable: true }, { ...select("idMatiere", "Matière", "matieres", "idMatiere", (i) => i.nom), immutable: true }, number("coefficientMatiere", "Coefficient")],
  columns: [col("Classe", (r, l) => l.classe(r.idClasse)), col("Matière", (r, l) => l.matiere(r.idMatiere)), col("Coefficient", (r) => r.coefficientMatiere)],
};
const enseignementsAdmin = {
  ...managementPresentation, formLabel: "Enseignement", primaryColumn: "Matière",
  fieldGroups: [["Association pédagogique", ["idEnseignant", "idClasse", "idMatiere"]], ["Pondération", ["coefficientMatiere"]], ["Dates et activation", ["dateDebut", "dateFin", "actif"]]],
  description: "Le coefficient doit correspondre à la pondération classe–matière existante. Modifiez le coefficient commun dans Pondérations ; un nouveau couple reprend le coefficient saisi ici.",
  title: "Enseignements", resource: "enseignements", idKey: "idEnseignement", lookups: ["enseignants", "classes", "matieres"],
  fields: [select("idEnseignant", "Enseignant", "enseignants", "idPersonne", personOption), select("idClasse", "Classe", "classes", "idClasse", classOption), select("idMatiere", "Matière", "matieres", "idMatiere", (i) => i.nom), number("coefficientMatiere", "Coefficient"), date("dateDebut", "Début"), date("dateFin", "Fin", true), { name: "actif", label: "Actif", type: "checkbox", defaultValue: true }],
  columns: [col("Matière", (r, l) => l.matiere(r.idMatiere)), col("Classe", (r, l) => l.classe(r.idClasse)), col("Enseignant", (r, l) => l.enseignant(r.idEnseignant)), col("Période", (r) => `${formatDate(r.dateDebut)} au ${r.dateFin ? formatDate(r.dateFin) : "présent"}`), col("Statut", (r) => r.actif ? "Actif" : "Inactif")],
  validate: (f) => datesValid(f.dateDebut, f.dateFin),
};
const enseignementsRead = {
  ...enseignementsAdmin,
  description: "Consultez les matières, les classes et les dates des enseignements accessibles à votre profil.",
  lookups: ["classes", "matieres"],
  columns: enseignementsAdmin.columns.filter((c) => c.label !== "Enseignant"),
};
const evaluationsAdmin = {
  formFields: EvaluationFields, rowDetails: EvaluationDetails, formLabel: "Évaluation",
  description: "Retrouvez le contexte, la période et les paramètres de notation de chaque évaluation.",
  title: "Évaluations", resource: "evaluations", idKey: "idEvaluation", lookups: ["enseignements", "classes", "matieres", "periodes", "enseignants"],
  fields: [select("idEnseignement", "Enseignement", "enseignements", "idEnseignement", teachingOption), select("idPeriode", "Période", "periodes", "idPeriode", (i) => `${i.libelle} · ${schoolYearLabel(i.anneeScolaire)}`), text("libelle", "Libellé", 150), date("dateEvaluation", "Date"), text("typeEvaluation", "Type", 50), number("coefficientEvaluation", "Coefficient"), number("bareme", "Barème", "20")],
  columns: [col("Évaluation", (r) => r.libelle), col("Type", (r) => r.typeEvaluation), col("Coefficient", (r) => r.coefficientEvaluation), col("Matière et classe", (r, l) => l.enseignement(r.idEnseignement)), col("Enseignant", (r, l) => l.enseignantEnseignement(r.idEnseignement)), col("Période", (r, l) => l.periode(r.idPeriode)), col("Date", (r) => formatDate(r.dateEvaluation)), col("Barème", (r) => r.bareme)],
};
const evaluationsRead = {
  ...evaluationsAdmin, lookups: ["enseignements", "classes", "matieres", "periodes"],
  columns: evaluationsAdmin.columns.filter((c) => c.label !== "Enseignant"),
};
const inscriptions = {
  ...managementPresentation, formLabel: "Inscription", primaryColumn: "Élève",
  fieldGroups: [["Élève concerné", ["idEleve"]], ["Dates et statut de l’inscription", ["dateInscription", "dateFin", "statut"]]],
  description: "Retrouvez les inscriptions des élèves, leurs dates et leur statut.",
  title: "Inscriptions", resource: "inscriptions", idKey: "idInscription", lookups: ["eleves"],
  fields: [select("idEleve", "Élève", "eleves", "idPersonne", personOption), date("dateInscription", "Date d'inscription"), date("dateFin", "Fin", true), text("statut", "Statut", 30)],
  columns: [col("Élève", (r, l) => l.eleve(r.idEleve)), col("Date", (r) => formatDate(r.dateInscription)), col("Fin", (r) => formatDate(r.dateFin)), col("Statut", (r) => r.statut)],
  validate: (f) => datesValid(f.dateInscription, f.dateFin),
};
const scolarites = {
  ...managementPresentation, formLabel: "Scolarité", primaryColumn: "Élève",
  fieldGroups: [["Élève et classe", ["idEleve", "idClasse"]], ["Dates et statut de l’affectation", ["dateDebut", "dateFin", "statut"]]],
  description: "Reliez chaque parcours à un élève, une classe et une année scolaire. Les dates retracent son affectation.",
  title: "Scolarités et affectations", resource: "scolarites", idKey: "idScolarite", lookups: ["eleves", "classes"],
  fields: [select("idEleve", "Élève", "eleves", "idPersonne", personOption), select("idClasse", "Classe et année", "classes", "idClasse", classOption), date("dateDebut", "Début"), date("dateFin", "Fin", true), text("statut", "Statut", 30)],
  columns: [col("Élève", (r, l) => l.eleve(r.idEleve)), col("Classe", (r, l) => l.classe(r.idClasse)), col("Dates", (r) => `${formatDate(r.dateDebut)} au ${r.dateFin ? formatDate(r.dateFin) : "présent"}`), col("Statut", (r) => r.statut)],
  validate: (f) => datesValid(f.dateDebut, f.dateFin),
};
const enseignants = {
  ...managementPresentation, formLabel: "Profil enseignant", primaryColumn: "Nom",
  fieldGroups: [["Identité de la personne", ["nom", "prenom"]], ["Coordonnées partagées", ["emailContact", "telephone", "adresse"]], ["Profil enseignant", ["numeroEmploye"]]],
  description: "Consultez les profils enseignants. L’identité et les coordonnées appartiennent à la personne et sont partagées entre ses profils.",
  title: "Enseignants", profileType: "ENSEIGNANT", resource: "enseignants", idKey: "idPersonne",
  fields: [text("nom", "Nom"), text("prenom", "Prénom"), { ...text("emailContact", "E-mail de contact", 255, true), type: "email" }, { ...text("telephone", "Téléphone", 30, true), type: "tel" }, text("adresse", "Adresse", 500, true), text("numeroEmploye", "Numéro d'employé", 50)],
  columns: [col("Nom", (r) => `${r.prenom} ${r.nom}`), col("E-mail", (r) => r.emailContact || "—"), col("Téléphone", (r) => r.telephone || "—"), col("N° employé", (r) => r.numeroEmploye)],
};
const responsables = {
  ...managementPresentation, formLabel: "Profil responsable", primaryColumn: "Nom",
  fieldGroups: [["Identité de la personne", ["nom", "prenom"]], ["Coordonnées partagées", ["emailContact", "telephone", "adresse"]], ["Profil responsable légal", ["profession"]]],
  description: "Consultez les profils des responsables légaux. L’identité et les coordonnées appartiennent à la personne et sont partagées entre ses profils.",
  title: "Responsables légaux", profileType: "RESPONSABLE", resource: "responsables", idKey: "idPersonne",
  fields: [text("nom", "Nom"), text("prenom", "Prénom"), { ...text("emailContact", "E-mail de contact", 255, true), type: "email" }, { ...text("telephone", "Téléphone", 30, true), type: "tel" }, text("adresse", "Adresse", 500, true), text("profession", "Profession", 150, true)],
  columns: [col("Nom", (r) => `${r.prenom} ${r.nom}`), col("E-mail", (r) => r.emailContact || "—"), col("Téléphone", (r) => r.telephone || "—"), col("Profession", (r) => r.profession || "—")],
};

export const PonderationsPage = () => <ResourcePage {...ponderations} />;
export const MatieresPage = () => <ResourcePage {...matieres} />;
export const ClassesPage = () => <ResourcePage {...classes} />;
export const PeriodesPage = () => <ResourcePage {...periodes} />;
export const InscriptionsPage = () => <ResourcePage {...inscriptions} />;
export const ScolaritesPage = () => <ResourcePage {...scolarites} />;
export const EnseignantsPage = () => <ResourcePage {...enseignants} />;
export const ResponsablesPage = () => <ResourcePage {...responsables} />;
export function EnseignementsPage() { const { auth } = useAuth(); return <ResourcePage {...(auth?.role === "ADMIN" ? enseignementsAdmin : enseignementsRead)} />; }
export function EvaluationsPage() { const { auth } = useAuth(); return <ResourcePage {...(auth?.role === "ADMIN" ? evaluationsAdmin : evaluationsRead)} canWrite={(role) => role !== "RESPONSABLE"} />; }
