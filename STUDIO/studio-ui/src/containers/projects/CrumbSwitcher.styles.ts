import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    trigger: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        /* Never wider than the place it is put in: the name truncates instead. */
        max-width: 100%;
        min-width: 0;
        padding: 0 4px;
        border: 0;
        border-radius: ${token.borderRadiusSM}px;
        background: transparent;
        color: inherit;
        font: inherit;
        cursor: pointer;

        &:hover {
            background: ${token.colorFillTertiary};
        }

        /* Blocked because what it switches is busy with something else — nothing to wait for here. */
        &:disabled {
            cursor: not-allowed;
        }

        /* The switch this trigger started is what the wait is for. */
        &[aria-busy='true'] {
            cursor: progress;
        }
    `,
    caret: css`
        color: ${token.colorTextQuaternary};
        font-size: 10px;
    `,
    /** The caret's place while the switch runs, so the trigger itself says something is changing. */
    busy: css`
        color: ${token.colorPrimary};
    `,
    /**
     * The popup reads as one card: a fixed width so a long name is clipped instead of stretching the list
     * across the screen, and the list below the search is the card's own body, not a second card floating
     * under it.
     */
    popup: css`
        min-width: 220px;
        max-width: 320px;
        background: ${token.colorBgElevated};
        border-radius: ${token.borderRadiusLG}px;
        box-shadow: ${token.boxShadowSecondary};
        overflow: hidden;

        .ant-dropdown-menu {
            box-shadow: none;
            background: transparent;
            border-radius: 0;
            padding: 4px 0;
            max-height: 320px;
            overflow-y: auto;
        }

        .ant-dropdown-menu-item {
            max-width: 100%;
        }

        /* A long name is clipped to the fixed width instead of stretching the popup. */
        .ant-dropdown-menu-title-content {
            min-width: 0;
            overflow: hidden;
        }
    `,
    search: css`
        padding: 8px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    empty: css`
        padding: 12px;
        color: ${token.colorTextTertiary};
        text-align: center;
    `,
}))
