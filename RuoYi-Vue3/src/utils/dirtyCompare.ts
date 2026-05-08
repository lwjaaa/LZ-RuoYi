export type NormalizedDirtyValue =
  | null
  | string
  | number
  | boolean
  | NormalizedDirtyValue[]
  | { [key: string]: NormalizedDirtyValue };

export interface DirtyNormalizeOptions {
  ignoredKeys?: readonly string[];
  unorderedArrayKeys?: readonly string[];
}

const DEFAULT_IGNORED_KEYS = ["collapsed"];
const DEFAULT_UNORDERED_ARRAY_KEYS = ["tagIds"];

function buildKeySet(
  customKeys: readonly string[] | undefined,
  defaultKeys: readonly string[],
): Set<string> {
  return new Set([...(customKeys || []), ...defaultKeys]);
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Object.prototype.toString.call(value) === "[object Object]";
}

function normalizePrimitive(value: unknown): NormalizedDirtyValue {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  if (typeof value === "number") {
    return Number.isNaN(value) ? null : value;
  }
  if (typeof value === "string" || typeof value === "boolean") {
    return value;
  }
  if (value instanceof Date) {
    return value.toISOString();
  }
  return null;
}

export function normalizeDirtyModel(
  value: unknown,
  options: DirtyNormalizeOptions = {},
  currentKey = "",
): NormalizedDirtyValue {
  const ignoredKeys = buildKeySet(options.ignoredKeys, DEFAULT_IGNORED_KEYS);
  const unorderedArrayKeys = buildKeySet(
    options.unorderedArrayKeys,
    DEFAULT_UNORDERED_ARRAY_KEYS,
  );

  if (Array.isArray(value)) {
    const normalizedItems = value.map((item) =>
      normalizeDirtyModel(item, options),
    );
    if (unorderedArrayKeys.has(currentKey)) {
      return [...normalizedItems].sort((a, b) =>
        JSON.stringify(a).localeCompare(JSON.stringify(b)),
      );
    }
    return normalizedItems;
  }

  if (isPlainObject(value)) {
    return Object.keys(value)
      .filter((key) => !ignoredKeys.has(key))
      .sort()
      .reduce<{ [key: string]: NormalizedDirtyValue }>((normalized, key) => {
        normalized[key] = normalizeDirtyModel(value[key], options, key);
        return normalized;
      }, {});
  }

  return normalizePrimitive(value);
}

export function stableStringify(
  value: unknown,
  options: DirtyNormalizeOptions = {},
): string {
  return JSON.stringify(normalizeDirtyModel(value, options));
}

export function isDirtyBySnapshot(
  current: unknown,
  snapshot: unknown,
  options: DirtyNormalizeOptions = {},
): boolean {
  return stableStringify(current, options) !== stableStringify(snapshot, options);
}
