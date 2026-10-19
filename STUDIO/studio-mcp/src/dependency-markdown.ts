/**
 * The Markdown rendering of a table dependency graph: Mermaid diagrams of the executable calls and of the data model,
 * the details of the nodes, and the concise summary of the graph.
 */

import type * as Types from "./types.js";
import { formatCount, largestFitting, trimChar } from "./text.js";

function dependencyLabel(id: string, nodesById: ReadonlyMap<string, Types.TableNodeView>): string {
  const name = nodesById.get(id)?.name;
  return name ? `${String(name).replaceAll("\n", " ")} (\`${id}\`)` : `\`${id}\``;
}

function dependencyNodesById(nodes: Types.TableNodeView[]): Map<string, Types.TableNodeView> {
  return new Map(nodes.flatMap((node) => node.id ? [[node.id, node] as const] : []));
}

/** The ids of the nodes that have one. */
function nodeIds(nodes: Types.TableNodeView[]): Set<string> {
  return new Set(nodes.flatMap((node) => node.id ? [node.id] : []));
}

/** The key of an edge from `source` to `target`, which no two other ids share. */
function edgeKey(source: string, target: string): string {
  return `${source}\u0000${target}`;
}

function isDatatypeNode(node: Types.TableNodeView): node is Types.DatatypeNodeView {
  return node.kind === "Datatype";
}

function hasDependencyNodeId<T extends Types.TableNodeView>(node: T): node is T & { id: string } {
  return typeof node.id === "string" && node.id.length > 0;
}

function formatDatatypeField(
  field: Types.DatatypeNodeFieldView,
  nodesById: ReadonlyMap<string, Types.TableNodeView>,
): string {
  const name = String(field.name ?? "unnamed").replaceAll("\n", " ");
  const type = String(field.type ?? "unknown").replaceAll("\n", " ");
  const target = field.ref
    ? ` -> ${dependencyLabel(field.ref, nodesById)}`
    : "";
  return `\`${name}: ${type}\`${target}${field.collection ? " (collection)" : ""}`;
}

interface VocabularyPreviewItem {
  omitted?: number;
  value?: unknown;
}

/** Insert a presentation-only gap between the first and last backend preview values. */
function vocabularyPreviewItems(vocabulary: Types.DatatypeNodeVocabularyView): VocabularyPreviewItem[] {
  const values = vocabulary.valuesPreview ?? [];
  const omitted = Math.max(0, (vocabulary.valueCount ?? values.length) - values.length);
  if (!vocabulary.truncated || omitted === 0) return values.map((value) => ({ value }));

  const head = Math.ceil(values.length / 2);
  return [
    ...values.slice(0, head).map((value) => ({ value })),
    { omitted },
    ...values.slice(head).map((value) => ({ value })),
  ];
}

function vocabularyValueText(value: unknown): string {
  return typeof value === "string" ? value : JSON.stringify(value);
}

function formatVocabularyPreview(vocabulary: Types.DatatypeNodeVocabularyView): string {
  const items = vocabularyPreviewItems(vocabulary);
  if (items.length === 0) return "None";
  return items.map((item) => item.omitted !== undefined
    ? `+ ${item.omitted} more`
    : `\`${vocabularyValueText(item.value).replaceAll("`", "\\`")}\``
  ).join(", ");
}

/** The query a dependency graph answers, which its Markdown names. */
export interface DependencyQuery {
  scope?: string;
  tableId?: string;
  module?: string;
  layer?: string;
  direction?: string;
  depth?: number;
}

function formatDependencyQuery(context: DependencyQuery | undefined, inline: boolean = false): string {
  if (!context) return "";
  const values: string[] = [];
  if (context.scope) values.push(`scope ${context.scope}`);
  if (context.tableId) values.push(`root table \`${context.tableId}\``);
  if (context.module) values.push(`module \`${context.module}\``);
  if (context.layer) values.push(`layer ${context.layer}`);
  if (context.direction) values.push(`direction ${context.direction}`);
  if (context.depth !== undefined) values.push(`depth ${context.depth}`);
  if (values.length === 0) return "";
  return inline ? `Query: ${values.join(", ")}.` : `**Query:** ${values.join(", ")}`;
}

interface DependencyMarkdownOptions {
  detailed?: boolean;
  characterLimit?: number;
}

interface DependencyEdge {
  source: string;
  target: string;
}

interface DependencyRelations {
  dependencies: ReadonlyMap<string, string[]>;
  dependents: ReadonlyMap<string, string[]>;
}

function normalizeMermaidText(value: string | undefined, fallback: string): string {
  const normalized = (value ?? fallback).replace(/\s+/g, " ").trim();
  return normalized || fallback;
}

function escapeMermaidLabel(value: string | undefined, fallback: string): string {
  return normalizeMermaidText(value, fallback)
    .replaceAll("&", "&amp;")
    .replaceAll("%", "&#37;")
    .replaceAll("\"", "&quot;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function escapeMermaidRelationship(value: string | undefined, fallback: string): string {
  return normalizeMermaidText(value, fallback)
    .replace(/[%:;{}[\]<>|"'`]/g, " ")
    .replace(/\s+/g, " ")
    .trim() || fallback;
}

function mermaidErAttribute(value: string | undefined, fallback: string, isType: boolean): string {
  const allowed = isType ? /[^A-Za-z0-9_()[\]-]+/g : /[^A-Za-z0-9_-]+/g;
  const normalized = trimChar(normalizeMermaidText(value, fallback).replace(allowed, "_"), "_");
  return /^[A-Za-z]/.test(normalized) ? normalized : `${fallback}_${normalized || fallback}`;
}

/**
 * Mermaid ER attributes require both a type and a name. A zero-width space is
 * accepted by the ER grammar and keeps the mandatory type column visually
 * empty for vocabulary values, whose shared type is already in the entity
 * header. The omitted-values row uses it for both mandatory columns so only
 * its `+ N more` comment is visible.
 */
const MERMAID_ER_EMPTY_ATTRIBUTE_PART = "\u200B";

function collectDependencyEdges(nodes: Types.TableNodeView[]): DependencyEdge[] {
  const ids = nodeIds(nodes);
  const edges = new Map<string, DependencyEdge>();
  const add = (source: string | undefined, target: string | undefined): void => {
    if (!source || !target || !ids.has(source) || !ids.has(target)) return;
    edges.set(edgeKey(source, target), { source, target });
  };

  for (const node of nodes) {
    node.dependencies?.forEach((target) => add(node.id, target));
    node.dependents?.forEach((source) => add(source, node.id));
  }
  return [...edges.values()];
}

function dependencyRelations(nodes: Types.TableNodeView[]): DependencyRelations {
  const edges = collectDependencyEdges(nodes);
  // The ids at the other end of the edges of each node, grouped by the end the node is at
  const linked = (end: "source" | "target", other: "source" | "target"): Map<string, string[]> =>
    new Map(Array.from(Map.groupBy(edges, (edge) => edge[end]), ([id, group]) => [id, group.map((edge) => edge[other])]));
  return { dependencies: linked("source", "target"), dependents: linked("target", "source") };
}

function selectDependencyNodes(nodes: Types.TableNodeView[], count: number): Types.TableNodeView[] {
  if (count >= nodes.length) return nodes;

  const selected = new Set<Types.TableNodeView>();
  const datatype = nodes.find(isDatatypeNode);
  const executable = nodes.find((node) => !isDatatypeNode(node));
  if (datatype) selected.add(datatype);
  if (executable && selected.size < count) selected.add(executable);
  for (const node of nodes) {
    if (selected.size >= count) break;
    selected.add(node);
  }
  return nodes.filter((node) => selected.has(node));
}

const MERMAID_RESERVED_IDENTIFIERS = new Set([
  "accdescr",
  "acctitle",
  "callback",
  "class",
  "classdef",
  "classdiagram",
  "click",
  "cssclass",
  "direction",
  "end",
  "erdiagram",
  "link",
  "namespace",
  "note",
  "style",
  "title",
]);

/** `base`, or `base_2`, `base_3`… when it is taken, added to the names in `used`. */
function uniqueName(base: string, used: Set<string>): string {
  let name = base;
  let suffix = 1;
  while (used.has(name)) {
    suffix++;
    name = `${base}_${suffix}`;
  }
  used.add(name);
  return name;
}

/**
 * Keep datatype identifiers human-readable and directly usable as entity names.
 * Do not switch to Mermaid's `id["Display name"]` entity-alias syntax here:
 * embedded Mermaid versions used by some MCP clients render that syntax literally
 * in ER diagrams and create a second, disconnected entity when a relationship
 * references `id`. Sanitize names instead, prefix reserved words, and suffix
 * collisions so the same compatible identifier is used in declarations and links.
 */
function dependencyAliases(nodes: Types.TableNodeView[]): Map<string, string> {
  const aliases = new Map<string, string>();
  const datatypeAliases = new Set<string>();
  let executableIndex = 0;
  for (const node of nodes.filter(hasDependencyNodeId)) {
    aliases.set(node.id, isDatatypeNode(node)
      ? uniqueName(datatypeAlias(node.name ?? node.id), datatypeAliases)
      : `e${executableIndex++}`);
  }
  return aliases;
}

/** The name of a datatype as a Mermaid identifier: word characters, starting with a letter, never a reserved word. */
function datatypeAlias(name: string): string {
  const normalizedName = trimChar(normalizeMermaidText(name, "Datatype").replace(/\W+/g, "_"), "_");
  return /^[A-Za-z]/.test(normalizedName) && !MERMAID_RESERVED_IDENTIFIERS.has(normalizedName.toLowerCase())
    ? normalizedName
    : `Type_${normalizedName || "Datatype"}`;
}

/**
 * Vocabulary value types belong to the whole declaration, so show them once in
 * the ER entity header rather than repeating them beside every preview value.
 * These are quoted entity names, not Mermaid's incompatible `id["label"]` ER
 * alias syntax described above; the exact same name is used for declarations
 * and relationships so clients cannot create disconnected duplicate entities.
 */
function datatypeErEntityNames(
  nodes: Array<Types.DatatypeNodeView & { id: string }>,
  aliases: ReadonlyMap<string, string>,
): Map<string, string> {
  return new Map(nodes.map((node) => {
    const alias = aliases.get(node.id);
    return [node.id, node.vocabulary
      ? `"${alias}<${mermaidErAttribute(node.vocabulary.valueType, "Value", true)}>"`
      : String(alias)];
  }));
}

function formatExecutableDiagram(
  nodes: Types.TableNodeView[],
  allEdges: DependencyEdge[],
  aliases: ReadonlyMap<string, string>,
): string | undefined {
  const executableNodes = nodes.filter(
    (node): node is Types.TableNodeView & { id: string } => !isDatatypeNode(node) && hasDependencyNodeId(node),
  );
  if (executableNodes.length === 0) return undefined;

  const executableIds = new Set(executableNodes.map((node) => node.id));
  const lines = ["## Executable call graph", "", "```mermaid", "flowchart LR"];
  for (const node of executableNodes) {
    const name = escapeMermaidLabel(node.name ?? node.id, "Unnamed table");
    const kind = escapeMermaidLabel(node.kind ?? node.tableType, "Executable");
    lines.push(`  ${aliases.get(node.id)}["${name}<br/>${kind}"]`);
  }
  for (const edge of allEdges) {
    if (!executableIds.has(edge.source) || !executableIds.has(edge.target)) continue;
    lines.push(`  ${aliases.get(edge.source)} --> ${aliases.get(edge.target)}`);
  }

  const dispatchers = executableNodes.filter(
    (node) => node.kind === "Dispatcher",
  );
  const ordinary = executableNodes.filter((node) => !dispatchers.includes(node));
  lines.push(
    "  classDef executable fill:#eef5ff,stroke:#2563eb,color:#172554",
    "  classDef dispatcher fill:#fff7ed,stroke:#ea580c,color:#431407,stroke-width:2px",
  );
  if (ordinary.length > 0) {
    lines.push(`  class ${ordinary.map((node) => aliases.get(node.id)).join(",")} executable`);
  }
  if (dispatchers.length > 0) {
    lines.push(`  class ${dispatchers.map((node) => aliases.get(node.id)).join(",")} dispatcher`);
  }
  lines.push("```", "", "Arrows point from the caller to the dependency. Dispatcher nodes are highlighted.");
  return lines.join("\n");
}

/** A Markdown code span. */
function codeSpan(text: string): string {
  return `\`${text}\``;
}

/** The Mermaid ER entity of a datatype: its declared fields and the preview of its vocabulary. */
function datatypeEntityLines(node: Types.DatatypeNodeView, entityName: string | undefined): string[] {
  const vocabularyItems = node.vocabulary ? vocabularyPreviewItems(node.vocabulary) : [];
  if ((node.fields ?? []).length === 0 && vocabularyItems.length === 0) {
    return [`  ${entityName}`];
  }
  const usedMemberNames = new Set<string>();
  const fieldLines = (node.fields ?? []).map((field) => {
    const type = mermaidErAttribute(field.type, "Type", true);
    const name = uniqueName(mermaidErAttribute(field.name, "field", false), usedMemberNames);
    return `    ${type} ${name}`;
  });
  const valueLines = vocabularyItems.map((item) => {
    if (item.omitted !== undefined) {
      return `    ${MERMAID_ER_EMPTY_ATTRIBUTE_PART} ${MERMAID_ER_EMPTY_ATTRIBUTE_PART} "+ ${item.omitted} more"`;
    }
    const name = uniqueName(mermaidErAttribute(vocabularyValueText(item.value), "value", false), usedMemberNames);
    return `    ${MERMAID_ER_EMPTY_ATTRIBUTE_PART} ${name}`;
  });
  return [`  ${entityName} {`, ...fieldLines, ...valueLines, "  }"];
}

/** The links between datatypes: their references and dependencies, their inheritance, and the datatypes they join. */
interface DatatypeLinks {
  relationships: string[];
  inheritance: string[];
  connected: Set<string>;
}

function datatypeLinks(
  datatypeNodes: Array<Types.DatatypeNodeView & { id: string }>,
  allEdges: DependencyEdge[],
  aliases: ReadonlyMap<string, string>,
  entityNames: ReadonlyMap<string, string>,
): DatatypeLinks {
  const datatypeIds = new Set(datatypeNodes.map((node) => node.id));
  const links: DatatypeLinks = { relationships: [], inheritance: [], connected: new Set() };
  const representedEdges = new Set<string>();
  // A link shown between two datatypes connects both, and is not shown again as a plain dependency
  const link = (source: string, target: string): void => {
    links.connected.add(source);
    links.connected.add(target);
    representedEdges.add(edgeKey(source, target));
  };
  for (const node of datatypeNodes) {
    if (node.extends && datatypeIds.has(node.extends)) {
      links.inheritance.push(`  ${aliases.get(node.extends)} <|-- ${aliases.get(node.id)}`);
      link(node.id, node.extends);
    }
    const references = (node.fields ?? []).filter(
      (field): field is typeof field & { ref: string } => field.ref !== undefined && datatypeIds.has(field.ref),
    );
    for (const field of references) {
      const cardinality = field.collection ? "o{" : "o|";
      const label = escapeMermaidRelationship(field.name, "field");
      links.relationships.push(`  ${entityNames.get(node.id)} ||--${cardinality} ${entityNames.get(field.ref)} : ${label}`);
      link(node.id, field.ref);
    }
  }
  const dependencies = allEdges.filter((edge) => datatypeIds.has(edge.source) && datatypeIds.has(edge.target)
    && !representedEdges.has(edgeKey(edge.source, edge.target)));
  for (const edge of dependencies) {
    links.relationships.push(`  ${entityNames.get(edge.source)} ||..o{ ${entityNames.get(edge.target)} : depends on`);
    link(edge.source, edge.target);
  }
  return links;
}

function formatDatatypeDiagrams(
  nodes: Types.TableNodeView[],
  allEdges: DependencyEdge[],
  aliases: ReadonlyMap<string, string>,
): string | undefined {
  const datatypeNodes = nodes.filter(isDatatypeNode).filter(hasDependencyNodeId);
  if (datatypeNodes.length === 0) return undefined;

  const entityNames = datatypeErEntityNames(datatypeNodes, aliases);
  const links = datatypeLinks(datatypeNodes, allEdges, aliases, entityNames);
  const isolatedDatatypes = datatypeNodes.filter(
    (node) => !links.connected.has(node.id)
      && (node.fields ?? []).length === 0
      && (!node.vocabulary || vocabularyPreviewItems(node.vocabulary).length === 0),
  );
  const lines = [
    "## Data model",
    "",
    "```mermaid",
    "erDiagram",
    "  direction LR",
    ...datatypeNodes.flatMap((node) => datatypeEntityLines(node, entityNames.get(node.id))),
    ...links.relationships,
    "```",
    "",
    "Datatype entities list declared fields. Vocabulary headers use `Name<Type>` and their rows list the bounded preview without repeating the shared type; a `+ N more` row marks values omitted between its first and last entries. Reference links point from one source instance to an optional (`0..1`) or collection (`0..*`) target.",
  ];
  if (isolatedDatatypes.length > 0) {
    const names = isolatedDatatypes.map((node) => codeSpan(node.name ?? node.id)).join(", ");
    lines.push("", `Unconnected datatypes/vocabularies without displayable members: ${names}.`);
  }
  if (links.inheritance.length > 0) {
    lines.push(
      "",
      "## Datatype inheritance",
      "",
      "```mermaid",
      "classDiagram",
      ...links.inheritance,
      "```",
      "",
      "Inheritance uses a hollow triangle from the parent to the child datatype.",
    );
  }
  return lines.join("\n");
}

function formatDependencyOverview(
  allNodes: Types.TableNodeView[],
  visibleNodes: Types.TableNodeView[],
  edges: DependencyEdge[],
  context?: DependencyQuery,
): string {
  const lines = ["# Table Dependency Graph"];
  const query = formatDependencyQuery(context);
  if (query) lines.push("", query);

  const aliases = dependencyAliases(visibleNodes);
  const diagrams = [
    formatExecutableDiagram(visibleNodes, edges, aliases),
    formatDatatypeDiagrams(visibleNodes, edges, aliases),
  ].filter((diagram): diagram is string => Boolean(diagram));
  if (diagrams.length > 0) lines.push("", diagrams.join("\n\n"));
  else lines.push("", "No renderable table nodes found.");

  if (visibleNodes.length < allNodes.length) {
    lines.push("", `Diagram shows ${visibleNodes.length} of ${allNodes.length} nodes; ${allNodes.length - visibleNodes.length} omitted to keep the response within the character limit.`);
  }
  return lines.join("\n");
}

function fitDependencyOverview(
  nodes: Types.TableNodeView[],
  context: DependencyQuery | undefined,
  characterLimit: number | undefined,
): string {
  // The edges of the whole graph, collected once for every size the fitting tries
  const edges = collectDependencyEdges(nodes);
  if (characterLimit === undefined) return formatDependencyOverview(nodes, nodes, edges, context);
  const render = (count: number): string =>
    formatDependencyOverview(nodes, selectDependencyNodes(nodes, count), edges, context);
  const best = largestFitting(1, nodes.length, render, characterLimit);
  if (best) return best;

  const fallback = "# Table Dependency Graph\n\nDiagram omitted because the configured character limit is too small.";
  return fallback.slice(0, characterLimit);
}

/** The table id, the type, the project and the location of a node, as far as they are known. */
function nodeIdentityLines(node: Types.TableNodeView): string[] {
  const lines: string[] = [];
  if (node.id) lines.push(`- **Table ID:** ${codeSpan(node.id)}`);
  if (node.tableType || node.kind) {
    const kind = node.kind ? ` (${node.kind})` : "";
    lines.push(`- **Type:** ${node.tableType ?? "N/A"}${kind}`);
  }
  if (node.project) lines.push(`- **Project:** ${node.project}`);
  if (node.file || node.pos) {
    const at = node.pos ? ` at ${node.pos}` : "";
    lines.push(`- **Location:** ${node.file ?? "N/A"}${at}`);
  }
  return lines;
}

/** What a datatype declares: its parent, and its vocabulary or its fields. */
function datatypeDetailLines(node: Types.DatatypeNodeView, nodesById: ReadonlyMap<string, Types.TableNodeView>): string[] {
  const lines: string[] = [];
  if (node.extends) lines.push(`- **Extends:** ${dependencyLabel(node.extends, nodesById)}`);
  if (node.vocabulary) {
    const valueCount = node.vocabulary.valueCount ?? node.vocabulary.valuesPreview?.length ?? 0;
    const valueType = `${node.vocabulary.valueType ?? "unknown"} value`;
    lines.push(
      `- **Vocabulary:** ${formatCount(valueCount, valueType)}`,
      `- **Values preview:** ${formatVocabularyPreview(node.vocabulary)}`,
    );
  } else if (node.fields && node.fields.length > 0) {
    lines.push("- **Declared fields:**", ...node.fields.map((field) => `  - ${formatDatatypeField(field, nodesById)}`));
  } else {
    lines.push("- **Declared fields:** None");
  }
  return lines;
}

/** What an executable table declares: its signature, its return type and its dimension properties. */
function executableDetailLines(node: Exclude<Types.TableNodeView, Types.DatatypeNodeView>): string[] {
  const lines: string[] = [];
  if (node.signature) lines.push(`- **Signature:** ${codeSpan(node.signature)}`);
  if (node.returnType) lines.push(`- **Return type:** ${codeSpan(node.returnType)}`);
  if (node.dimensionProperties && Object.keys(node.dimensionProperties).length > 0) {
    lines.push(`- **Dimension properties:** ${codeSpan(JSON.stringify(node.dimensionProperties))}`);
  }
  return lines;
}

function formatDependencyNodeDetail(
  node: Types.TableNodeView,
  nodesById: ReadonlyMap<string, Types.TableNodeView>,
  relations: DependencyRelations,
): string {
  const name = String(node.name ?? node.id ?? "Unnamed table").replaceAll("\n", " ");
  const labels = (ids: string[] | undefined): string => ids?.map((id) => dependencyLabel(id, nodesById)).join(", ") || "None";
  const lines = [
    `### ${name}`,
    ...nodeIdentityLines(node),
    ...(isDatatypeNode(node) ? datatypeDetailLines(node, nodesById) : executableDetailLines(node)),
    `- **Depends on:** ${labels(node.id ? relations.dependencies.get(node.id) : node.dependencies)}`,
    `- **Used by:** ${labels(node.id ? relations.dependents.get(node.id) : node.dependents)}`,
  ];
  if (node.properties && Object.keys(node.properties).length > 0) {
    lines.push(`- **Properties:** ${codeSpan(JSON.stringify(node.properties))}`);
  }
  return lines.join("\n");
}

function appendDependencyDetails(
  overview: string,
  nodes: Types.TableNodeView[],
  characterLimit: number | undefined,
): string {
  const nodesById = dependencyNodesById(nodes);
  const relations = dependencyRelations(nodes);
  const details = nodes.map((node) => formatDependencyNodeDetail(node, nodesById, relations));
  const detailsHeader = `${overview}\n\n## Node details\n\nDependencies and reverse uses are derived from the returned graph edges. Executable and datatype layers intentionally do not link to each other.`;
  const complete = `${detailsHeader}\n\n${details.join("\n\n")}`;
  if (characterLimit === undefined || complete.length <= characterLimit) return complete;

  const omission = (omitted: number): string => omitted > 0
    ? `\n\n${formatCount(omitted, "node detail")} omitted to keep the response within the character limit.`
    : "";
  let result = detailsHeader;
  let included = 0;
  for (const detail of details) {
    if (`${result}\n\n${detail}${omission(nodes.length - included - 1)}`.length > characterLimit) break;
    result += `\n\n${detail}`;
    included++;
  }
  if ((result + omission(nodes.length - included)).length <= characterLimit) {
    result += omission(nodes.length - included);
  }
  return result.length <= characterLimit ? result : overview;
}

/** Render dependency layers as complete Mermaid diagrams with an optional metadata appendix. */
export function formatTableDependencies(
  nodes: Types.TableNodeView[],
  context?: DependencyQuery,
  options: DependencyMarkdownOptions = {},
): string {
  if (nodes.length === 0) {
    return "# Table Dependency Graph\n\nNo tables found.";
  }

  const overviewBudget = options.detailed && options.characterLimit !== undefined
    ? Math.max(Math.floor(options.characterLimit * 0.65), Math.min(options.characterLimit, 800))
    : options.characterLimit;
  const overview = fitDependencyOverview(nodes, context, overviewBudget);
  return options.detailed
    ? appendDependencyDetails(overview, nodes, options.characterLimit)
    : overview;
}

/** `every data-model node has a model-layer link`, or how many have none. */
function isolationText(isolated: number): string {
  if (isolated === 0) return "every data-model node has a model-layer link";
  const verb = isolated === 1 ? "has" : "have";
  return `${formatCount(isolated, "data-model node")} ${verb} no links within the datatype layer`;
}

/** `all value previews are complete`, or how many are truncated. */
function previewText(truncated: number): string {
  if (truncated === 0) return "all value previews are complete";
  const verb = truncated === 1 ? "is" : "are";
  return `${formatCount(truncated, "preview")} ${verb} truncated`;
}

/** The busiest executable tables of a graph: the three that call the most others. */
function highestFanOutLines(executableNodes: Types.TableNodeView[], executableLinks: DependencyEdge[]): string[] {
  const executableFanOut = Map.groupBy(executableLinks, (edge) => edge.source);
  const fanOutOf = (node: Types.TableNodeView): number => (node.id ? executableFanOut.get(node.id)?.length ?? 0 : 0);
  const highestFanOut = executableNodes
    .filter((node) => fanOutOf(node) > 0)
    .toSorted((left, right) => fanOutOf(right) - fanOutOf(left))
    .slice(0, 3)
    .map((node) => `${node.name ?? node.id ?? "unnamed"} (${formatCount(fanOutOf(node), "dependency", "dependencies")})`);
  return highestFanOut.length > 0 ? [`Highest executable fan-out: ${highestFanOut.join(", ")}.`] : [];
}

/** The data model of a graph: its fields and references, its inheritance, its links and its vocabularies. */
function dataModelLines(datatypes: Types.DatatypeNodeView[], datatypeLinks: DependencyEdge[]): string[] {
  const fields = datatypes.flatMap((node) => node.fields ?? []);
  const references = fields.filter((field) => field.ref);
  const collections = references.filter((field) => field.collection);
  const vocabularies = datatypes.flatMap((node) => node.vocabulary ? [node.vocabulary] : []);
  const vocabularyValues = vocabularies.reduce(
    (sum, vocabulary) => sum + (vocabulary.valueCount ?? vocabulary.valuesPreview?.length ?? 0), 0);
  const truncatedVocabularies = vocabularies.filter((vocabulary) => vocabulary.truncated).length;
  const inheritance = datatypes.filter((node) => node.extends).length;
  const connectedDatatypeIds = new Set(datatypeLinks.flatMap((edge) => [edge.source, edge.target]));
  const isolated = datatypes.filter((node) => !node.id || !connectedDatatypeIds.has(node.id)).length;
  const lines = [
    `Data model declares ${formatCount(fields.length, "field")}, including ${formatCount(references.length, "typed reference")} (${formatCount(collections.length, "collection")}), with ${formatCount(inheritance, "inheritance relation")}; ${isolationText(isolated)}.`,
  ];
  if (vocabularies.length > 0) {
    lines.push(
      `It also contains ${formatCount(vocabularies.length, "vocabulary", "vocabularies")} with ${formatCount(vocabularyValues, "declared value")}; ${previewText(truncatedVocabularies)}.`,
    );
  }
  return lines;
}

/** The concise summary of a dependency graph: its layers, the highest fan-out, the data model and the query. */
export function summarizeDependencyGraph(
  nodes: Types.TableNodeView[],
  total: number,
  markdownContext: DependencyQuery | undefined,
): string[] {
  const edges = collectDependencyEdges(nodes);
  const executableNodes = nodes.filter((node) => !isDatatypeNode(node));
  const datatypes = nodes.filter(isDatatypeNode);
  const executableIds = nodeIds(executableNodes);
  const datatypeIds = nodeIds(datatypes);
  const executableLinks = edges.filter((edge) => executableIds.has(edge.source) && executableIds.has(edge.target));
  const datatypeLinks = edges.filter((edge) => datatypeIds.has(edge.source) && datatypeIds.has(edge.target));
  const layerSummaries: string[] = [];
  if (executableNodes.length > 0) {
    layerSummaries.push(`${formatCount(executableNodes.length, "executable node")} with ${formatCount(executableLinks.length, "call link")}`);
  }
  if (datatypes.length > 0) {
    layerSummaries.push(`${formatCount(datatypes.length, "datatype/vocabulary node")} with ${formatCount(datatypeLinks.length, "model link")}`);
  }
  const layers = layerSummaries.length > 0 ? `: ${layerSummaries.join("; ")}` : "";
  const query = formatDependencyQuery(markdownContext, true);
  return [
    `Dependency graph contains ${formatCount(total, "node")} and ${formatCount(edges.length, "link")}${layers}.`,
    ...highestFanOutLines(executableNodes, executableLinks),
    ...(datatypes.length > 0 ? dataModelLines(datatypes, datatypeLinks) : []),
    ...(query ? [query] : []),
  ];
}
