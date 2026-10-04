import { createStyles } from 'antd-style'
import { paperToken } from '../../styles/paper'

/** The height of the application header the viewer sits under. */
const HEADER_HEIGHT = 48.5

export const useStyles = createStyles(({ css, token }) => ({
    /** The frame of the viewer: the contents rail on the left, the page filling the rest. */
    frame: css`
        display: flex;
        height: calc(100vh - ${HEADER_HEIGHT}px);
        overflow: hidden;
        background: ${token.colorBgContainer};
    `,
    /** The table of contents, dragged wider or narrower by its right edge, which the grip is laid along. */
    rail: css`
        display: flex;
        flex-direction: column;
        flex: none;
        position: relative;
        border-right: 1px solid ${token.colorSplit};
        background: ${token.colorBgLayout};
    `,
    /** The search above the table of contents. */
    search: css`
        display: flex;
        flex-direction: column;
        gap: ${token.paddingXS}px;
        padding: ${token.paddingSM}px ${token.paddingSM}px ${token.paddingXS}px;
    `,
    scope: css`
        align-self: flex-start;
        max-width: 100%;
    `,
    /** Where the table of contents stays while a search shows its results, so the folders opened stay open. */
    railSlot: css`
        display: flex;
        flex: 1;
        flex-direction: column;
        min-height: 0;

        &[hidden] {
            display: none;
        }
    `,
    results: css`
        flex: 1;
        min-height: 0;
        margin: 0;
        padding: 0 0 ${token.paddingXS}px;
        overflow-y: auto;
        list-style: none;
    `,
    result: css`
        display: flex;
        flex-direction: column;
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        color: ${token.colorText};

        &:hover {
            background: ${token.colorFillTertiary};
            color: ${token.colorText};
        }
    `,
    resultTitle: css`
        font-weight: 500;
    `,
    resultPage: css`
        color: ${token.colorTextSecondary};
        font-size: ${token.fontSizeSM}px;
    `,
    snippet: css`
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSizeSM}px;

        mark {
            padding: 0;
            background: ${token.colorWarningBg};
            color: ${token.colorText};
        }
    `,
    railBody: css`
        flex: 1;
        min-height: 0;
        padding: ${token.paddingXS}px 0;
    `,
    tree: css`
        background: transparent;

        .ant-tree-node-content-wrapper {
            min-width: 0;
            overflow: hidden;
        }

        .ant-tree-title {
            display: block;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
    `,
    /** The scrolling part: the page and, beside it, the headings of the page. */
    content: css`
        flex: 1;
        min-width: 0;
        overflow: auto;
    `,
    page: css`
        display: flex;
        gap: ${token.paddingLG}px;
        max-width: 1280px;
        padding: ${token.paddingLG}px ${token.paddingXL}px;
    `,
    article: css`
        flex: 1;
        min-width: 0;
        max-width: 960px;
    `,
    /** The headings of the page, staying in sight while the page scrolls. */
    outline: css`
        position: sticky;
        top: 0;
        flex: none;
        align-self: flex-start;
        width: 220px;
        max-height: calc(100vh - ${HEADER_HEIGHT + 2 * token.paddingLG}px);
        overflow-y: auto;

        @media (max-width: 1100px) {
            display: none;
        }
    `,
    state: css`
        padding: ${token.paddingLG}px;
    `,
    note: css`
        margin: 0 0 1em;

        p:last-child {
            margin-bottom: 0;
        }
    `,
    diagram: css`
        margin: 0 0 1em;
        overflow-x: auto;
        text-align: center;
    `,
    /** The text of a page, laid out the way a reader of the documentation site sees it. */
    markdown: css`
        color: ${token.colorText};
        font-size: ${token.fontSize}px;
        line-height: 1.7;
        overflow-wrap: break-word;

        h1, h2, h3, h4, h5, h6 {
            margin: 1.5em 0 0.6em;
            color: ${token.colorTextHeading};
            font-weight: 600;
            line-height: 1.3;
            scroll-margin-top: ${token.padding}px;
        }

        h1 {
            font-size: ${token.fontSizeHeading2}px;
        }

        h2 {
            font-size: ${token.fontSizeHeading3}px;
        }

        h3 {
            font-size: ${token.fontSizeHeading4}px;
        }

        h4, h5, h6 {
            font-size: ${token.fontSizeHeading5}px;
        }

        > :first-child {
            margin-top: 0;
        }

        p, ul, ol, pre, blockquote, dl {
            margin: 0 0 1em;
        }

        li > p {
            margin-bottom: 0.5em;
        }

        a {
            color: ${token.colorLink};
        }

        a:hover {
            color: ${token.colorLinkHover};
        }

        img {
            max-width: 100%;
            height: auto;
        }

        hr {
            margin: 1.5em 0;
            border: none;
            border-top: 1px solid ${token.colorSplit};
        }

        blockquote {
            padding: 0 1em;
            border-left: 4px solid ${token.colorBorder};
            color: ${token.colorTextSecondary};
        }

        code {
            padding: 0.1em 0.4em;
            border-radius: ${token.borderRadiusSM}px;
            background: ${token.colorFillTertiary};
            font-family: ${token.fontFamilyCode};
            font-size: 0.9em;
        }

        pre {
            padding: ${token.paddingSM}px ${token.padding}px;
            overflow-x: auto;
            border: 1px solid ${token.colorBorderSecondary};
            border-radius: ${token.borderRadius}px;
            background: ${token.colorFillQuaternary};
        }

        pre code {
            padding: 0;
            background: none;
            font-size: ${token.fontSizeSM}px;
            line-height: 1.5;
        }

        .contains-task-list {
            padding-left: 1em;
            list-style: none;
        }
    `,
    tableScroll: css`
        margin: 0 0 1em;
        overflow-x: auto;
    `,
    /** A pipe table or a `csv` block; an `openl` block is drawn by the table editor's own grid instead. */
    table: css`
        border-collapse: collapse;

        th, td {
            padding: ${token.paddingXXS}px ${token.paddingSM}px;
            border: 1px solid ${token.colorBorderSecondary};
            text-align: left;
            vertical-align: top;
        }

        th {
            background: ${token.colorFillAlter};
            font-weight: 600;
        }
    `,
    /** A header cell of an `openl` block: the table header and the column headers, shaded on the paper. */
    openlHeader: css`
        background: ${paperToken().colorFillSecondary};
        font-weight: 600;
    `,
}))
