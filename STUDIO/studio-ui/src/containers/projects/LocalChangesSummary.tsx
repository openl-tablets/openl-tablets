import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from 'antd'
import { DownOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { NormalizedFileChange } from './fileChanges'
import { FileChangeIcon } from './FileChangeIcon'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    root: css`
        flex: none;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    header: css`
        justify-content: flex-start;
        height: auto;
        padding: ${token.paddingXS}px ${token.padding}px;
        color: ${token.colorTextSecondary};

        .ant-btn-icon {
            display: inline-flex;
        }
    `,
    title: css`
        min-width: 0;
    `,
    list: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginXXS}px;
        margin: 0;
        padding: 0 ${token.padding}px ${token.paddingXS}px ${token.padding}px;
        list-style: none;
    `,
    item: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        min-width: 0;
        color: ${token.colorTextSecondary};
        font-size: 12px;
    `,
    path: css`
        flex: 1;
        min-width: 0;
    `,
}))

interface LocalChangesSummaryProps {
    changes: NormalizedFileChange[]
}

export const LocalChangesSummary = ({ changes }: LocalChangesSummaryProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const [open, setOpen] = useState(false)

    if (changes.length === 0) {
        return null
    }

    const ToggleIcon = open ? DownOutlined : RightOutlined

    return (
        <div className={styles.root} data-testid="local-changes">
            <Button
                block
                className={styles.header}
                data-testid="local-changes-toggle"
                icon={<ToggleIcon />}
                onClick={() => setOpen(value => !value)}
                type="text"
            >
                <span className={cx(shared.ellipsis, styles.title)}>{t('browser.files.local_changes', { count: changes.length })}</span>
            </Button>
            {open && (
                <ul className={styles.list}>
                    {changes.map(change => (
                        <li key={change.path} className={styles.item} data-testid={`local-change-${change.type}-${change.path}`}>
                            <FileChangeIcon
                                testId={`local-change-icon-${change.type}-${change.path}`}
                                title={t(`browser.files.change.${change.type}`)}
                                type={change.type}
                            />
                            <span className={cx(shared.mono, shared.ellipsis, styles.path)} title={change.path}>{change.path}</span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}
