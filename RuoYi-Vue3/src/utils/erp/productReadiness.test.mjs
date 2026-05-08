import { strict as assert } from "node:assert";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";
import { build } from "esbuild";

const testDirname = path.dirname(fileURLToPath(import.meta.url));

async function loadModule() {
  const outdir = await mkdtemp(path.join(tmpdir(), "product-readiness-"));
  const outfile = path.join(outdir, "productReadiness.mjs");

  await build({
    entryPoints: [path.join(testDirname, "productReadiness.ts")],
    outfile,
    bundle: true,
    format: "esm",
    platform: "node",
  });

  const module = await import(pathToFileURL(outfile).href);
  return {
    module,
    cleanup: () => rm(outdir, { recursive: true, force: true }),
  };
}

const { module: productReadiness, cleanup } = await loadModule();

test.after(cleanup);

function createReadyForm(overrides = {}) {
  return {
    productTitle: "Walnut Console Cabinet",
    productType: "Cabinet",
    bodyHtml: "<p>Detailed product description</p>",
    description: "Detailed product description",
    imageSearchKeyword: "DDEDL0024",
    mediaList: [
      {
        mediaId: 10,
        filename: "main.jpg",
        nasMediaUrl: "/profile/main.jpg",
        mediaContentType: "IMAGE",
      },
    ],
    ...overrides,
  };
}

function createReadyVariant(overrides = {}) {
  return {
    variantId: 1,
    sku: "DDEDL0024-001",
    mediaId: 10,
    isActiveAvailable: "1",
    purchasePrice: 340,
    pkLength: 20,
    pkWidth: 20,
    pkHeight: 20,
    pkWeight: 2.5,
    freight: 45,
    profitRate: 32,
    price: 99,
    ...overrides,
  };
}

test("missing publish-blocking fields returns must-fix issues", () => {
  const result = productReadiness.evaluateProductReadiness({
    formData: createReadyForm({
      productTitle: "",
      imageSearchKeyword: "",
      mediaList: [],
    }),
    variants: [createReadyVariant({ sku: "", price: 0 })],
  });

  const issueIds = result.issues.map((issue) => issue.id);

  assert.equal(result.errorCount, 6);
  assert.ok(issueIds.includes("product-title-required"));
  assert.ok(issueIds.includes("image-search-keyword-required"));
  assert.ok(issueIds.includes("media-image-required"));
  assert.ok(issueIds.includes("variant-1-media-missing"));
  assert.ok(issueIds.includes("variant-1-sku-required"));
  assert.ok(issueIds.includes("variant-1-price-required"));
});

test("variant image binding outside media list returns must-fix issue", () => {
  const result = productReadiness.evaluateProductReadiness({
    formData: createReadyForm(),
    variants: [createReadyVariant({ mediaId: 999 })],
  });

  assert.equal(result.errorCount, 1);
  assert.equal(result.issues[0].id, "variant-1-media-missing");
});

test("missing cost and package inputs returns optimization warnings", () => {
  const result = productReadiness.evaluateProductReadiness({
    formData: createReadyForm({ bodyHtml: "", description: "", productType: "" }),
    variants: [
      createReadyVariant({
        purchasePrice: null,
        pkLength: null,
        pkWidth: null,
        pkHeight: null,
        pkWeight: null,
        freight: null,
        profitRate: null,
        mediaId: null,
      }),
    ],
  });

  const issueIds = result.warningIssues.map((issue) => issue.id);

  assert.equal(result.errorCount, 0);
  assert.ok(issueIds.includes("body-html-recommended"));
  assert.ok(issueIds.includes("product-type-recommended"));
  assert.ok(issueIds.includes("variant-1-purchase-price-recommended"));
  assert.ok(issueIds.includes("variant-1-package-size-recommended"));
  assert.ok(issueIds.includes("variant-1-weight-recommended"));
  assert.ok(issueIds.includes("variant-1-freight-recommended"));
  assert.ok(issueIds.includes("variant-1-profit-rate-recommended"));
  assert.ok(issueIds.includes("variant-1-media-recommended"));
});

test("complete product returns 100 readiness completion", () => {
  const result = productReadiness.evaluateProductReadiness({
    formData: createReadyForm(),
    variants: [createReadyVariant()],
  });

  assert.equal(result.errorCount, 0);
  assert.equal(result.warningCount, 0);
  assert.equal(result.completion, 100);
});
