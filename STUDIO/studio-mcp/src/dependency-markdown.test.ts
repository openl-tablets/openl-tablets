/**
 * Unit tests for dependency-markdown.ts: the Markdown of a table dependency graph, rendered through formatResponse.
 */

import { describe, it, expect } from "vitest";
import { formatResponse } from "./formatters.js";
import { RESPONSE_LIMITS } from "./constants.js";
import type { TableNodeView } from "./types.js";

describe("table dependency formatting", () => {
  const graph: TableNodeView[] = [
    {
      id: "driver",
      name: "Driver",
      kind: "Datatype",
      tableType: "Datatype",
      extends: "party",
      dependencies: ["party", "vehicle", "driver"],
      dependents: ["driver"],
      fields: [
        { name: "name", type: "String" },
        { name: "vehicle", type: "Vehicle", ref: "vehicle" },
        { name: "otherDrivers", type: "Driver[]", ref: "driver", collection: true },
      ],
    },
    { id: "party", name: "Party", kind: "Datatype", tableType: "Datatype" },
    {
      id: "vehicle",
      name: "Vehicle",
      kind: "Datatype",
      tableType: "Vocabulary",
      vocabulary: {
        valueType: "Integer",
        valueCount: 8,
        valuesPreview: [100, 200, 300, 600, 700, 800],
        truncated: true,
      },
    },
    {
      id: "premium-dispatcher",
      name: "Premium",
      kind: "Dispatcher",
      tableType: "Dispatcher",
      dependencies: ["premium-ca", "premium-ca", "missing"],
    },
    {
      id: "premium-ca",
      name: "Premium",
      kind: "Rules",
      tableType: "SimpleRules",
      signature: "Double Premium(Driver driver)",
      returnType: "Double",
      dimensions: { effectiveDate: "2020-01-01T00:00:00.000", state: ["CA"] },
      dependents: ["premium-dispatcher"],
    },
  ];

  it("preserves discriminated graph nodes in JSON", () => {
    const result = JSON.parse(formatResponse(graph, "json", { dataType: "table_dependencies" }));

    expect(result.data).toEqual(graph);
  });

  it("renders mixed dependency layers as separate Mermaid diagrams", () => {
    const result = formatResponse(graph, "markdown", {
      dataType: "table_dependencies",
      markdownContext: { scope: "whole project", layer: "all" },
    });

    expect(result).toContain("## Executable call graph");
    expect(result).toContain("flowchart LR");
    expect(result).toContain('e0["Premium<br/>Dispatcher"]');
    expect(result).toContain("e0 --> e1");
    expect(result).toContain("class e0 dispatcher");
    expect(result).toContain("## Data model");
    expect(result).toContain("erDiagram");
    expect(result).toContain("Driver {");
    expect(result).toContain("String name");
    expect(result).toContain("Vehicle vehicle");
    expect(result).toContain("Driver[] otherDrivers");
    expect(result).toContain('"Vehicle<Integer>" {');
    expect(result).toContain("\u200B value_100");
    expect(result).toContain('\u200B \u200B "+ 2 more"');
    expect(result).toContain("\u200B value_800");
    expect(result).toContain('Driver ||--o| "Vehicle<Integer>" : vehicle');
    expect(result).toContain('Driver ||--o{ Driver : otherDrivers');
    expect(result).toContain("## Datatype inheritance");
    expect(result).toContain("classDiagram");
    expect(result).toContain("Party <|-- Driver");
    expect(result.match(/```mermaid/g)).toHaveLength(3);
    expect(result).toContain("layer all");
    expect(result).not.toContain("## Node details");
    expect(result).not.toContain("**Table ID:**");
  });

  it("adds node metadata after the Mermaid diagrams in detailed Markdown", () => {
    const result = formatResponse(graph, "markdown_detailed", {
      dataType: "table_dependencies",
      markdownContext: { scope: "whole project", layer: "all" },
    });

    expect(result).toContain("flowchart LR");
    expect(result).toContain("classDiagram");
    expect(result).toContain("## Node details");
    expect(result).toContain("**Extends:** Party (`party`)");
    expect(result).toContain("`vehicle: Vehicle`");
    expect(result).toContain("`otherDrivers: Driver[]`");
    expect(result).toContain("(collection)");
    expect(result).toContain("**Vocabulary:** 8 Integer values");
    expect(result).toContain("**Values preview:** `100`, `200`, `300`, + 2 more, `600`, `700`, `800`");
    expect(result).toContain("**Used by:** Driver (`driver`)");
    expect(result).toContain("**Signature:** `Double Premium(Driver driver)`");
    expect(result).toContain(
      "**Dimensions:** `{\"effectiveDate\":\"2020-01-01T00:00:00.000\",\"state\":[\"CA\"]}`",
    );
    // a version is named by the dimensions that tell it from the others, its dates as the API answers them
    expect(result).toContain("### Premium [effectiveDate=2020-01-01T00:00:00.000, state=CA]");
    expect(result).toContain("Premium [effectiveDate=2020-01-01T00:00:00.000, state=CA] (`premium-ca`)");
  });

  it("keeps concise Markdown textual", () => {
    const result = formatResponse(graph, "markdown_concise", {
      dataType: "table_dependencies",
      markdownContext: { scope: "whole project", layer: "datatype" },
    });

    expect(result).not.toContain("```mermaid");
    expect(result).toContain("5 nodes and 4 links");
    expect(result).toContain("2 executable nodes with 1 call link");
    expect(result).toContain("3 datatype/vocabulary nodes with 3 model links");
    expect(result).toContain("Highest executable fan-out: Premium (1 dependency)");
    expect(result).toContain("3 fields, including 2 typed references (1 collection)");
    expect(result).toContain("1 inheritance relation; every data-model node has a model-layer link");
    expect(result).toContain("1 vocabulary with 8 declared values; 1 preview is truncated");
    expect(result).toContain("layer datatype");
  });

  it("escapes untrusted Mermaid labels without exposing executable syntax", () => {
    const unsafeGraph: TableNodeView[] = [
      {
        id: "unsafe",
        name: 'Unsafe"]\n%%{init: {"theme":"dark"}}%%',
        kind: "Datatype",
        tableType: "Datatype",
        dependencies: ["target"],
        fields: [{ name: 'owner:\n"quoted" %%', type: "Target", ref: "target" }],
      },
      {
        id: "target",
        name: "Target",
        kind: "Datatype",
        tableType: "Vocabulary",
        vocabulary: {
          valueType: "String\n}\n%%{init}%%",
          valueCount: 1,
          valuesPreview: ['value"}\n%%{init}%%'],
          truncated: false,
        },
      },
      { id: "unsafe-executable", name: 'Executable"]\n%%{init}%%', kind: "Rules", tableType: "SimpleRules" },
    ];

    const result = formatResponse(unsafeGraph, "markdown", { dataType: "table_dependencies" });

    expect(result).toContain("&quot;");
    expect(result).toContain("Unsafe_init_theme_dark {");
    expect(result).not.toContain("%%{init");
    expect(result).toContain("owner quoted");
    expect(result).toContain('"Target<String_init>" {');
    expect(result).toContain("\u200B value_init");
    expect((result.match(/```/g) ?? []).length % 2).toBe(0);
  });

  it("keeps datatype identifiers unique after sanitizing their names", () => {
    const duplicateNames: TableNodeView[] = [
      { id: "risk-space", name: "Risk Score", kind: "Datatype", tableType: "Datatype" },
      { id: "risk-dash", name: "Risk-Score", kind: "Datatype", tableType: "Datatype" },
    ];

    const result = formatResponse(duplicateNames, "markdown", { dataType: "table_dependencies" });

    expect(result).toMatch(/^  Risk_Score$/m);
    expect(result).toMatch(/^  Risk_Score_2$/m);
    expect(result).toContain("Unconnected datatypes/vocabularies without displayable members: `Risk Score`, `Risk-Score`.");
  });

  it("renders values for unconnected vocabularies that Mermaid would otherwise hide", () => {
    const vocabularies: TableNodeView[] = [
      {
        id: "eligibility",
        name: "EligibilityType",
        kind: "Datatype",
        tableType: "Vocabulary",
        vocabulary: {
          valueType: "String",
          valueCount: 3,
          valuesPreview: ["Not Eligible", "Provisional", "Eligible"],
          truncated: false,
        },
      },
      {
        id: "blank",
        name: "Blank",
        kind: "Datatype",
        tableType: "Vocabulary",
        vocabulary: { valueType: "String", valueCount: 0, truncated: false },
      },
    ];

    const result = formatResponse(vocabularies, "markdown", { dataType: "table_dependencies" });

    expect(result).toContain('"EligibilityType<String>" {');
    expect(result).toContain("\u200B Not_Eligible");
    expect(result).toContain("\u200B Provisional");
    expect(result).toContain("\u200B Eligible");
    expect(result).toMatch(/^  "Blank<String>"$/m);
    expect(result).toContain("Unconnected datatypes/vocabularies without displayable members: `Blank`.");
  });

  it("keeps Mermaid keywords in datatype display labels only", () => {
    const reservedNames: TableNodeView[] = [
      {
        id: "direction",
        name: "direction",
        kind: "Datatype",
        tableType: "Datatype",
        extends: "classDiagram",
        dependencies: ["classDiagram"],
        fields: [{ name: "value", type: "String" }],
      },
      { id: "classDiagram", name: "classDiagram", kind: "Datatype", tableType: "Datatype" },
    ];

    const result = formatResponse(reservedNames, "markdown", { dataType: "table_dependencies" });

    expect(result).toContain("Type_direction {");
    expect(result).toMatch(/^  Type_classDiagram$/m);
    expect(result).toContain("Type_classDiagram <|-- Type_direction");
    expect(result).not.toMatch(/^  direction \{/m);
  });

  it.each(["markdown", "markdown_detailed"] as const)(
    "omits whole nodes instead of cutting a Mermaid block at the character limit in %s",
    (responseFormat) => {
      const largeGraph: TableNodeView[] = Array.from({ length: 40 }, (_, index) => ({
        id: `node-${index}`,
        name: `Executable table with a deliberately long name ${index}`,
        kind: "Rules",
        tableType: "SimpleRules",
        dependencies: index < 39 ? [`node-${index + 1}`] : [],
      }));

      const result = formatResponse(largeGraph, responseFormat, {
        dataType: "table_dependencies",
        characterLimit: 900,
      });

      expect(result.length).toBeLessThanOrEqual(900);
      expect(result).toContain("nodes;");
      expect(result).toContain("omitted to keep the response within the character limit");
      expect((result.match(/```/g) ?? []).length % 2).toBe(0);
      expect(result).toMatch(/```mermaid[\s\S]*```/);
    },
  );

  it("fits a large dependency graph without rebuilding every smaller candidate", () => {
    const largeGraph: TableNodeView[] = Array.from({ length: 5_000 }, (_, index) => ({
      id: `node-${index}`,
      name: `Executable table ${index}`,
      kind: "Rules",
      tableType: "SimpleRules",
      dependencies: index < 4_999 ? [`node-${index + 1}`] : [],
    }));

    const startedAt = performance.now();
    const result = formatResponse(largeGraph, "markdown", { dataType: "table_dependencies" });

    expect(result.length).toBeLessThanOrEqual(RESPONSE_LIMITS.MAX_CHARACTERS);
    expect(result).toContain("omitted to keep the response within the character limit");
    expect(performance.now() - startedAt).toBeLessThan(2_000);
  });
});
