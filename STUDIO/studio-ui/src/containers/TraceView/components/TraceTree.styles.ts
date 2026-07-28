import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    tree: css`
        display: flex;
        flex-direction: column;
        overflow: auto;
    `,
    header: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: ${token.colorTextTertiary};
    `,
    // A quiet caption warning the tree is capped, so its partial branches aren't mistaken for the whole run.
    // Pinned to the top of the scroll area, so the warning stays visible while drilling deep into the tree.
    truncated: css`
        position: sticky;
        top: 0;
        z-index: 1;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorWarningText};
        background: ${token.colorWarningBg};
    `,
    // A per-node caption for sub-calls dropped once the tree hit its size limit, tinted like the truncation
    // warning so the gap reads as "capped here", distinct from a plain "+N more" that can still be paged in.
    notRetained: css`
        color: ${token.colorWarningText};
        font-style: italic;
    `,
    // A clickable "+N more" that pages in the next executions of a lazily-loaded branch.
    moreLink: css`
        cursor: pointer;
        color: ${token.colorLink};
        &:hover {
            text-decoration: underline;
        }
    `,
    // The header's right-hand controls (detailed toggle + Total/Self) sit together, away from the title.
    headerControls: css`
        display: flex;
        align-items: center;
        gap: ${token.marginSM}px;
    `,
    // The Total/Self switch is a control, not a heading — reset the heading typography.
    timeToggle: css`
        text-transform: none;
        font-weight: normal;
        letter-spacing: normal;
    `,
    // The "Show detailed trace" checkbox is a control, not a heading — reset the heading typography.
    detailedToggle: css`
        text-transform: none;
        font-weight: normal;
        letter-spacing: normal;
        white-space: nowrap;
    `,
    // Centered progress note while the simple mode runs the calculation and downloads its tree.
    progress: css`
        display: flex;
        align-items: center;
        justify-content: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingLG}px;
        color: ${token.colorTextSecondary};
    `,
    row: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        cursor: pointer;
        white-space: nowrap;
        border-left: 2px solid transparent;
        &:hover {
            background: ${token.colorFillTertiary};
        }
    `,
    frame: css`
        font-weight: 600;
    `,
    current: css`
        border-left-color: ${token.colorPrimary};
    `,
    selected: css`
        background: ${token.colorFillSecondary};
    `,
    runnable: css`
        cursor: pointer;
        &:hover {
            background: ${token.colorPrimaryBg};
        }
    `,
    // Auxiliary rows (step references, "+N more", loading, capped-branch notes): quieter than the
    // rules themselves, so the tree's structure stays the loudest thing on screen.
    inactive: css`
        opacity: 0.6;
    `,
    // The line the engine is on right now.
    currentStep: css`
        font-weight: 600;
        background: ${token.colorWarningBg};
    `,
    // Not yet reached: a click runs execution here.
    pending: css`
        color: ${token.colorTextTertiary};
    `,
    // Expand/collapse control for an executed branch.
    chevron: css`
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 14px;
        flex: 0 0 auto;
        color: ${token.colorTextTertiary};
        cursor: pointer;
        &:hover {
            color: ${token.colorText};
        }
    `,
    // Keeps labels aligned on rows that have no chevron.
    chevronSlot: css`
        width: 14px;
        flex: 0 0 auto;
    `,
    name: css`
        overflow: hidden;
        text-overflow: ellipsis;
    `,
    // Executed-tree node header: a returned table, kept readable so its name and timing stand out.
    callNode: css`
        color: ${token.colorTextSecondary};
    `,
    // Frame kind (spreadsheet, decision, …) as a quiet eyebrow, not a boxed Tag, so it does not
    // compete with the table name on every row.
    kind: css`
        flex: 0 0 auto;
        font-size: ${token.fontSizeSM}px;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: ${token.colorTextTertiary};
    `,
    // Execution-instance badge: which pass of a looped table this frame is, so stepping through a loop
    // visibly advances (#2, #3, …) instead of repeating the same name.
    pass: css`
        flex: 0 0 auto;
        padding: 0 ${token.paddingXXS}px;
        border-radius: ${token.borderRadiusSM}px;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
        background: ${token.colorFillQuaternary};
    `,
    // Dispatch badge: the table is one of several versions overloaded by dimension properties; hovering
    // lists the versions with the chosen one marked.
    dispatchTag: css`
        display: inline-flex;
        align-items: center;
        gap: 2px;
        flex: 0 0 auto;
        padding: 0 ${token.paddingXXS}px;
        border-radius: ${token.borderRadiusSM}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
        background: ${token.colorFillQuaternary};
        cursor: help;
    `,
    dispatchTipTitle: css`
        font-weight: 600;
        margin-bottom: ${token.marginXXS}px;
    `,
    dispatchCandidate: css`
        opacity: 0.75;
    `,
    dispatchChosen: css`
        opacity: 1;
        font-weight: 600;
        &::before {
            content: '✓ ';
        }
    `,
    // Real execution time. A length-based heat bar keeps the timing channel separate from the status
    // colours (amber/red already mean current/error), so a slow call reads by bar length, not by hue.
    duration: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        margin-left: auto;
        flex: 0 0 auto;
    `,
    durationBar: css`
        width: 56px;
        height: 4px;
        flex: 0 0 auto;
        border-radius: ${token.borderRadiusXS}px;
        background: ${token.colorFillTertiary};
        overflow: hidden;
    `,
    durationFill: css`
        display: block;
        height: 100%;
        min-width: 1px;
        border-radius: ${token.borderRadiusXS}px;
        background: ${token.colorTextTertiary};
    `,
    durationValue: css`
        min-width: 46px;
        text-align: right;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    // Re-run the trace and stop at this returned table to inspect it live.
    replay: css`
        flex: 0 0 auto;
        color: ${token.colorTextTertiary};
        cursor: pointer;
        &:hover {
            color: ${token.colorPrimary};
        }
    `,
    // Inherits the row colour, so a greyed (not-yet-reached) row greys its label too.
    leafLabel: css`
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
    `,
    // Plain, self-explanatory state marks instead of coloured dots: an arrow = where the calculation
    // is now (muted on the callers leading to it), a cross = it failed. An executed line reads as
    // plain text and a not-yet-reached line is greyed, so neither needs a mark.
    mark: css`
        width: 12px;
        flex: 0 0 auto;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: ${token.fontSizeSM}px;
    `,
    markCurrent: css`
        color: ${token.colorWarning};
    `,
    markFrame: css`
        color: ${token.colorPrimary};
    `,
    markWaiting: css`
        color: ${token.colorTextQuaternary};
    `,
    markError: css`
        color: ${token.colorError};
    `,
    // A decision table's evaluated conditions: green check when matched, red cross when not — the legacy look.
    condMatched: css`
        color: ${token.colorSuccess};
    `,
    condUnmatched: css`
        color: ${token.colorError};
    `,
    // A condition is an info row: the ✓/✗ mark is the signal and must stay full-strength, so the row is not
    // dimmed with opacity (which would wash out the mark). The label is muted instead, and the row carries no
    // click affordance since a condition runs nothing.
    conditionRow: css`
        cursor: default;
        &:hover {
            background: transparent;
        }
    `,
    condLabel: css`
        color: ${token.colorTextSecondary};
    `,
    // Step-reference marker: the formula used a step computed elsewhere; the row links to the original.
    refIcon: css`
        width: 8px;
        flex: 0 0 auto;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    // Short attention pulse on the original step row after a reference jump.
    flashed: css`
        animation: trace-ref-flash 1.6s ease-out;
        @keyframes trace-ref-flash {
            0%, 60% {
                background: ${token.colorPrimaryBg};
            }
            100% {
                background: transparent;
            }
        }
    `,
}))
