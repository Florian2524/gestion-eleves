import { defineConfig, devices } from "@playwright/test";

// La recette doit être lancée par le script dédié, jamais sur les ports de développement.
const baseURL = process.env.E2E_BASE_URL;
const apiURL = process.env.E2E_API_URL;
for (const [value, forbiddenPort] of [[baseURL, "5173"], [apiURL, "8080"]]) {
  const url = value && new URL(value);
  if (process.env.E2E_ISOLATED_RUN !== "1" || !url || url.hostname !== "127.0.0.1"
      || url.protocol !== "http:" || !url.port || url.port === forbiddenPort) {
    throw new Error("Utilisez scripts/test-e2e.sh : infrastructure isolée explicitement requise.");
  }
}

export default defineConfig({
  testDir: "./e2e",
  testMatch: "**/*.spec.js",

  timeout: 30_000,

  expect: {
    timeout: 5_000,
  },

  fullyParallel: false,
  workers: 1,

  reporter: [
    ["list"],
    ["html", {
      outputFolder: "playwright-report",
      open: "never",
    }],
  ],

  use: {
    baseURL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },

  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
      },
    },
  ],
});
