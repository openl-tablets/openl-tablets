import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    facts: css`
        display: grid;
        grid-template-columns: max-content 1fr;
        gap: ${token.marginXXS}px ${token.marginSM}px;
        margin: 0 0 ${token.margin}px;

        dt {
            color: ${token.colorTextSecondary};
        }

        dd {
            margin: 0;
        }
    `,
    side: css`
        margin-top: ${token.marginXS}px;

        /* The title of the side stays in sight while its libraries scroll under it. */
        summary {
            position: sticky;
            top: 0;
            z-index: 1;
            background: ${token.colorBgElevated};
            cursor: pointer;
            font-weight: ${token.fontWeightStrong};
        }
    `,
    list: css`
        margin: ${token.marginXS}px 0 0;
        padding: 0;
        list-style: none;

        /*
         * The license takes the width it needs but leaves the name 16em, or half of a narrow row, so a long one never
         * squeezes the name into a column of letters.
         */
        li {
            display: grid;
            grid-template-columns: minmax(0, 1fr) fit-content(max(50%, 100% - 16em));
            gap: ${token.marginSM}px;
            overflow-wrap: anywhere;
        }
    `,
    /* A license breaks between the licenses it names, never inside an identifier. */
    licenses: css`
        text-align: end;

        a,
        button span {
            white-space: nowrap;
        }

        a {
            color: ${token.colorLink};

            &:hover {
                color: ${token.colorLinkHover};
            }
        }
    `,
    license: css`
        padding: 0;
        border: 0;
        background: none;
        color: ${token.colorLink};
        font: inherit;
        cursor: pointer;

        &:hover {
            color: ${token.colorLinkHover};
        }
    `,
}))
