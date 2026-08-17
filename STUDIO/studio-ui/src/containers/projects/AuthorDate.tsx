import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css, token }) => ({
    date: css`
        color: ${token.colorTextTertiary};
        white-space: nowrap;
    `,
}))

interface AuthorDateProps {
    /** Who made the change. A dash stands in for an unknown author. */
    author: string | undefined
    /** When it happened, already formatted. Omitted when unknown. */
    date: string | null
}

/**
 * Who changed something and when: the name on its own line, the date muted underneath.
 *
 * Only the colour of the date is the component's own — the type size comes from whatever container it sits
 * in. The two lines are siblings rather than a wrapped block, so the caller's layout decides how they sit.
 */
export const AuthorDate = ({ author, date }: AuthorDateProps) => {
    const { styles } = useStyles()
    return (
        <>
            <div>{author || '—'}</div>
            {date && <div className={styles.date}>{date}</div>}
        </>
    )
}
