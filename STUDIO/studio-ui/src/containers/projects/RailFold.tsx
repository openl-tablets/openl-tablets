import { Button, Tooltip } from 'antd'
import { LeftOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { booleanText, storedSetting, type StoredSetting } from '../../utils/localStore'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    /** The rail put away: a strip carrying the handle that brings it back. */
    folded: css`
        width: 28px;
        min-width: 28px;
        align-items: center;
        padding-top: ${token.paddingXS}px;
    `,
}))

/**
 * Whether a left rail is folded away, kept under the given key, so the rail opens the next time as the reader left it.
 * Open is the default, kept as no key at all.
 */
export const railFolded = (key: string): StoredSetting<boolean> => storedSetting(key, false, booleanText)

interface FoldRailButtonProps {
    onFold: () => void
    /** The test id of the rail; the handle is named with `-collapse` after it. */
    testId: string
}

/** The handle on the header row of a left rail that folds the rail away. */
export const FoldRailButton = ({ onFold, testId }: FoldRailButtonProps) => {
    const { t } = useTranslation()
    return (
        <Tooltip title={t('common:rail.hide')}>
            <Button
                aria-label={t('common:rail.hide')}
                data-testid={`${testId}-collapse`}
                icon={<LeftOutlined />}
                onClick={onFold}
                size="small"
                type="text"
            />
        </Tooltip>
    )
}

interface FoldedRailProps {
    onUnfold: () => void
    /** The test id of the rail; the strip is named with `-collapsed` after it, its handle with `-expand`. */
    testId: string
}

/** A left rail folded away: a narrow strip whose handle brings the rail back. */
export const FoldedRail = ({ onUnfold, testId }: FoldedRailProps) => {
    const { t } = useTranslation()
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    return (
        <aside className={cx(shared.rail, styles.folded)} data-testid={`${testId}-collapsed`}>
            <Tooltip title={t('common:rail.show')}>
                <Button
                    aria-label={t('common:rail.show')}
                    data-testid={`${testId}-expand`}
                    icon={<RightOutlined />}
                    onClick={onUnfold}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </aside>
    )
}
