import { Button, Tooltip } from 'antd'
import { LeftOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { booleanText, storedSetting, type StoredSetting } from '../../utils/localStore'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    /** The rail put away: a strip as narrow as its handle. */
    folded: css`
        width: 28px;
        min-width: 28px;
    `,
    /**
     * The strip is one button from top to bottom, like the spine of a book: the arrow that opens the rail, and under it
     * the name of the rail written up the strip, so the reader sees what is put away and that it opens.
     */
    spine: css`
        display: flex;
        flex: 1;
        flex-direction: column;
        align-items: center;
        gap: ${token.marginXS}px;
        width: 100%;
        min-height: 0;
        margin: 0;
        padding: ${token.paddingXS}px 0;
        border: 0;
        background: transparent;
        color: ${token.colorTextSecondary};
        cursor: pointer;
        /* A native <button> does not inherit the font of the page. */
        font-family: inherit;
        font-size: ${token.fontSize}px;

        &:hover {
            background: ${token.colorFillTertiary};
            color: ${token.colorText};
        }

        &:focus-visible {
            outline: 2px solid ${token.colorPrimaryBorder};
            outline-offset: -2px;
        }
    `,
    /** The arrow, where the handle of the open rail stands. */
    arrow: css`
        display: flex;
        flex: none;
        align-items: center;
        height: ${token.controlHeightSM}px;
        font-size: ${token.fontSizeSM}px;
    `,
    /** The name, read from the bottom up, as on the spine of a book; a strip shorter than the name cuts it. */
    name: css`
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        writing-mode: vertical-rl;
        transform: rotate(180deg);
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
    /** The name of the rail, written up the strip: what the reader brings back by clicking it. */
    name: string
    onUnfold: () => void
    /** The test id of the rail; the strip is named with `-collapsed` after it, its handle with `-expand`. */
    testId: string
}

/** A left rail folded away: a narrow strip with the name of the rail, which brings the rail back on a click. */
export const FoldedRail = ({ name, onUnfold, testId }: FoldedRailProps) => {
    const { t } = useTranslation()
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    return (
        <aside className={cx(shared.rail, styles.folded)} data-testid={`${testId}-collapsed`}>
            <Tooltip placement="right" title={t('common:rail.show')}>
                <button
                    aria-label={t('common:rail.show_named', { name })}
                    className={styles.spine}
                    data-testid={`${testId}-expand`}
                    onClick={onUnfold}
                    type="button"
                >
                    <span className={styles.arrow}><RightOutlined /></span>
                    <span className={styles.name}>{name}</span>
                </button>
            </Tooltip>
        </aside>
    )
}
