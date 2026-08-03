import {
  useState,
} from "react";

import {
  ArrowLeft,
  Eye,
  EyeOff,
  GraduationCap,
  LogIn,
  ShieldCheck,
} from "lucide-react";

import {
  Link,
  Navigate,
  useLocation,
  useNavigate,
} from "react-router";

import { ApiError } from "../api/http";
import Button from "../components/ui/Button";
import Field from "../components/ui/Field";
import { useAuth } from "../context/authContextCore";

const initialForm = {
  emailConnexion: "",
  motDePasse: "",
};

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const {
    isAuthenticated,
    login,
  } = useAuth();

  const [form, setForm] =
    useState(initialForm);

  const [errors, setErrors] =
    useState({});

  const [globalError, setGlobalError] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  const [showPassword, setShowPassword] =
    useState(false);

  if (isAuthenticated) {
    return (
      <Navigate
        to="/espace"
        replace
      />
    );
  }

  function updateField(event) {
    const {
      name,
      value,
    } = event.target;

    setForm((currentForm) => ({
      ...currentForm,
      [name]: value,
    }));

    setErrors((currentErrors) => ({
      ...currentErrors,
      [name]: "",
    }));

    setGlobalError("");
  }

  function validateForm() {
    const nextErrors = {};

    if (!form.emailConnexion.trim()) {
      nextErrors.emailConnexion =
        "L’adresse électronique est obligatoire.";
    }

    if (!form.motDePasse) {
      nextErrors.motDePasse =
        "Le mot de passe est obligatoire.";
    } else if (
      form.motDePasse.length < 8
    ) {
      nextErrors.motDePasse =
        "Le mot de passe doit contenir au moins 8 caractères.";
    }

    setErrors(nextErrors);

    return (
      Object.keys(nextErrors).length === 0
    );
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setGlobalError("");

    try {
      await login({
        emailConnexion:
          form.emailConnexion.trim(),
        motDePasse:
          form.motDePasse,
      });

      const destination =
        typeof location.state?.from === "string"
          ? location.state.from
          : "/espace";

      navigate(
        destination,
        {
          replace: true,
        },
      );
    } catch (error) {
      if (error instanceof ApiError) {
        const validationErrors =
          error.details?.validationErrors;

        if (
          validationErrors &&
          typeof validationErrors === "object"
        ) {
          setErrors(validationErrors);
        }

        if (error.status === 401) {
          setGlobalError(
            "Adresse électronique ou mot de passe incorrect.",
          );
        } else {
          setGlobalError(
            error.message,
          );
        }
      } else {
        setGlobalError(
          "Le serveur est actuellement inaccessible.",
        );
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="grid min-h-screen bg-brand-black lg:grid-cols-[1.1fr_0.9fr]">
      <section className="relative hidden min-h-screen overflow-hidden lg:block">
        <img
          src="/images/hero-campus.jpg"
          alt="Étudiants réunis sur un campus"
          className="absolute inset-0 size-full object-cover"
        />

        <div className="absolute inset-0 bg-gradient-to-t from-school-blue-dark via-school-blue-dark/55 to-school-blue-dark/15" />

        <div className="relative flex min-h-screen flex-col justify-between p-12 text-white xl:p-16">
          <Link
            to="/"
            className="inline-flex w-fit items-center gap-2 font-semibold transition hover:text-brand-yellow"
          >
            <ArrowLeft size={19} />
            Retour à l’accueil
          </Link>

          <div className="max-w-2xl">
            <p className="font-display text-lg font-semibold uppercase tracking-[0.2em] text-brand-yellow">
              Portail sécurisé
            </p>

            <h1 className="mt-5 font-display text-6xl font-bold uppercase leading-[0.92] xl:text-7xl">
              Accédez à votre espace scolaire
            </h1>

            <p className="mt-7 max-w-xl text-lg leading-8 text-white/80">
              Retrouvez les élèves, les notes et les
              bulletins depuis une interface unique,
              adaptée à votre rôle.
            </p>
          </div>
        </div>
      </section>

      <section className="flex min-h-screen items-center px-5 py-10 sm:px-10 lg:px-14 xl:px-20">
        <div className="mx-auto w-full max-w-lg">
          <Link
            to="/"
            className="inline-flex items-center gap-2 text-sm font-semibold text-zinc-400 transition hover:text-brand-yellow lg:hidden"
          >
            <ArrowLeft size={18} />
            Retour à l’accueil
          </Link>

          <div className="mt-10 flex items-center gap-4 lg:mt-0">
            <span className="grid size-14 place-items-center bg-brand-yellow text-black">
              <GraduationCap size={30} />
            </span>

            <div>
              <p className="font-display text-3xl font-bold uppercase text-white">
                Scolarité
              </p>

              <p className="text-xs font-bold uppercase tracking-[0.2em] text-zinc-500">
                Gestion des élèves
              </p>
            </div>
          </div>

          <div className="mt-12">
            <p className="font-display text-base font-semibold uppercase tracking-[0.18em] text-brand-yellow">
              Identification
            </p>

            <h2 className="mt-2 font-display text-4xl font-bold uppercase text-white sm:text-5xl">
              Connexion
            </h2>

            <p className="mt-4 leading-7 text-zinc-400">
              Saisissez les identifiants associés à
              votre compte utilisateur.
            </p>
          </div>

          {globalError && (
            <div
              role="alert"
              className="mt-7 border border-red-500/60 bg-red-500/10 px-4 py-3 text-sm text-red-300"
            >
              {globalError}
            </div>
          )}

          <form
            className="mt-8 space-y-6"
            onSubmit={handleSubmit}
            noValidate
          >
            <Field
              id="emailConnexion"
              name="emailConnexion"
              type="email"
              label="Adresse électronique"
              placeholder="administrateur@ecole.fr"
              value={form.emailConnexion}
              onChange={updateField}
              error={errors.emailConnexion}
              autoComplete="username"
              autoFocus
              required
            />

            <div className="relative">
              <Field
                id="motDePasse"
                name="motDePasse"
                type={
                  showPassword
                    ? "text"
                    : "password"
                }
                label="Mot de passe"
                placeholder="Au moins 8 caractères"
                value={form.motDePasse}
                onChange={updateField}
                error={errors.motDePasse}
                autoComplete="current-password"
                required
              />

              <button
                type="button"
                className="absolute right-3 top-10 grid size-10 place-items-center text-zinc-500 transition hover:text-brand-yellow"
                aria-label={
                  showPassword
                    ? "Masquer le mot de passe"
                    : "Afficher le mot de passe"
                }
                onClick={() =>
                  setShowPassword(
                    (currentValue) =>
                      !currentValue,
                  )
                }
              >
                {showPassword ? (
                  <EyeOff size={20} />
                ) : (
                  <Eye size={20} />
                )}
              </button>
            </div>

            <Button
              type="submit"
              className="w-full"
              disabled={loading}
            >
              <LogIn size={19} />

              {loading
                ? "Connexion en cours..."
                : "Se connecter"}
            </Button>
          </form>

          <div className="mt-8 flex items-start gap-3 border-t border-zinc-800 pt-6 text-sm leading-6 text-zinc-500">
            <ShieldCheck
              size={21}
              className="mt-0.5 shrink-0 text-brand-yellow"
            />

            <p>
              L’accès est protégé par un jeton JWT.
              Votre session est automatiquement
              supprimée à son expiration.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
