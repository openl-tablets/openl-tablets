import { Spin } from 'antd'
import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css }) => ({
    /** Takes the place, and the padding, the list itself would have taken. */
    box: css`
        padding: 12px;
        text-align: center;
    `,
}))

interface ListLoadingProps {
    testId?: string | undefined
}

/**
 * What is being read, shown where what is read belongs.
 *
 * <p>A heavy workspace takes seconds to list its branches, its projects or its modules, and an empty list in
 * the meantime reads as nothing to choose from. Every list read on opening says so the same way.
 */
export const ListLoading = ({ testId }: ListLoadingProps) => {
    const { styles } = useStyles()

    return (
        <div className={styles.box} data-testid={testId}>
            <Spin size="small" />
        </div>
    )
}
