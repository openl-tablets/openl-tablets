import { Spin } from 'antd'
import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css, token }) => ({
    veil: css`
        position: absolute;
        inset: 0;
        z-index: 2;
        display: flex;
        align-items: center;
        justify-content: center;
        background: color-mix(in srgb, ${token.colorBgContainer} 60%, transparent);
    `,
}))

/**
 * The busy state of what it is laid over: dims it, says the wait is running, and takes the clicks that
 * would start a second operation on it.
 *
 * It fills its closest positioned ancestor, so what stays workable is decided by where it is put — over
 * one project on its page, over the list on the projects home. It never covers a dialog: those are
 * portalled above it, and a dialog the user is filling in is not something to wait behind.
 */
export const BusyVeil = ({ 'data-testid': testId }: { 'data-testid': string }) => {
    const { styles } = useStyles()
    return (
        <div className={styles.veil} data-testid={testId}>
            <Spin />
        </div>
    )
}
