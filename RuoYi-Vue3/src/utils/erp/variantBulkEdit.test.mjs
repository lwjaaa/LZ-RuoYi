import { strict as assert } from "node:assert";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";
import { build } from "esbuild";

const testDirname = path.dirname(fileURLToPath(import.meta.url));

async function loadModule() {
  const outdir = await mkdtemp(path.join(tmpdir(), "variant-bulk-edit-"));
  const outfile = path.join(outdir, "variantBulkEdit.mjs");

  await build({
    entryPoints: [path.join(testDirname, "variantBulkEdit.ts")],
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

const { module: variantBulkEdit, cleanup } = await loadModule();

test.after(cleanup);

function createVariant(id, overrides = {}) {
  return {
    variantId: id,
    sku: `SKU-${id}`,
    mediaId: id,
    media: { mediaId: id, filename: `${id}.jpg` },
    purchasePrice: 100,
    pkLength: 10,
    pkWidth: 10,
    pkHeight: 10,
    pkWeight: 1,
    isActualShipment: "0",
    freight: 12,
    unitCostPrice: 112,
    exchangeRate: 7.2,
    price: 25,
    compareAtPrice: 30,
    ...overrides,
  };
}

test("field patch applies only to selected variants", () => {
  const variants = [createVariant(1), createVariant(2), createVariant(3)];

  const result = variantBulkEdit.applyBulkVariantPatch(
    variants,
    [1, 3],
    variantBulkEdit.createFieldPatch("purchasePrice", 280),
  );

  assert.equal(result[0].purchasePrice, 280);
  assert.equal(result[1].purchasePrice, 100);
  assert.equal(result[2].purchasePrice, 280);
});

test("media patch binds selected variants to the selected media", () => {
  const media = { mediaId: 88, filename: "shared.jpg" };
  const variants = [createVariant(1), createVariant(2)];

  const result = variantBulkEdit.applyBulkVariantPatch(
    variants,
    [2],
    variantBulkEdit.createMediaPatch(media),
  );

  assert.equal(result[0].mediaId, 1);
  assert.equal(result[1].mediaId, 88);
  assert.deepEqual(result[1].media, media);
});

test("first selected variant fills all editable input fields downward", () => {
  const variants = [
    createVariant(1, {
      sku: "SOURCE-SKU",
      mediaId: 66,
      media: { mediaId: 66, filename: "source.jpg" },
      purchasePrice: 310,
      pkLength: 40,
      pkWidth: 30,
      pkHeight: 20,
      pkWeight: 4.5,
      isActualShipment: "1",
      freight: 58,
      unitCostPrice: 368,
      exchangeRate: 7.1,
      price: 92,
      compareAtPrice: 120,
    }),
    createVariant(2),
    createVariant(3),
  ];

  const result = variantBulkEdit.applyFirstSelectedFillDown(variants, [1, 3]);

  assert.equal(result[1].sku, "SKU-2");
  assert.equal(result[2].sku, "SOURCE-SKU");
  assert.equal(result[2].mediaId, 66);
  assert.equal(result[2].purchasePrice, 310);
  assert.equal(result[2].pkLength, 40);
  assert.equal(result[2].pkWidth, 30);
  assert.equal(result[2].pkHeight, 20);
  assert.equal(result[2].pkWeight, 4.5);
  assert.equal(result[2].isActualShipment, "1");
  assert.equal(result[2].freight, 58);
  assert.equal(result[2].unitCostPrice, 368);
  assert.equal(result[2].exchangeRate, 7.1);
  assert.equal(result[2].price, 92);
  assert.equal(result[2].compareAtPrice, 120);
});

test("no selected variants leaves list unchanged", () => {
  const variants = [createVariant(1), createVariant(2)];

  const result = variantBulkEdit.applyBulkVariantPatch(
    variants,
    [],
    variantBulkEdit.createFieldPatch("price", 49),
  );

  assert.deepEqual(result, variants);
});
