import { createStyles } from 'antd-style'
import { MOCKUP } from './projectsTheme'

/**
 * The recurring looks of the Projects tab, as real style hooks rather than snippets pasted into each
 * component: a value label, a pane header, a chip, a state dot, a selectable card.
 *
 * Compose them with the component's own styles and override whatever differs — the later class wins:
 *
 * ```tsx
 * const { styles: shared } = useSharedStyles()
 * const { styles, cx } = useStyles()
 * <span className={cx(shared.chipTag, styles.tag)} />
 * ```
 */
export const useSharedStyles = createStyles(({ css, token }) => ({
    /**
     * The explicit compact (12px) value size, for the few places that opt into it directly (module cells,
     * patterns, source paths, the rail count). `ValueText` itself inherits its context size instead.
     */
    valueText: css`
        font-size: 12px;
        line-height: 18px;
    `,
    /** Single-line clipping. */
    ellipsis: css`
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    /**
     * The small uppercase caption of a section, a column header or a metadata label — the one typographic
     * accent for anything that names a value rather than being one.
     */
    microLabel: css`
        color: ${token.colorTextTertiary};
        font-size: 11px;
        font-weight: 500;
        letter-spacing: 0.05em;
        text-transform: uppercase;
    `,
    /**
     * The header strip of a Files-tab pane. One rule keeps the tree toolbar and the file header at the
     * same height, and with them their bottom borders and their buttons, whatever controls each carries.
     */
    paneHeader: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-height: 56px;
        padding: 12px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /** A column that fills its parent and clips instead of stretching it — the body of a pane. */
    paneColumn: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
    `,
    /** The centred "nothing here yet" body of a pane, with its oversized icon. */
    panePlaceholder: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        text-align: center;
        color: ${token.colorTextTertiary};

        .anticon {
            font-size: 32px;
            color: ${token.colorTextQuaternary};
        }
    `,
    /** A compact tag: no margin, chip-sized text, so several fit on one row without stretching it. */
    chipTag: css`
        margin: 0;
        font-size: 11px;
        line-height: 18px;
    `,
    /**
     * A full-width compact pair — a text control with the button hanging off its end. The control
     * shrinks with the form and scrolls its long value inside itself; without this a value-sized
     * control (an AutoComplete measures its text) pushes the button past the edge of the dialog.
     */
    compactField: css`
        display: flex;
        width: 100%;
        min-width: 0;

        > :first-child {
            flex: 1 1 auto;
            min-width: 0;
        }

        > :last-child:not(:first-child) {
            flex: none;
        }
    `,
    /** A round state dot (compile state, deployment state); the caller fills it with the state colour. */
    stateDot: css`
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex: none;
    `,
    /** A dashed box standing in for a list that has nothing in it yet. */
    dashedEmpty: css`
        padding: 24px;
        border: 1px dashed ${token.colorBorder};
        border-radius: ${token.borderRadiusLG}px;
        text-align: center;
        color: ${token.colorTextTertiary};
    `,
    /** The frame of a browse screen: a fixed rail on the left, the list filling the rest. */
    page: css`
        display: flex;
        height: calc(100vh - 64px);
        overflow: hidden;
        background: ${token.colorBgLayout};
    `,
    /** The left rail carrying the facets of the list — repositories, statuses, tags. */
    rail: css`
        display: flex;
        flex-direction: column;
        width: 256px;
        flex: none;
        border-right: 1px solid ${token.colorBorderSecondary};
        background: ${MOCKUP.sidebarBg};
        overflow: hidden;
    `,
    /** The rail's own header, above the scrolling facets. */
    railHead: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px;
        font-size: 14px;
        font-weight: 600;
    `,
    /** The scrolling body of the rail. */
    railScroll: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding-bottom: 12px;
    `,
    /** One selectable line of the rail: a facet value, a repository. */
    railRow: css`
        display: flex;
        align-items: center;
        gap: 10px;
        width: 100%;
        margin: 0;
        padding: 4px 6px;
        border: none;
        border-radius: ${token.borderRadiusSM}px;
        background: transparent;
        cursor: pointer;
        /* A native <button> does not inherit the font family; without this its text drops to the
           browser's control font, since the app loads no CSS reset. */
        font-family: inherit;
        font-size: 14px;
        text-align: left;

        &:hover {
            background: ${MOCKUP.accent};
        }
    `,
    /** The picked line of the rail. */
    railRowActive: css`
        background: ${MOCKUP.accent};
        color: ${MOCKUP.accentFg};
    `,
    /** Everything to the right of the rail. */
    main: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;
    `,
    /** The screen's header band: title, summary and the actions of the whole screen. */
    header: css`
        padding: 12px 16px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    /** The title row of the header, with the screen actions pushed to the far side. */
    headTop: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
    `,
    /** The screen title. */
    pageTitle: css`
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        letter-spacing: -0.02em;
    `,
    /** The line under the title that says what the list currently holds. */
    subtitle: css`
        margin-top: 4px;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    /** The actions of the whole screen, at the end of the title row. */
    headActions: css`
        display: flex;
        align-items: center;
        gap: 8px;
        flex: none;
    `,
    /** The scrolling area the list itself lives in. */
    content: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
    /** The padded box an empty or failed list is shown in. */
    stateBox: css`
        margin: 24px;
        padding: 48px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${token.colorBgContainer};
    `,
    /** The padding a skeleton is shown with while the list loads. */
    loading: css`
        padding: 24px;
    `,
    /** A list table: white rows on the page tint, framed like a card. */
    listTable: css`
        width: calc(100% - 32px);
        border-collapse: separate;
        border-spacing: 0;
        margin: 16px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${token.colorBgContainer};
        font-size: 14px;
        overflow: hidden;
    `,
    /** The header row of a list table. */
    listHead: css`
        background: ${token.colorBgContainer};

        th {
            padding: 8px 12px;
            border-bottom: 1px solid ${token.colorBorderSecondary};
            text-align: left;
            white-space: nowrap;
        }

        th:first-of-type {
            padding-left: 16px;
        }
    `,
    /** One row of a list table. */
    listRow: css`
        background: ${token.colorBgContainer};

        td {
            padding: 12px;
            border-bottom: 1px solid ${token.colorBorderSecondary};
            vertical-align: middle;
        }

        td:first-of-type {
            padding-left: 16px;
        }

        &:hover td {
            background: ${token.colorFillQuaternary};
        }

        &:last-of-type td {
            border-bottom: none;
        }
    `,

    /** A card the user picks from a set — a creation method, an access-subject kind. */
    selectableCard: css`
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadiusLG}px;
        cursor: pointer;
        transition: border-color 0.15s ease, background 0.15s ease;

        &:hover {
            border-color: ${token.colorPrimaryBorder};
            background: ${token.colorFillQuaternary};
        }
    `,
    /** The picked state of {@link selectableCard}, applied next to it. */
    selectedCard: css`
        border-color: ${token.colorPrimary};
        background: ${token.colorPrimaryBg};
    `,
    /** The grip a draggable row or group is picked up by; the pointer turns to a grab hand over it. */
    dragHandle: css`
        flex: none;
        display: inline-flex;
        align-items: center;
        color: ${token.colorTextQuaternary};
        cursor: grab;

        &:active {
            cursor: grabbing;
        }
    `,
    /** A row faded while it is being dragged. */
    dragging: css`
        opacity: 0.6;
    `,
    /** A section heading that folds its section: the whole line is a borderless button. */
    sectionToggle: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        flex: 1;
        min-width: 0;
        padding: 0;
        border: none;
        background: transparent;
        color: inherit;
        cursor: pointer;
        font: inherit;
        letter-spacing: inherit;
        text-align: left;
        text-transform: inherit;
    `,
}))
