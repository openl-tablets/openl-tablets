# Dead-code sweep ledger — openl-tablets

## Resume point

- Reinitialised from zero on the owner's instruction: no prior finding, keep-list entry or exhausted vein is carried
  over. Every change type is re-derived from the current `origin/main`.
- Sweep in progress on `origin/main` 737e6794be; nothing below this line is settled until a run writes it.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | pending |
| 2 | Never-read assignments, dead stores | pending |
| 3 | Unused locals, private fields/methods/params | pending |
| 4 | Unused Maven dependency declarations | pending |
| 5 | Pom metadata: managed entries, exclusions, plugin config, properties | pending |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | pending |
| 7 | Unreferenced resources (descriptors, config files, images) | pending |
| 8 | CSS rules and inline styles | pending |
| 9 | Legacy JS functions and pages | pending |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | pending |
| 11 | TypeScript exports, types, components, imports | pending |
| 12 | Test fixtures: workbooks, utility classes, stub members | pending |
| 13 | Package-private/protected members and unreferenced internal classes | pending |

## Open PR

- None.

## Merged PRs

- None since the reset.

## Module coverage

- Nothing swept yet.

## Deferred findings

- None yet.

## False-positive shapes

- None yet.

## Method rules

- None yet.

## Keep-list

- None yet.

## CI flakes

- None yet.

## Container facts

- None yet.

## Exhausted veins

- None yet.

## Human follow-ups

- None yet.

## Run log

- 2026-09-16 g: ledger reset to zero on the owner's instruction; full re-sweep started from `origin/main` 737e6794be.
