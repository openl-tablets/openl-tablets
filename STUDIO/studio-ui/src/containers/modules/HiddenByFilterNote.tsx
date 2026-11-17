import { useTranslation } from 'react-i18next'
import { Typography } from 'antd'
import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css }) => ({
    note: css`
        display: block;
    `,
}))

interface HiddenByFilterNoteProps {
    /** How many of the tables found the type filter leaves out. */
    count: number
    /** Lists the tables the type filter leaves out. */
    onShow: () => void
    /** The test id of the note; its link takes the same id ending in `-show`. */
    testId: string
    /** Sets the room around the note, which the place it is shown in decides. */
    className?: string
}

/**
 * Tells how many tables a search found that the type filter hides, with a link that shows them.
 *
 * <p>The note wraps onto as many lines as it needs, so the link stays in sight however narrow the place it is in.
 *
 * @author Yury Molchan
 */
export const HiddenByFilterNote = ({ count, onShow, testId, className }: HiddenByFilterNoteProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    return (
        <Typography.Text className={cx(styles.note, className)} data-testid={testId} type="secondary">
            {t('browser.module.hidden_by_filter', { count })}
            {/* The dash keeps to the text, so a line the note wraps onto starts with the link. */}
            {'\u00a0— '}
            <Typography.Link data-testid={`${testId}-show`} onClick={onShow}>
                {t('browser.module.show_hidden')}
            </Typography.Link>
        </Typography.Text>
    )
}
