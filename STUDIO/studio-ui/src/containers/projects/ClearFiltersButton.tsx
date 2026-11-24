import { Button, Tooltip } from 'antd'
import { ClearOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css }) => ({
    /**
     * Leads the header row of the view, its icon lined up with the content below. On a rail dragged narrow, the
     * label gives way to the actions of the row and ends in an ellipsis.
     */
    button: css`
        min-width: 0;
        padding-inline: 0;
    `,
}))

interface ClearFiltersButtonProps {
    onClick: () => void
    /** What the button tells about the view it heads, shown on hover. */
    hint?: string | undefined
    'data-testid': string
}

/**
 * Clears the values picked in the Filters view. Both views of the rail wear it at the start of their header row, in
 * the same place, only while a value is picked, so seeing it also tells that the view is filtered.
 */
export const ClearFiltersButton = ({ onClick, hint, 'data-testid': testId }: ClearFiltersButtonProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { styles: shared } = useSharedStyles()
    const button = (
        <Button
            className={styles.button}
            data-testid={testId}
            icon={<ClearOutlined />}
            onClick={onClick}
            size="small"
            type="link"
        >
            <span className={shared.ellipsis}>{t('home.clear_filters')}</span>
        </Button>
    )
    return hint ? <Tooltip title={hint}>{button}</Tooltip> : button
}
