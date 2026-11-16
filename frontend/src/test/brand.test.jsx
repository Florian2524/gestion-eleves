import { readFileSync } from "node:fs";
import { URL as NodeURL } from "node:url";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { expect, it, vi } from "vitest";
import { AuthContext } from "../context/authContextCore";
import AppHeader from "../components/layout/AppHeader";
import ConnectedHeader from "../components/layout/ConnectedHeader";
import HomePage from "../pages/HomePage";

const html = new DOMParser().parseFromString(readFileSync(new NodeURL("../../index.html", import.meta.url), "utf8"), "text/html");

it("déclare le nom de l’application, la langue et les métadonnées utiles", () => {
  expect(html.title).toBe("Scolarité | Gestion des élèves");
  expect(html.documentElement.lang).toBe("fr");
  expect(html.querySelector('meta[name="description"]').content).toBe("Application de gestion des élèves");
  expect(html.querySelector('meta[name="viewport"]').content).toContain("width=device-width");
  expect(html.querySelector('meta[name="theme-color"]').content).toBe("#062c43");
});

it("référence un favicon SVG autonome, valide et versionné dans les ressources publiques", () => {
  const link = html.querySelector('link[rel="icon"]');
  expect(link).not.toBeNull();
  expect(link.type).toBe("image/svg+xml");
  const href = link.getAttribute("href").replace("%BASE_URL%", import.meta.env.BASE_URL);
  const url = new NodeURL(href, "https://scolarite.test");
  expect(url.pathname).toBe("/favicon.svg");
  expect(url.searchParams.get("v")).toBeTruthy();
  const svg = new DOMParser().parseFromString(readFileSync(new NodeURL(`../../public${url.pathname}`, import.meta.url), "utf8"), "image/svg+xml");
  expect(svg.querySelector("parsererror")).toBeNull();
  expect(svg.documentElement.namespaceURI).toBe("http://www.w3.org/2000/svg");
  expect(svg.documentElement.getAttribute("viewBox")).toBe("0 0 32 32");
  expect(svg.querySelector("title").textContent).toBe("Scolarité");
  expect(svg.querySelector("path")).not.toBeNull();
  expect(svg.querySelector("script, image, use, filter, foreignObject, [href]")).toBeNull();
});

it("associe le bouton du menu public au menu ouvert et conserve sa fermeture", async () => {
  const user = userEvent.setup();
  render(<AuthContext.Provider value={{ isAuthenticated: false }}><AppHeader /></AuthContext.Provider>);
  const button = screen.getByRole("button", { name: "Ouvrir le menu" });
  expect(button).toHaveAttribute("aria-expanded", "false");
  await user.click(button);
  const menu = screen.getByRole("navigation", { name: "Navigation mobile" });
  expect(menu.id).toBe(button.getAttribute("aria-controls"));
  expect(button).toHaveAttribute("aria-expanded", "true");
  await user.click(screen.getByRole("button", { name: "Fermer le menu" }));
  expect(screen.queryByRole("navigation", { name: "Navigation mobile" })).not.toBeInTheDocument();
  expect(button).toHaveAttribute("aria-expanded", "false");
});

it("conserve une identification cohérente dans l’en-tête connecté et le pied de page public", () => {
  const context = { auth: { role: "ADMIN" }, isAuthenticated: false, logout: vi.fn() };
  const view = render(<AuthContext.Provider value={context}><MemoryRouter><ConnectedHeader /></MemoryRouter></AuthContext.Provider>);
  const header = within(screen.getByRole("banner"));
  expect(header.getByText("Scolarité")).toBeInTheDocument();
  expect(header.getByText("Gestion des élèves")).toBeInTheDocument();
  expect(header.getByRole("link", { name: "Scolarité — accueil public" })).toHaveAttribute("href", "/");
  view.unmount();
  render(<AuthContext.Provider value={context}><HomePage /></AuthContext.Provider>);
  expect(within(screen.getByRole("contentinfo")).getByText("Scolarité · Gestion des élèves")).toBeInTheDocument();
});
