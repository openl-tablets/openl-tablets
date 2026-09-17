import type { FC } from 'react'
import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from '../styles/listPageTheme'

interface LogoProps {
    width?: number | string
    height?: number | string
}

/**
 * The colours of the mark, taken from the palette in force so the logo turns with the theme and the appearance.
 *
 * The three faces of the cube step from the primary colour to the brand colour; the outline is the brand
 * colour, and the edges between the faces are drawn in the colour that reads on the primary one — light in the
 * light appearance, dark in the dark one.
 */
const useStyles = createStyles(({ css }) => ({
    outline: css`
        stroke: ${LIST_PAGE_COLORS.brand};
    `,
    lit: css`
        fill: ${LIST_PAGE_COLORS.primary};
    `,
    shaded: css`
        fill: color-mix(in srgb, ${LIST_PAGE_COLORS.primary} 55%, ${LIST_PAGE_COLORS.brand});
    `,
    dark: css`
        fill: ${LIST_PAGE_COLORS.brand};
    `,
    edges: css`
        stroke: ${LIST_PAGE_COLORS.primaryFg};
    `,
}))

/** The OpenL cube, drawn in the colours of the theme and appearance in force. */
const Logo: FC<LogoProps> = ({ width = 24, height = 24 }) => {
    const { styles } = useStyles()
    return (
        <svg fill="none" height={height} viewBox="-1066 -1200 2132 2400" width={width} xmlns="http://www.w3.org/2000/svg">
            <path className={styles.outline} d="M0 1000 866 500 866-500 0-1000-866-500-866 500Z" strokeLinejoin="round" strokeWidth="400" />
            <path className={styles.lit} d="M0 1000 l866-500 0-500-866 500-866-500 0 500z" />
            <path className={styles.shaded} d="M0 500 l866-500 0-500-866 500-866-500 0 500z" />
            <path className={styles.dark} d="M0-1000-866-500 0 0 866-500Z" />
            <path
                className={styles.edges}
                d="M0 500 433 250 433-250 0-500-433-250-433 250Z M0 1000 866 500 866-500 0-1000-866-500-866 500Z M0 1000 L0 0-866-500M866-500 0 0"
                strokeLinejoin="round"
                strokeWidth="75"
            />
        </svg>
    )
}

export default Logo
