import { strict as assert } from "node:assert";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";
import { build } from "esbuild";

const testDirname = path.dirname(fileURLToPath(import.meta.url));

async function loadDirtyCompareModule() {
  const outdir = await mkdtemp(path.join(tmpdir(), "dirty-compare-"));
  const outfile = path.join(outdir, "dirtyCompare.mjs");

  await build({
    entryPoints: [path.join(testDirname, "dirtyCompare.ts")],
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

const { module: dirtyCompare, cleanup } = await loadDirtyCompareModule();

test.after(cleanup);

test("stableStringify ignores object key order and normalizes empty values", () => {
  const left = {
    b: 1,
    a: {
      d: "",
      c: undefined,
    },
  };
  const right = {
    a: {
      c: null,
      d: null,
    },
    b: 1,
  };

  assert.equal(dirtyCompare.stableStringify(left), dirtyCompare.stableStringify(right));
});

test("stableStringify sorts tagIds but preserves business array order", () => {
  const snapshot = {
    step1FormData: {
      tagIds: [3, 1, 2],
    },
    mediaList: [{ mediaId: 1 }, { mediaId: 2 }],
  };
  const currentWithSameTags = {
    step1FormData: {
      tagIds: [2, 3, 1],
    },
    mediaList: [{ mediaId: 1 }, { mediaId: 2 }],
  };
  const currentWithReorderedMedia = {
    step1FormData: {
      tagIds: [2, 3, 1],
    },
    mediaList: [{ mediaId: 2 }, { mediaId: 1 }],
  };

  assert.equal(
    dirtyCompare.stableStringify(snapshot),
    dirtyCompare.stableStringify(currentWithSameTags),
  );
  assert.notEqual(
    dirtyCompare.stableStringify(snapshot),
    dirtyCompare.stableStringify(currentWithReorderedMedia),
  );
});
