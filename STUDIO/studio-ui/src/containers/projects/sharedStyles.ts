import { createStyles } from 'antd-style'
import { useListPageStyles } from '../../styles/listPageStyles'
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
const useProjectSharedStyles = createStyles(({ css, token }) => ({
    /** A plain list read the way exposed methods are: values on their own lines, a line down the left. */
    linedList: css`
        list-style: none;
        margin: 0;
        padding: 0 0 0 12px;
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
        border-left: 2px solid ${token.colorBorderSecondary};
    `,
    linedItem: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        word-break: break-word;
    `,
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
    /**
     * The frame of a workspace screen: a head across the top and, under it, everything the screen shows.
     *
     * The project, a module opened from it and a deployment wear the same frame, so the height under the
     * application header is reckoned in one place rather than in each of them.
     */
    workspacePage: css`
        display: flex;
        flex-direction: column;
        height: calc(100vh - 64px);
        overflow: hidden;
        background: ${token.colorBgContainer};
    `,
    /** What a workspace shows under its head: a rail on the left, the screen filling the rest. */
    workspaceBody: css`
        display: flex;
        flex: 1;
        min-width: 0;
        min-height: 0;
    `,
    /**
     * A tree read in a rail: rows one line high, a small indent per step, and a name read in full.
     *
     * The rail is narrow, so every step of the hierarchy costs width; a name is never wrapped or clipped —
     * a table or a project is recognised by its full name, and the rail scrolls sideways instead.
     */
    railTree: css`
        background: transparent;

        .ant-tree-treenode {
            padding-bottom: 0;
            white-space: nowrap;
            align-items: center;
        }

        .ant-tree-indent-unit {
            width: 12px;
        }

        .ant-tree-switcher {
            width: 18px;
            line-height: 24px;
        }

        .ant-tree-node-content-wrapper {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            min-height: 24px;
            line-height: 24px;
            padding: 0 4px;
            overflow: visible;
        }

        .ant-tree-title,
        .ant-tree-node-content-wrapper .ant-tree-title {
            overflow: visible;
            text-overflow: clip;
            white-space: nowrap;
        }

        .ant-tree-iconEle {
            width: auto;
            line-height: 24px;
        }
    `,
    /** Everything to the right of the rail. */
    main: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;
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

/** Project-specific primitives plus the common list-screen structure used by Projects and Deployments. */
export const useSharedStyles = () => {
    const common = useListPageStyles()
    const project = useProjectSharedStyles()
    return {
        ...project,
        styles: { ...common.styles, ...project.styles },
    }
}
