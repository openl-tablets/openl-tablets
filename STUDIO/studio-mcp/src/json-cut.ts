/**
 * A JSON value cut to a length while it stays the same structure: an agent reads what is left by the same paths and
 * learns from the cuts what it lacks, where a preview of the text cut at the limit would be neither JSON nor complete.
 */

import { formatCount } from "./text.js";

/** The longest text a cut value keeps of a string. */
const LONGEST_TEXT = 200;

/** A node of the value with the place it holds: its path, its parent and the key or index it has there. */
interface Node {
  path: string;
  parent: Record<string, unknown> | unknown[];
  key: string | number;
  value: unknown;
  size: number;
}

/**
 * The value cut to fit `limit` characters of JSON, and the cuts made, by path; a value that fits is left whole.
 * Strings longer than 200 characters are shortened first; then the largest arrays lose items from their middle,
 * keeping their first and their last ones, and when no array has more than one item left, the largest objects are
 * replaced by the number of their keys. An `enum` of a JSON Schema is never cut: what is left of it would name the
 * values allowed wrong, where the count of the keys of the object holding it names a cut.
 */
export function cutJson(value: unknown, limit: number): { value: unknown; cuts: string[] } {
  if (sizeOf(value) <= limit) {
    return { value, cuts: [] };
  }
  const root = { value: structuredClone(value) };
  const cuts: string[] = [];
  const shortened = shortenTexts(root, "value");
  if (shortened > 0) {
    cuts.push(`${formatCount(shortened, "text")} to ${LONGEST_TEXT} characters`);
  }
  let scan = nodesOf(root.value);
  while (scan.size > limit) {
    const arrays = scan.arrays.filter((node) => node.key !== "enum" && (node.value as unknown[]).length > 1);
    if (arrays.length > 0) {
      cutLargest(arrays, scan.size, limit, (node, size) => cutMiddle(node, size, limit, cuts));
    } else if (scan.objects.length > 0) {
      cutLargest(scan.objects, scan.size, limit, (node) => cutObject(node, cuts));
    } else {
      break;
    }
    scan = nodesOf(root.value);
  }
  return { value: root.value, cuts };
}

/**
 * Cut the largest of the nodes one scan found, by `cut`, which answers the size a node has left, until the sizes the
 * scan knows fit the limit. A node inside one already cut waits for the next scan, which measures what is left.
 */
function cutLargest(nodes: Node[], size: number, limit: number, cut: (node: Node, size: number) => number): void {
  const cutPaths: string[] = [];
  for (const node of nodes.toSorted((a, b) => b.size - a.size)) {
    if (size <= limit) {
      return;
    }
    if (!cutPaths.some((path) => node.path.startsWith(path) && ".[".includes(node.path[path.length]))) {
      size -= node.size - cut(node, size);
      cutPaths.push(node.path);
    }
  }
}

/** Shorten every string of the value longer than {@link LONGEST_TEXT}, and count them. */
function shortenTexts(parent: Record<string, unknown> | unknown[], key: string | number): number {
  const value = (parent as Record<string | number, unknown>)[key];
  if (typeof value === "string") {
    if (value.length <= LONGEST_TEXT) {
      return 0;
    }
    (parent as Record<string | number, unknown>)[key] = `${value.slice(0, LONGEST_TEXT)}…`;
    return 1;
  }
  if (value === null || typeof value !== "object") {
    return 0;
  }
  const keys: Array<string | number> = Array.isArray(value) ? [...value.keys()] : Object.keys(value);
  return keys.reduce<number>((count, child) => count + shortenTexts(value as unknown[], child), 0);
}

/**
 * Remove items from the middle of an array, as many as its share of the excess, name the cut, and answer the size
 * the array has left.
 */
function cutMiddle(node: Node, size: number, limit: number, cuts: string[]): number {
  const items = node.value as unknown[];
  // Keep the share of the items that the limit is of the whole, at most half of them, so a large excess is cut at once;
  // the first and the last item stay while the array has more than two: the last of a stack is where the run stopped
  const keep = Math.max(items.length > 2 ? 2 : 1,
    Math.min(Math.floor(items.length / 2), Math.floor((items.length * limit) / size)));
  const head = Math.ceil(keep / 2);
  const kept = [...items.slice(0, head), ...items.slice(items.length - (keep - head))];
  (node.parent as Record<string | number, unknown>)[node.key] = kept;
  cuts.push(`${node.path} (${items.length - keep} of ${items.length} items from the middle)`);
  return sizeOf(kept);
}

/** Replace an object by the number of its keys, name the cut, and answer the size of what replaces it. */
function cutObject(node: Node, cuts: string[]): number {
  const keys = Object.keys(node.value as object).length;
  const replacement = `{${keys} keys cut}`;
  (node.parent as Record<string | number, unknown>)[node.key] = replacement;
  cuts.push(`${node.path} (${keys} keys)`);
  return sizeOf(replacement);
}

/** The arrays and the objects of a value below its root, each with its path and its size in JSON, and its size. */
function nodesOf(value: unknown): { arrays: Node[]; objects: Node[]; size: number } {
  const arrays: Node[] = [];
  const objects: Node[] = [];
  const visit = (parent: Record<string, unknown> | unknown[], key: string | number, path: string): number => {
    const child = (parent as Record<string | number, unknown>)[key];
    if (child === null || typeof child !== "object") {
      // An undefined item of an array is null in JSON
      return JSON.stringify(child)?.length ?? "null".length;
    }
    const entries: Array<[string | number, string]> = Array.isArray(child)
      ? child.map((_, index) => [index, `${path}[${index}]`])
      // JSON leaves out a key whose value is undefined
      : Object.keys(child).filter((name) => (child as Record<string, unknown>)[name] !== undefined)
        .map((name) => [name, path ? `${path}.${name}` : name]);
    const size = 2 + Math.max(0, entries.length - 1) + entries.reduce((sum, [name, childPath]) =>
      sum + (Array.isArray(child) ? 0 : JSON.stringify(name).length + 1) + visit(child as object as unknown[], name, childPath), 0);
    if (path) {
      (Array.isArray(child) ? arrays : objects).push({ path, parent, key, value: child, size });
    }
    return size;
  };
  return { arrays, objects, size: visit({ value }, "value", "") };
}

/** The length of the value in JSON. */
function sizeOf(value: unknown): number {
  return JSON.stringify(value)?.length ?? 0;
}
