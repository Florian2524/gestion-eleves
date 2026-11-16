import ConnectedHeader from "../components/layout/ConnectedHeader";
import {
  ArrowRight, BookOpen, BookOpenCheck, CalendarDays, ClipboardCheck,
  ClipboardList, FileText, GraduationCap, HeartHandshake, IdCard,
  Layers, NotebookPen, Scale, School, ShieldCheck, UserRound,
  UserRoundCog, Users,
} from "lucide-react";
import { Link } from "react-router";
import { useAuth } from "../context/authContextCore";

const roleLabels = {
  ADMIN: "Administrateur",
  ENSEIGNANT: "Enseignant",
  RESPONSABLE: "Responsable légal",
};

const welcomeMessages = {
  ADMIN: "Organisez la scolarité, suivez les résultats et retrouvez les outils d’administration de l’établissement.",
  ENSEIGNANT: "Retrouvez les dossiers scolaires, saisissez les notes et consultez les enseignements, les évaluations et les bulletins.",
  RESPONSABLE: "Consultez les dossiers scolaires, les résultats et les bulletins de vos élèves, ainsi que les enseignements et les évaluations accessibles.",
};

// Keep each module's existing role list and destination unchanged.
const modules = [
  { title: "Élèves", to: "/eleves", section: "suivi", icon: Users, roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"], description: "Consulter les dossiers et les informations scolaires.", responsibleDescription: "Consulter les dossiers et les informations scolaires de vos élèves." },
  { title: "Classes", to: "/classes", section: "suivi", icon: School, roles: ["ADMIN"], description: "Retrouver et organiser les classes de l’établissement." },
  { title: "Inscriptions", to: "/inscriptions", section: "suivi", icon: ClipboardList, roles: ["ADMIN"], description: "Gérer les inscriptions des élèves." },
  { title: "Scolarités", to: "/scolarites", section: "suivi", icon: GraduationCap, roles: ["ADMIN"], description: "Suivre les parcours et les affectations en classe." },
  { title: "Notes", to: "/notes", section: "pedagogie", icon: NotebookPen, roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"], description: "Saisir et consulter les résultats des évaluations.", responsibleDescription: "Consulter les résultats des évaluations de vos élèves." },
  { title: "Bulletins", to: "/bulletins", section: "pedagogie", icon: FileText, roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"], description: "Consulter les moyennes et télécharger les bulletins PDF.", responsibleDescription: "Consulter les moyennes de vos élèves et télécharger leurs bulletins PDF." },
  { title: "Enseignements", to: "/enseignements", section: "pedagogie", icon: BookOpenCheck, roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"], description: "Consulter les enseignements associés aux classes et aux matières." },
  { title: "Évaluations", to: "/evaluations", section: "pedagogie", icon: ClipboardCheck, roles: ["ADMIN", "ENSEIGNANT", "RESPONSABLE"], description: "Consulter les évaluations et leurs barèmes." },
  { title: "Matières", to: "/matieres", section: "pedagogie", icon: BookOpen, roles: ["ADMIN"], description: "Organiser les matières enseignées." },
  { title: "Pondérations", to: "/ponderations", section: "pedagogie", icon: Scale, roles: ["ADMIN"], description: "Gérer les coefficients utilisés pour les moyennes." },
  { title: "Périodes", to: "/periodes", section: "pedagogie", icon: CalendarDays, roles: ["ADMIN"], description: "Définir les périodes du suivi scolaire." },
  { title: "Enseignants", to: "/enseignants", section: "administration", icon: UserRound, roles: ["ADMIN"], description: "Gérer les profils et les coordonnées des enseignants." },
  { title: "Responsables", to: "/responsables", section: "administration", icon: HeartHandshake, roles: ["ADMIN"], description: "Gérer les profils et les coordonnées des responsables légaux." },
  { title: "Responsabilités légales", to: "/responsabilites-legales", section: "administration", icon: ShieldCheck, roles: ["ADMIN"], description: "Associer les responsables légaux aux élèves concernés." },
  { title: "Comptes utilisateurs", to: "/comptes-utilisateurs", section: "administration", icon: UserRoundCog, roles: ["ADMIN"], description: "Administrer les comptes utilisateurs et leurs profils." },
];

const sections = [
  { id: "suivi", title: "Suivi des élèves", icon: Users },
  { id: "pedagogie", title: "Pédagogie et évaluations", icon: Layers },
  { id: "administration", title: "Administration", icon: ShieldCheck },
];

export default function DashboardPage() {
  const { auth } = useAuth();
  const roleLabel = roleLabels[auth?.role] ?? auth?.role ?? "Utilisateur";
  const visibleModules = modules.filter((module) => module.roles.includes(auth?.role));

  return (
    <div className="min-h-screen bg-school-paper">
      <ConnectedHeader />

      <main id="main-content" tabIndex={-1} className="mx-auto max-w-[1440px] px-4 py-8 sm:px-6 lg:px-10 lg:py-12">
        <section aria-labelledby="dashboard-title" className="grid min-w-0 gap-7 border border-slate-200 border-t-4 border-t-brand-yellow bg-white p-5 sm:p-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,20rem)] lg:p-10">
          <div className="min-w-0">
            <p className="text-sm font-bold uppercase tracking-widest text-school-blue">Tableau de bord</p>
            <h1 id="dashboard-title" className="mt-3 font-display text-4xl font-bold uppercase leading-tight text-school-ink sm:text-5xl">Bienvenue dans votre espace</h1>
            <p className="mt-4 max-w-2xl leading-7 text-school-muted">{welcomeMessages[auth?.role] ?? "Retrouvez vos modules de gestion scolaire."}</p>
          </div>
          <div className="min-w-0 self-center border border-school-blue-light bg-school-blue-light/40 p-5">
            <p className="flex items-center gap-2 text-sm font-semibold text-school-blue"><IdCard size={20} aria-hidden="true" />Profil connecté</p>
            <p className="mt-3 font-display text-2xl font-semibold text-school-blue-dark">{roleLabel}</p>
            <p className="mt-2 break-all text-sm leading-6 text-school-ink">{auth?.emailConnexion}</p>
          </div>
        </section>

        <div className="mt-10 space-y-10">
          {sections.map((section) => {
            const sectionModules = visibleModules.filter((module) => module.section === section.id);
            if (sectionModules.length === 0) return null;
            const SectionIcon = section.icon;
            const compactProfile = auth?.role === "ENSEIGNANT" || auth?.role === "RESPONSABLE";
            const singleCard = compactProfile && sectionModules.length === 1;
            return (
              <section key={section.id} aria-labelledby={`section-${section.id}`}>
                <div className="mb-5 flex items-center gap-3">
                  <SectionIcon size={24} className="shrink-0 text-school-blue" aria-hidden="true" />
                  <h2 id={`section-${section.id}`} className="font-display text-2xl font-semibold text-school-blue-dark sm:text-3xl">{section.title}</h2>
                </div>
                <div className={`grid auto-rows-fr gap-4 ${singleCard ? "grid-cols-1" : compactProfile ? "sm:grid-cols-2" : "sm:grid-cols-2 xl:grid-cols-3"}`}>
                  {sectionModules.map((module) => {
                    const Icon = module.icon;
                    return (
                      <article key={module.to} className={`flex min-w-0 flex-col border border-slate-200 bg-white p-5 transition-colors hover:border-school-blue focus-within:border-school-blue sm:p-6 ${singleCard ? "lg:flex-row lg:items-center lg:gap-6" : ""}`}>
                        <span className="grid size-11 shrink-0 place-items-center bg-brand-yellow text-school-blue-dark"><Icon size={24} aria-hidden="true" /></span>
                        <h3 className={`mt-5 break-words font-display text-2xl font-semibold leading-tight text-school-blue-dark ${singleCard ? "lg:mt-0" : ""}`}>{module.title}</h3>
                        <p className={`mb-6 mt-3 text-sm leading-6 text-school-muted ${singleCard ? "lg:my-0 lg:flex-1" : ""}`}>{auth?.role === "RESPONSABLE" ? module.responsibleDescription ?? module.description : module.description}</p>
                        <Link to={module.to} aria-label={`Ouvrir le module ${module.title}`} className={`mt-auto flex min-h-11 items-center justify-between gap-3 border-t border-slate-200 px-2 py-3 text-sm font-bold text-school-blue transition-colors hover:bg-school-blue hover:text-white focus-visible:bg-school-blue focus-visible:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-school-blue-dark ${singleCard ? "lg:mt-0 lg:shrink-0 lg:border-t-0 lg:border-l lg:px-4" : ""}`}>
                          Ouvrir le module<ArrowRight size={18} className="shrink-0" aria-hidden="true" />
                        </Link>
                      </article>
                    );
                  })}
                </div>
              </section>
            );
          })}
        </div>
      </main>
    </div>
  );
}
