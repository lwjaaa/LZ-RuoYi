import { strict as assert } from "node:assert";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";
import { build } from "esbuild";

const testDirname = path.dirname(fileURLToPath(import.meta.url));

async function loadModule() {
  const outdir = await mkdtemp(path.join(tmpdir(), "product-workbench-"));
  const outfile = path.join(outdir, "productWorkbench.mjs");

  await build({
    entryPoints: [path.join(testDirname, "productWorkbench.ts")],
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

const { module: productWorkbench, cleanup } = await loadModule();

test.after(cleanup);

test("failed sync status has higher priority than need-resync", () => {
  const state = productWorkbench.getProductSyncState({
    syncStatus: "2",
    needResync: true,
    syncMessage: "media upload failed",
  });

  assert.equal(state.key, "failed");
  assert.equal(state.label, "同步失败");
  assert.equal(state.primaryAction, "修复");
});

test("partial sync status points users to task details", () => {
  const state = productWorkbench.getProductSyncState({
    syncStatus: "4",
    latestTaskStatus: "PART_SUCCESS",
    latestTaskError: "variant failed",
  });

  assert.equal(state.key, "partial");
  assert.equal(state.primaryAction, "查看明细");
  assert.equal(state.description, "variant failed");
});

test("synced products with newer local edits are marked for resync", () => {
  const state = productWorkbench.getProductSyncState({
    syncStatus: "1",
    shopifyProductId: "gid://shopify/Product/100",
    needResync: true,
  });

  assert.equal(state.key, "need_resync");
  assert.equal(state.label, "需重新同步");
  assert.equal(state.primaryAction, "重同步");
});

test("missing field codes become maintenance labels", () => {
  const labels = productWorkbench.formatMissingFields([
    "MAIN_MEDIA",
    "PRICE",
    "FREIGHT",
  ]);

  assert.deepEqual(labels, ["缺主图", "缺售价", "缺运费"]);
});

test("missing field severity follows store required fields", () => {
  assert.equal(productWorkbench.getMissingFieldSeverity([], ["PRICE"]), "success");
  assert.equal(productWorkbench.getMissingFieldSeverity(["PURCHASE_PRICE"], ["PRICE"]), "warning");
  assert.equal(productWorkbench.getMissingFieldSeverity(["PRICE"], ["PRICE"]), "danger");
});
