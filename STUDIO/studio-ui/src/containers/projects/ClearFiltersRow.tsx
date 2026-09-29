import { Button, Tooltip } from 'antd'
import { ClearOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'

const useStyles = createStyles(({ css }) => ({
    /**
     * A row of its own under the header of the view, so the button fits however narrow the rail is dragged. The
     * icon of the button lines up with the title above it.
     */
    row: css`
        padding: 0 9px 8px;
    `,
}))

interface ClearFiltersRowProps {
    onClick: () => void
    /** What the button tells about the view it heads, shown on hover. */
    hint?: string | undefined
    'data-testid': string
}

/**
 * Clears the values picked in the Filters view. Both views of the rail wear it under their header, in the same
 * place, only while a value is picked, so seeing it also tells that the view is filtered.
 */
export const ClearFiltersRow = ({ onClick, hint, 'data-testid': testId }: ClearFiltersRowProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const button = (
        <Button data-testid={testId} icon={<ClearOutlined />} onClick={onClick} size="small" type="link">
            {t('home.clear_filters')}
        </Button>
    )
    return <div className={styles.row}>{hint ? <Tooltip title={hint}>{button}</Tooltip> : button}</div>
}
