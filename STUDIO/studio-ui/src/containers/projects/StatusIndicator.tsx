import { useTranslation } from 'react-i18next'
import { createStyles, useTheme } from 'antd-style'
import type { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'

const useStyles = createStyles(({ css, token }) => ({
    pill: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 1px 8px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorFillQuaternary};
        color: ${token.colorText};
        font-size: 12px;
        font-weight: 500;
        line-height: 20px;
        white-space: nowrap;

        .anticon {
            font-size: 13px;
        }
    `,
}))

interface StatusPillProps {
    status: ProjectStatus
    testId?: string
}

/**
 * Labelled status pill for the detail header. The label stays on the calm default tag ink for
 * legibility on every status; the leading icon carries the status hue.
 */
export const StatusPill = ({ status, testId }: StatusPillProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const token = useTheme()
    const meta = STATUS_META[status]
    const Icon = meta.icon
    return (
        <span className={styles.pill} data-testid={testId}>
            <Icon aria-hidden style={{ color: token[meta.tokenColor] }} />
            {t(meta.labelKey)}
        </span>
    )
}
