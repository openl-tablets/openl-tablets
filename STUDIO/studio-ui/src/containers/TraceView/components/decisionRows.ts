import type { StepValueView } from 'types/trace'

/** A decision-table condition row — matched or unmatched. The returned-rule row is not a condition. */
export const isCondition = (step: StepValueView): boolean =>
    step.decision === 'matched' || step.decision === 'unmatched'

/**
 * The steps to show for a tree node. With the detailed toggle on, the whole decision-table breakdown shows;
 * off, the per-condition rows drop out and only the returned rule and any non-decision steps remain — the
 * plain legacy view shared by the business and advanced trees.
 */
export const displaySteps = (steps: StepValueView[], showDetailed: boolean): StepValueView[] =>
    showDetailed ? steps : steps.filter(step => !isCondition(step))
