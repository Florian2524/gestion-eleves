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

const highlights = [
  {
    value: "Dossiers",
    label: "Élèves et scolarités",
  },
  {
    value: "Résultats",
    label: "Notes et évaluations",
  },
  {
    value: "Bulletins",
    label: "Consultation et téléchargement",
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
            {highlights.map((highlight) => (
              <article
                key={highlight.label}
                className="border-b border-white/20 px-8 py-12 md:border-b-0 md:border-r md:last:border-r-0 lg:px-12 lg:py-16"
              >
                <p className="font-display text-4xl font-bold uppercase text-brand-yellow sm:text-5xl">
                  {highlight.value}
                </p>

                <p className="mt-3 font-display text-lg font-semibold uppercase tracking-wide">
                  {highlight.label}
                </p>
              </article>
            ))}
          </div>
        </section>

        <section className="bg-school-paper">
          <div className="mx-auto max-w-[1440px] px-4 py-16 sm:px-6 lg:px-10 lg:py-24">
            <div className="grid gap-8 xl:grid-cols-[1.2fr_0.8fr]">
              <article className="border border-slate-200 bg-white p-7 lg:p-10">
                <p className="font-display text-base font-semibold uppercase tracking-[0.16em] text-school-blue">
                  Organisation scolaire
                </p>

                <h2 className="mt-3 max-w-3xl font-display text-4xl font-bold uppercase leading-tight text-school-ink sm:text-5xl">
                  Un suivi clair, du dossier au bulletin
                </h2>

                <p className="mt-6 max-w-3xl leading-7 text-school-muted">
                  Centralisez les informations utiles
                  et facilitez le suivi quotidien de la
                  scolarité depuis un espace unique.
                </p>

                <div className="mt-10 grid gap-5 sm:grid-cols-3">
                  <div className="border-t-4 border-brand-yellow bg-school-paper p-5">
                    <Users
                      size={27}
                      className="text-school-blue"
                    />

                    <h3 className="mt-5 font-display text-2xl font-bold uppercase text-school-blue">
                      Dossiers élèves
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-school-muted">
                      Coordonnées, parcours et
                      informations scolaires.
                    </p>
                  </div>

                  <div className="border-t-4 border-brand-yellow bg-school-paper p-5">
                    <NotebookPen
                      size={27}
                      className="text-school-blue"
                    />

                    <h3 className="mt-5 font-display text-2xl font-bold uppercase text-school-blue">
                      Résultats
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-school-muted">
                      Évaluations, notes et suivi de la
                      progression.
                    </p>
                  </div>

                  <div className="border-t-4 border-brand-yellow bg-school-paper p-5">
                    <FileDown
                      size={27}
                      className="text-school-blue"
                    />

                    <h3 className="mt-5 font-display text-2xl font-bold uppercase text-school-blue">
                      Bulletins
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-school-muted">
                      Moyennes et documents disponibles
                      au téléchargement.
                    </p>
                  </div>
                </div>
              </article>

              <aside className="flex flex-col bg-brand-black p-7 text-white lg:p-9">
                <span className="grid size-14 place-items-center bg-brand-yellow text-black">
                  <ShieldCheck size={28} />
                </span>

                <p className="mt-9 font-display text-base font-semibold uppercase tracking-[0.18em] text-brand-yellow">
                  Accès personnel
                </p>

                <h2 className="mt-3 font-display text-4xl font-bold uppercase leading-tight">
                  Connectez-vous à votre espace
                </h2>

                <p className="mt-5 leading-7 text-zinc-400">
                  Chaque utilisateur accède aux
                  fonctionnalités correspondant à son
                  rôle au sein de l’établissement.
                </p>

                <div className="mt-9 border-t border-zinc-800 pt-7">
                  <div className="flex items-center gap-4">
                    <BookOpenCheck
                      size={25}
                      className="text-brand-yellow"
                    />

                    <span className="font-semibold">
                      Données accessibles selon le profil
                    </span>
                  </div>

                  <div className="mt-5 flex items-center gap-4">
                    <FileDown
                      size={25}
                      className="text-brand-yellow"
                    />

                    <span className="font-semibold">
                      Bulletins disponibles au téléchargement
                    </span>
                  </div>
                </div>

                <a
                  id="connexion"
                  href="/connexion"
                  className="mt-auto inline-flex min-h-13 items-center justify-center gap-3 bg-brand-yellow px-6 py-3 font-display text-lg font-semibold uppercase text-black"
                >
                  Accéder à mon espace
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
            Établissement scolaire · Suivi de la scolarité
          </p>
        </div>
      </footer>
    </div>
  );
}
