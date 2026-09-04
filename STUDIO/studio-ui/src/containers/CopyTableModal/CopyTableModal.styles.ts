import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /** The copy's name sits in the right column, where the create dialog puts the name of the table it writes. */
    tableName: css`
        grid-column: 2;

        @media (max-width: ${token.screenSM}px) {
            grid-column: 1;
        }
    `,
}))
