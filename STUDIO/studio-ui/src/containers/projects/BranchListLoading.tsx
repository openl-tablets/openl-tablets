import { Spin } from 'antd'
import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css }) => ({
    /** Takes the place, and the padding, a list of branches would have taken. */
    box: css`
        padding: 12px;
        text-align: center;
    `,
}))

interface BranchListLoadingProps {
    testId?: string | undefined
}

/**
 * The branches are still being read, shown where the branches themselves belong.
 *
 * A heavy repository takes seconds to list them, and an empty list in the meantime reads as a repository
 * without a single branch. Every branch list — the switcher's popup and the pickers' dropdown alike — says
 * so the same way.
 */
export const BranchListLoading = ({ testId }: BranchListLoadingProps) => {
    const { styles } = useStyles()

    return (
        <div className={styles.box} data-testid={testId}>
            <Spin size="small" />
        </div>
    )
}
