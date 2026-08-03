import {
  ArrowRight,
  BookOpenCheck,
  FileDown,
  NotebookPen,
  ShieldCheck,
  Users,
} from "lucide-react";

import AppHeader from "../components/layout/AppHeader";

const services = [
  {
    number: "01",
    id: "eleves",
    title: "Élèves",
    description:
      "Consultez les dossiers, les coordonnées et les parcours scolaires.",
    action: "Voir les élèves",
    icon: Users,
  },
  {
    number: "02",
    id: "notes",
    title: "Notes",
    description:
      "Saisissez les résultats et accompagnez la progression de chaque élève.",
    action: "Gérer les notes",
    icon: NotebookPen,
  },
  {
    number: "03",
    id: "bulletins",
    title: "Bulletins",
    description:
      "Calculez les moyennes et générez des bulletins téléchargeables.",
    action: "Voir les bulletins",
    icon: FileDown,
  },
];

const statistics = [
  {
    value: "293",
    label: "Tests backend validés",
  },
  {
    value: "3",
    label: "Rôles sécurisés",
  },
  {
    value: "PDF",
    label: "Bulletins exportables",
  },
];

const students = [
  {
    matricule: "ELV-2026-001",
    name: "Camille Martin",
    className: "6e A",
  },
  {
    matricule: "ELV-2026-002",
    name: "Lucas Bernard",
    className: "5e B",
  },
  {
    matricule: "ELV-2026-003",
    name: "Sarah Dubois",
    className: "4e A",
  },
];

export default function HomePage() {
  return (
    <div className="min-h-screen bg-school-paper">
      <AppHeader />

      <main>
        <section
          id="accueil"
          className="relative isolate flex min-h-[650px] items-end overflow-hidden bg-school-blue-dark"
        >
          <img
            src="/images/hero-campus.jpg"
            alt="Groupe d'étudiants sur un campus"
            className="absolute inset-0 -z-20 size-full object-cover object-center"
          />

          <div className="absolute inset-0 -z-10 bg-gradient-to-r from-school-blue-dark via-school-blue-dark/80 to-black/20" />

          <div className="mx-auto w-full max-w-[1440px] px-4 pb-16 pt-28 text-white sm:px-6 sm:pb-20 lg:px-10 lg:pb-24">
            <p className="font-display text-lg font-semibold uppercase tracking-[0.2em] text-brand-yellow">
              La réussite au cœur du suivi
            </p>

            <h1 className="mt-5 max-w-5xl font-display text-[clamp(3.6rem,8vw,7.8rem)] font-bold uppercase leading-[0.9] tracking-tight">
              Un suivi scolaire
              <span className="block">
                clair et humain
              </span>
            </h1>

            <p className="mt-7 max-w-2xl text-base leading-8 text-white/85 sm:text-lg">
              Centralisez les dossiers des élèves,
              simplifiez la saisie des notes et rendez
              les bulletins accessibles depuis un
              espace unique et sécurisé.
            </p>

            <div className="mt-9 flex flex-wrap gap-3">
              <a
                href="#eleves"
                className="inline-flex min-h-13 items-center gap-3 bg-brand-yellow px-7 py-3 font-display text-lg font-semibold uppercase text-black transition hover:bg-white"
              >
                Consulter les élèves
                <ArrowRight size={21} />
              </a>

              <a
                href="#notes"
                className="inline-flex min-h-13 items-center gap-3 border-2 border-white bg-black/20 px-7 py-3 font-display text-lg font-semibold uppercase text-white transition hover:border-brand-yellow hover:text-brand-yellow"
              >
                Saisir une note
              </a>
            </div>
          </div>
        </section>

        <section className="bg-white">
          <div className="mx-auto max-w-[1440px] px-4 py-16 sm:px-6 lg:px-10 lg:py-24">
            <div className="grid gap-8 lg:grid-cols-[0.72fr_1.28fr] lg:gap-16">
              <div>
                <p className="font-display text-lg font-semibold uppercase tracking-[0.18em] text-school-blue">
                  Votre espace scolaire
                </p>

                <h2 className="mt-4 font-display text-5xl font-bold uppercase leading-none text-school-ink sm:text-6xl">
                  Toutes les fonctions essentielles
                </h2>

                <p className="mt-6 max-w-xl leading-7 text-school-muted">
                  Une interface pensée pour accéder
                  rapidement aux informations utiles,
                  sans complexifier le travail quotidien.
                </p>
              </div>

              <div className="grid gap-px overflow-hidden border border-slate-200 bg-slate-200 md:grid-cols-3">
                {services.map((service) => {
                  const Icon = service.icon;

                  return (
                    <article
                      key={service.number}
                      id={service.id}
                      className="group flex min-h-96 flex-col bg-white p-7 transition hover:bg-school-blue"
                    >
                      <div className="flex items-start justify-between gap-5">
                        <span className="font-display text-xl font-semibold text-school-muted group-hover:text-white/60">
                          {service.number}
                        </span>

                        <span className="grid size-13 place-items-center bg-brand-yellow text-black">
                          <Icon size={25} />
                        </span>
                      </div>

                      <h3 className="mt-14 font-display text-4xl font-bold uppercase text-school-blue group-hover:text-white">
                        {service.title}
                      </h3>

                      <p className="mt-5 leading-7 text-school-muted group-hover:text-white/75">
                        {service.description}
                      </p>

                      <a
                        href={`#${service.id}`}
                        className="mt-auto inline-flex items-center gap-2 pt-8 font-display text-lg font-semibold uppercase text-school-blue group-hover:text-brand-yellow"
                      >
                        {service.action}
                        <ArrowRight size={19} />
                      </a>
                    </article>
                  );
                })}
              </div>
            </div>
          </div>
        </section>

        <section className="bg-school-blue text-white">
          <div className="mx-auto grid max-w-[1440px] md:grid-cols-3">
            {statistics.map((statistic) => (
              <article
                key={statistic.label}
                className="border-b border-white/20 px-8 py-12 md:border-b-0 md:border-r md:last:border-r-0 lg:px-12 lg:py-16"
              >
                <p className="font-display text-6xl font-bold text-brand-yellow sm:text-7xl">
                  {statistic.value}
                </p>

                <p className="mt-3 font-display text-lg font-semibold uppercase tracking-wide">
                  {statistic.label}
                </p>
              </article>
            ))}
          </div>
        </section>

        <section className="bg-school-paper">
          <div className="mx-auto max-w-[1440px] px-4 py-16 sm:px-6 lg:px-10 lg:py-24">
            <div className="grid gap-8 xl:grid-cols-[1.35fr_0.65fr]">
              <article className="overflow-hidden border border-slate-200 bg-white">
                <div className="flex flex-wrap items-end justify-between gap-5 border-b border-slate-200 px-6 py-6 lg:px-8">
                  <div>
                    <p className="font-display text-base font-semibold uppercase tracking-[0.16em] text-school-blue">
                      Dossiers récents
                    </p>

                    <h2 className="mt-2 font-display text-4xl font-bold uppercase">
                      Élèves
                    </h2>
                  </div>

                  <a
                    href="#eleves"
                    className="inline-flex items-center gap-2 bg-brand-yellow px-5 py-3 font-display text-lg font-semibold uppercase text-black"
                  >
                    Voir tous
                    <ArrowRight size={19} />
                  </a>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full min-w-[680px] text-left">
                    <thead className="bg-school-blue-dark text-white">
                      <tr>
                        <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider lg:px-8">
                          Matricule
                        </th>

                        <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider">
                          Élève
                        </th>

                        <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider">
                          Classe
                        </th>

                        <th className="px-6 py-4 text-xs font-bold uppercase tracking-wider">
                          Action
                        </th>
                      </tr>
                    </thead>

                    <tbody>
                      {students.map((student) => (
                        <tr
                          key={student.matricule}
                          className="border-b border-slate-200 last:border-b-0 hover:bg-school-blue-light/40"
                        >
                          <td className="px-6 py-5 text-sm font-semibold text-school-muted lg:px-8">
                            {student.matricule}
                          </td>

                          <td className="px-6 py-5 font-bold">
                            {student.name}
                          </td>

                          <td className="px-6 py-5 text-school-muted">
                            {student.className}
                          </td>

                          <td className="px-6 py-5">
                            <button
                              type="button"
                              className="font-display text-lg font-semibold uppercase text-school-blue underline decoration-brand-yellow decoration-2 underline-offset-4"
                            >
                              Consulter
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </article>

              <aside className="flex flex-col bg-brand-black p-7 text-white lg:p-9">
                <span className="grid size-14 place-items-center bg-brand-yellow text-black">
                  <ShieldCheck size={28} />
                </span>

                <p className="mt-9 font-display text-base font-semibold uppercase tracking-[0.18em] text-brand-yellow">
                  Application sécurisée
                </p>

                <h2 className="mt-3 font-display text-4xl font-bold uppercase leading-tight">
                  Un accès adapté à chaque utilisateur
                </h2>

                <p className="mt-5 leading-7 text-zinc-400">
                  Administrateurs, enseignants et
                  responsables disposent d’un accès
                  authentifié par JWT.
                </p>

                <div className="mt-9 border-t border-zinc-800 pt-7">
                  <div className="flex items-center gap-4">
                    <BookOpenCheck
                      size={25}
                      className="text-brand-yellow"
                    />

                    <span className="font-semibold">
                      Données scolaires centralisées
                    </span>
                  </div>

                  <div className="mt-5 flex items-center gap-4">
                    <FileDown
                      size={25}
                      className="text-brand-yellow"
                    />

                    <span className="font-semibold">
                      Bulletins exportables en PDF
                    </span>
                  </div>
                </div>

                <a
                  id="connexion"
                  href="/connexion"
                  className="mt-auto inline-flex min-h-13 items-center justify-center gap-3 bg-brand-yellow px-6 py-3 font-display text-lg font-semibold uppercase text-black"
                >
                  Accéder à l’espace sécurisé
                  <ArrowRight size={20} />
                </a>
              </aside>
            </div>
          </div>
        </section>
      </main>

      <footer className="bg-school-blue-dark text-white">
        <div className="mx-auto flex max-w-[1440px] flex-col gap-3 px-4 py-8 text-sm sm:px-6 md:flex-row md:items-center md:justify-between lg:px-10">
          <p className="font-semibold">
            Gestion des élèves
          </p>

          <p className="text-white/60">
            Projet CDA · Application Spring Boot et React
          </p>
        </div>
      </footer>
    </div>
  );
}
