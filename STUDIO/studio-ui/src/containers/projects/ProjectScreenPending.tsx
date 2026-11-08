import React from 'react'
import { Alert, Skeleton } from 'antd'
import { useSharedStyles } from './sharedStyles'
import { UnresolvedProjectLink } from './UnresolvedProjectLink'

interface ProjectScreenPendingProps {
    /** Whether the address leads to no project. */
    linkProblem: boolean
    /** Why the project could not be read, or null while it is read. */
    failure?: string | null | undefined
    testId: string
}

/**
 * What a screen of a project shows until the project is read: the shape of the screen, why the project could not
 * be read, or that the address leads to no project.
 */
export const ProjectScreenPending = ({ linkProblem, failure = null, testId }: ProjectScreenPendingProps) => {
    const { styles: shared } = useSharedStyles()
    const shown = () => {
        if (linkProblem) {
            return <UnresolvedProjectLink />
        }
        return failure === null ? <Skeleton active /> : <Alert showIcon title={failure} type="error" />
    }
    return (
        <div className={shared.workspacePage}>
            <div className={shared.workspaceCentered} data-testid={testId}>{shown()}</div>
        </div>
    )
}
