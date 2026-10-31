/**
 * Unit tests of src/json-cut.ts: a value cut to a length in its structure, and the cuts it names.
 */

import { describe, it, expect } from "vitest";
import { cutJson } from "./json-cut.js";

describe("cutJson", () => {
  it("leaves a value that fits as it is", () => {
    const value = { a: [1, 2, 3], b: "text" };

    expect(cutJson(value, 1_000)).toEqual({ value, cuts: [] });
  });

  it("leaves a long text of a value that fits whole", () => {
    const value = { label: "x".repeat(500) };

    expect(cutJson(value, 1_000)).toEqual({ value, cuts: [] });
  });

  it("shortens long texts first", () => {
    const { value, cuts } = cutJson({ label: "x".repeat(500), short: "ok" }, 300);

    expect(value).toEqual({ label: `${"x".repeat(200)}…`, short: "ok" });
    expect(cuts).toEqual(["1 text to 200 characters"]);
  });

  it("cuts the largest array in its middle, keeping its first and its last items", () => {
    const frames = Array.from({ length: 10 }, (_, index) => ({ index, name: `frame-${index}` }));
    const { value, cuts } = cutJson({ status: "suspended", frames, small: [1, 2] }, 200);

    const kept = (value as { frames: Array<{ index: number }> }).frames.map((frame) => frame.index);
    expect(kept[0]).toBe(0);
    expect(kept.at(-1)).toBe(9);
    expect(kept.length).toBeLessThan(10);
    expect(JSON.stringify(value).length).toBeLessThanOrEqual(200);
    expect(cuts[0]).toBe(`frames (${10 - kept.length} of 10 items from the middle)`);
    // The smaller array is left whole while the larger one can still be cut
    expect((value as { small: number[] }).small).toEqual([1, 2]);
  });

  it("keeps the last item beside the first when only one would fit", () => {
    const frames = Array.from({ length: 7 }, (_, index) => ({ index, steps: "x".repeat(150) }));

    const { value, cuts } = cutJson({ frames }, 400);

    expect((value as { frames: Array<{ index: number }> }).frames.map((frame) => frame.index)).toEqual([0, 6]);
    expect(cuts).toContain("frames (5 of 7 items from the middle)");
  });

  it("names a cut inside nested arrays by its path", () => {
    const value = { parameters: [{ value: { codes: Array.from({ length: 80 }, (_, i) => `CODE${i}`) } }] };

    const { cuts } = cutJson(value, 300);

    expect(cuts[0]).toMatch(/^parameters\[0\]\.value\.codes \(\d+ of 80 items from the middle\)$/);
  });

  it("never cuts the enum of a schema, but the object that holds it", () => {
    const states = Array.from({ length: 80 }, (_, i) => `STATE${i}`);
    const schema = { properties: { state: { type: "string", enum: states }, name: { type: "string" } } };

    const { value, cuts } = cutJson(schema, 300);

    expect(value).toEqual({ properties: "{2 keys cut}" });
    expect(cuts).toEqual(["properties (2 keys)"]);
  });

  it("replaces the largest object by the count of its keys when no array can be cut", () => {
    const properties = Object.fromEntries(Array.from({ length: 50 }, (_, i) => [`field${i}`, { type: "string" }]));

    const { value, cuts } = cutJson({ name: "Policy", properties }, 100);

    expect(value).toEqual({ name: "Policy", properties: "{50 keys cut}" });
    expect(cuts).toEqual(["properties (50 keys)"]);
  });
});
