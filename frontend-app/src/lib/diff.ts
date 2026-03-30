export type TextOperation =
  | { type: "INSERT"; index: number; value: string }
  | { type: "DELETE"; index: number; length: number };

export function computeTextOperations(previous: string, next: string): TextOperation[] {
  if (previous === next) {
    return [];
  }

  let start = 0;
  while (start < previous.length && start < next.length && previous[start] === next[start]) {
    start += 1;
  }

  let previousEnd = previous.length - 1;
  let nextEnd = next.length - 1;

  while (previousEnd >= start && nextEnd >= start && previous[previousEnd] === next[nextEnd]) {
    previousEnd -= 1;
    nextEnd -= 1;
  }

  const removed = previous.slice(start, previousEnd + 1);
  const added = next.slice(start, nextEnd + 1);

  const operations: TextOperation[] = [];

  if (removed.length > 0 && added.length === 0) {
    operations.push({ type: "DELETE", index: start, length: removed.length });
    return operations;
  }

  if (added.length > 0 && removed.length === 0) {
    operations.push({ type: "INSERT", index: start, value: added });
    return operations;
  }

  if (removed.length > 0) {
    operations.push({ type: "DELETE", index: start, length: removed.length });
  }
  if (added.length > 0) {
    operations.push({ type: "INSERT", index: start, value: added });
  }

  return operations;
}
