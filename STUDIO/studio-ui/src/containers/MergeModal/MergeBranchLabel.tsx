import { BranchLabel } from '../projects/BranchLabel'
import { BranchInfo } from './types'

interface MergeBranchLabelProps {
    /** The branches of the project, the source of the marks a branch carries. */
    branches: BranchInfo[]
    name: string
    withIcon?: boolean | undefined
    testId?: string | undefined
}

/**
 * A branch of the merged project, marked as it is everywhere else: the Default badge for the repository
 * main branch and the shield for a protected one. The marks are looked up in the project's branch list.
 *
 * The dialog is about these branches, so the name reads as a value here — in full and in the normal text
 * colour — rather than as the note it is beside a project.
 */
export const MergeBranchLabel = ({ branches, name, withIcon, testId }: MergeBranchLabelProps) => {
    const info = branches.find(item => item.name === name)
    return (
        <BranchLabel
            prominent
            isDefault={info?.base ?? false}
            isProtected={info?.protected ?? false}
            name={name}
            testId={testId}
            withIcon={withIcon ?? false}
        />
    )
}
