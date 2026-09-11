import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    // The page fills the window it was opened in; what does not fit scrolls inside the tree and the
    // tables rather than running off the screen.
    page: css`
        box-sizing: border-box;
        display: flex;
        flex-direction: column;
        height: 100vh;
        overflow: hidden;
    `,
    // The drop area asks for the whole height of its box, which in a column layout is the height of
    // its own content - a circle that leaves the box measuring the drop area alone, with the list of
    // picked files hanging out of it. Asking for the height it needs breaks the circle.
    picker: css`
        flex: none;

        .ant-upload-drag {
            height: auto;
        }
    `,
    // The files that were picked, listed under the drop area the way the old screen listed them.
    files: css`
        margin: ${token.marginXS}px 0 0;
        padding: 0;
        list-style: none;
    `,
    file: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px 0;
        color: ${token.colorTextSecondary};
    `,
    fileName: css`
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    // The step where the files are picked; the comparison takes the window on its own terms.
    step: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
        gap: ${token.margin}px;
        padding: ${token.padding}px;
    `,
    // The window says which project is compared, because it opens away from the screen that asked for it.
    pickerTitle: css`
        flex: none;
        font-size: ${token.fontSizeLG}px;
        font-weight: 600;
    `,
    // The two files to compare, each picked on its own side, as the old page had them side by side.
    sides: css`
        display: flex;
        flex: none;
        flex-wrap: wrap;
        gap: ${token.marginLG}px;
    `,
    side: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginXS}px;
        min-width: 320px;
        flex: 1 1 320px;
    `,
    // The comparison: the tree beside the two files, each column headed on the same line.
    result: css`
        flex: 1;
        min-height: 0;
    `,
    column: css`
        display: flex;
        flex-direction: column;
        height: 100%;
        min-width: 0;
    `,
    head: css`
        display: flex;
        flex: none;
        align-items: center;
        gap: ${token.marginSM}px;
        height: 38px;
        padding: 0 ${token.paddingSM}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};

        .ant-btn-link {
            padding-inline: 0;
        }

        // A control left without the room it asks for is shortened rather than wrapped, so that the
        // line keeps the height of the line the files are named on.
        .ant-checkbox-wrapper {
            min-width: 0;

            > span:last-child {
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
            }
        }
    `,
    // The control that hides the list keeps to the right of the line it heads.
    headAction: css`
        margin-left: auto;
    `,
    headLabel: css`
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: ${token.colorTextTertiary};
    `,
    body: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding: ${token.paddingSM}px;
    `,
    // The two files share the room left by the list of elements, and the divider between them gives
    // either file more of it.
    panes: css`
        height: 100%;
    `,
    // Which conflicted file is compared, and what the merge did to it.
    conflictHead: css`
        display: flex;
        flex: none;
        gap: ${token.marginLG}px;
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        color: ${token.colorTextSecondary};
    `,
    // A file that is not a workbook reads line by line, in the colours a diff is read in.
    diff: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding: ${token.paddingXS}px 0;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        line-height: 1.5;
    `,
    diffLine: css`
        display: flex;
        white-space: pre-wrap;
        word-break: break-word;
    `,
    diffNumber: css`
        flex: 0 0 48px;
        padding-right: ${token.paddingXS}px;
        color: ${token.colorTextQuaternary};
        text-align: right;
        user-select: none;
    `,
    diffText: css`
        flex: 1;
        padding-right: ${token.paddingSM}px;
    `,
    // What the signs leading the rows mean, beside the control that picks the view: both are about how
    // the table below is read rather than about a file, so both keep to the end of the line.
    headTail: css`
        display: flex;
        align-items: center;
        margin-left: auto;
        gap: ${token.marginSM}px;
    `,
    legend: css`
        display: flex;
        gap: ${token.marginSM}px;
        color: ${token.colorTextSecondary};
        font-size: ${token.fontSizeSM}px;
        white-space: nowrap;
    `,
    legendSign: css`
        margin-right: ${token.marginXXS}px;
        color: ${token.colorTextTertiary};
    `,
    // The sign a row of the combined view is read by, in a column of its own before the table.
    combinedLead: css`
        width: 1.5em;
        color: ${token.colorTextTertiary};
        text-align: center;
    `,
    // What the first file had, beside what the second one has in its place.
    combinedBefore: css`
        color: ${token.colorTextTertiary};
        text-decoration: line-through;
    `,
    // The cells that read differently in the other file, in the colour the rest of Studio marks a change with.
    changed: css`
        background: ${token.colorWarningBg};
    `,
    added: css`
        color: ${token.colorSuccess};
    `,
    removed: css`
        color: ${token.colorError};
    `,
    add: css`
        background: ${token.colorSuccessBg};
    `,
    remove: css`
        background: ${token.colorErrorBg};
    `,
    changedIcon: css`
        color: ${token.colorWarning};
    `,
    // An element both files read the same is there to be found, not to be looked at.
    equalIcon: css`
        color: ${token.colorTextQuaternary};
    `,
    center: css`
        display: flex;
        height: 100%;
        align-items: center;
        justify-content: center;
    `,
}))
