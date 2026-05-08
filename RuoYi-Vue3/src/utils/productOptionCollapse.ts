export interface CollapsibleOption {
  collapsed?: boolean;
}

export function collectExpandedOptionIndexes(
  options: readonly CollapsibleOption[],
): number[] {
  return options.reduce<number[]>((indexes, option, index) => {
    if (!option.collapsed) {
      indexes.push(index);
    }
    return indexes;
  }, []);
}
