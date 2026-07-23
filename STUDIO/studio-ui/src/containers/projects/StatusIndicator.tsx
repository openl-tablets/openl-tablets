import { useTranslation } from 'react-i18next'
import { Tooltip } from 'antd'
import { createStyles, useTheme } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'

const useStyles = createStyles(({ css, token }) => ({
    mark: css`
        /* Fixed size and no growing: the icon sits on the name row whatever the heading size around it. */
        flex: none;
        display: inline-flex;
        align-items: center;
        font-size: 16px;
        line-height: 1;
    `,
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

interface StatusMarkProps {
    status: ProjectStatus
    testId?: string
}

/**
 * The status of a project as a single icon in front of its name, explained by a tooltip.
 *
 * Only the states a user has to act on are marked — unsaved changes and an older revision. A project that
 * is merely open, or closed, carries no mark: that is how projects normally are, and the full status is
 * spelled out on the Overview tab.
 */
export const StatusMark = ({ status, testId }: StatusMarkProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const token = useTheme()
    const meta = STATUS_META[status]
    // A status the user has to act on is the one that carries a hint.
    if (!meta.hintKey) {
        return null
    }
    const Icon = meta.icon
    return (
        <Tooltip title={t(meta.hintKey)}>
            <Icon
                aria-label={t(meta.labelKey)}
                className={styles.mark}
                data-testid={testId}
                style={{ color: token[meta.tokenColor] }}
            />
        </Tooltip>
    )
}
