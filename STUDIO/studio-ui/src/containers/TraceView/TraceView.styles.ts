import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    view: css`
        position: absolute;
        inset: 0;
        display: flex;
        background: #fff;
    `,
    resizing: css`
        cursor: ew-resize;
        user-select: none;
    `,
    viewError: css`
        display: flex;
        align-items: center;
        justify-content: center;
    `,
    leftPanel: css`
        min-width: 200px;
        max-width: 70%;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        border-right: 1px solid #e8e8e8;
    `,
    resizer: css`
        width: 5px;
        cursor: ew-resize;
        background: #f0f0f0;
        flex-shrink: 0;
        transition: background 0.2s;

        &:hover {
            background: #d9d9d9;
        }
    `,
    rightPanel: css`
        min-width: 200px;
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        padding: 16px;
    `,
    panelDisabled: css`
        pointer-events: none;
    `,
    errorBanner: css`
        position: absolute;
        top: 8px;
        left: 50%;
        transform: translateX(-50%);
        z-index: 100;
        max-width: 80%;
    `,
}))
