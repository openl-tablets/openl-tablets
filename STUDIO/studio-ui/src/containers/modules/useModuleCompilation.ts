import { useEffect, useRef, useState } from 'react'
import { useLiveProjectStatus } from '../../hooks/useLiveProjectStatus'
import { startModuleCompilation } from '../../services/modules'
import type { ProjectStatusUpdate } from '../../services/projectStatus'
import { errorHandler } from '../../utils/errorHandling'

/** How far the project has come, and whether the module the editor is opening is among what is done. */
export interface ModuleCompilation {
    /** True once this module is compiled, whatever the modules after it are still doing. */
    ready: boolean
    /** Modules compiled so far, and how many there are in all — what the progress reads from. */
    compiled: number
    total: number
    /** Set when the compilation could not even be asked for; nothing will arrive on the channel. */
    failure: string | null
    /** How many tests the compiled project holds, as the channel reports them. */
    tests: number
}

const modulesOf = (status: ProjectStatusUpdate | null) => status?.compilation?.modules

/**
 * Follows a module's compilation and says when its tables can be read.
 *
 * Compiling through to a module takes minutes on a large project, so nothing is waited for over HTTP: the
 * compilation is asked for once, and how far it has come arrives on the project's status channel, which names each
 * module as it finishes. The editor renders as soon as this module is named — the modules after it go on compiling
 * behind the open screen.
 *
 * A module already compiled when the screen opens is ready at once, and no compilation is asked for. A refresh
 * asks for it to be built again from the workbook, dropping what was compiled before — that is what a refresh is
 * for.
 *
 * Nothing is asked for at all while {@code enabled} is false — of a project nobody has opened there is no copy to
 * compile, and the screen asks the reader to open it first.
 */
export const useModuleCompilation = (
    projectId: string,
    branch: string | null,
    moduleName: string,
    initial: ProjectStatusUpdate | null,
    initialReadAt = 0,
    reloadToken = 0,
    enabled = true
): ModuleCompilation => {
    const [failure, setFailure] = useState<string | null>(null)
    const status = useLiveProjectStatus(projectId, branch, true, initial, initialReadAt)
    const compiledModules = modulesOf(status)?.compiledModules
    const ready = (compiledModules ?? []).includes(moduleName)

    // Asked for once per module, and once more for every refresh. Re-asking on each pushed status would restart
    // the very compilation the pushes are reporting on, and the session compiles one module at a time.
    const asked = useRef<string | null>(null)
    useEffect(() => {
        const key = `${projectId} ${branch ?? ''} ${moduleName} ${reloadToken}`
        // A module already compiled needs no compiling — unless a refresh asked, which is exactly a request to
        // compile it again.
        if (!enabled || asked.current === key || (ready && reloadToken === 0)) {
            return
        }
        asked.current = key
        setFailure(null)
        startModuleCompilation(projectId, moduleName, reloadToken > 0).catch((error: unknown) => {
            const failed = error instanceof Error ? error : new Error(String(error))
            errorHandler.logError(failed)
            setFailure(failed.message)
        })
    }, [projectId, branch, moduleName, reloadToken, ready, enabled])

    return {
        ready,
        compiled: modulesOf(status)?.compiled ?? 0,
        total: modulesOf(status)?.total ?? 0,
        failure,
        tests: status?.compilation?.tests?.total ?? 0,
    }
}
