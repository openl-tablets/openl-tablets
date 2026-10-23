# Root-cause catalog

Read at step 4 of the workflow: take the section for the issue type, then classify.

## Error or exception

The exception suspends at the throwing frame — read its parameters and step values there.
Read the table itself; never infer the formula.

## Null or blank result

- Find the first frame where the value became null and read the table that produced it.
- Read the **whole** lookup to see whether the input combination has a row.
- Decide: null by design (no match intended) or a gap — MISSING DATA, WRONG ROUTING or
  MISSING RULE.

## Wrong calculation

Find the step with the wrong value, read its formula, and walk back through its factors.
`excludeStepValues: [1]` hides neutral rating factors so the outlier stands out.

## Wrong decision or rule match

Inspect the decision-table frame: which rule fired and how each condition evaluated. Read the
full table, then decide whether the wrong row fired or the right row is missing.

## Correct math, wrong scenario (upstream mapping)

Suspect it when the result is off by a power of ten, the mismatch hits one segment only (child
rate wrong, adult fine), or the JSON looks contradictory to what the user says they entered.
Check the payload for:

- **Dual-encoded fields** — one concept sent two ways (`benefitAmount` and
  `benefitPercentage`); the formula silently picks one branch.
- **Scale errors** — `10` where `0.1` is expected; basis points as a decimal.
- **Phantom fields** — values the user never entered (defaults, stale offer config).
- **Inverted flags or mis-mapped enums** that route to the wrong branch.
- **Effective-date drift** — the date dispatches to a version the user did not expect.

Confirm with the arithmetic identity: the wrong result equals what another input would yield
through another branch (`100/250 = 0.4` equals `0.10 × 1000 / 250` from
`benefitPercentage = 0.10`). Ask for a UI screenshot, the product model, the offer
configuration or the mapping rules — but do not block on them; state mapping as the suspected
layer.

The fix has two owners: the primary fix in the caller's mapping layer, and an optional
defensive guard in the OpenL formula (reject contradictory inputs or document which branch
wins). Name the owning team for each; do not draft their tickets unless asked.
