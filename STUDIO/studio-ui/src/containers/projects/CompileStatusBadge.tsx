import { useTranslation } from 'react-i18next'
import { useLiveProjectStatus } from '../../hooks/useLiveProjectStatus'
import { type ProjectCompileState, type ProjectStatusUpdate } from '../../services/projectStatus'
import { CompileDot, getCompileTooltip } from './CompileIndicator'

interface CompileStatusBadgeProps {
    projectId: string
    branch: string | null
    initialStatus?: ProjectStatusUpdate | undefined
    initialState?: ProjectCompileState | undefined
}

/**
 * Live compilation status for an open project. The initial state comes from the project detail response;
 * pushed transitions keep it current afterwards.
 */
export const CompileStatusBadge = ({ projectId, branch, initialStatus, initialState }: CompileStatusBadgeProps) => {
    const { t } = useTranslation('repository')
    const status = useLiveProjectStatus(
        projectId,
        branch,
        true,
        initialStatus ?? (initialState ? { projectId, branch, compileState: initialState } : null)
    )

    if (!status || status.compileState === 'idle') {
        return null
    }

    const tooltip = getCompileTooltip(status, status.compileState, t)
    return <CompileDot showLabel state={status.compileState} testId="compile-status" tooltip={tooltip} />
}
