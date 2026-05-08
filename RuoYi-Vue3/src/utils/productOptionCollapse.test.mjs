import { strict as assert } from "node:assert";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";
import { build } from "esbuild";

const testDirname = path.dirname(fileURLToPath(import.meta.url));

async function loadModule() {
  const outdir = await mkdtemp(path.join(tmpdir(), "option-collapse-"));
  const outfile = path.join(outdir, "productOptionCollapse.mjs");

  await build({
    entryPoints: [path.join(testDirname, "productOptionCollapse.ts")],
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

const { module: optionCollapse, cleanup } = await loadModule();

test.after(cleanup);

test("collectExpandedOptionIndexes returns indexes for expanded options only", () => {
  const options = [
    { collapsed: true },
    { collapsed: false },
    { collapsed: false },
  ];

  assert.deepEqual(optionCollapse.collectExpandedOptionIndexes(options), [1, 2]);
});
