import { useEffect, useRef, useState } from 'react'
import { useLiveProjectStatus } from '../../hooks/useLiveProjectStatus'
import { startModuleCompilation } from '../../services/modules'
import type { ProjectCompileState, ProjectStatusUpdate } from '../../services/projectStatus'
import { errorHandler } from '../../utils/errorHandling'

/** How far the project has come, and whether the module the editor is opening is among what is done. */
interface ModuleCompilation {
    /** True once this module is compiled, whatever the modules after it are still doing. */
    ready: boolean
    /** Modules compiled so far, and how many there are in all — what the progress reads from. */
    compiled: number
    total: number
    /** Set when the compilation could not even be asked for; nothing will arrive on the channel. */
    failure: string | null
    /** How many tests the compiled project holds, as the channel reports them. */
    tests: number
    /** How the project's own compilation is going, for the screen to show beside the module. */
    state: ProjectCompileState
    /**
     * Set while the module is waiting for the reader to compile it — a write landed with automatic compilation
     * switched off, so what the compiler says about the module is what it said before that write.
     */
    verifyNeeded: boolean
    /** The status behind all of the above, so the screen can phrase what it says about it. */
    status: ProjectStatusUpdate | null
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
    enabled = true,
    /** Whether the module is to be built from its workbook afresh, dropping what was compiled before. */
    rebuild = true
): ModuleCompilation => {
    const [failure, setFailure] = useState<string | null>(null)
    // A compilation reporting its progress says how far it has come, not how many tests the project holds —
    // counting those walks every method it compiled. The last count stands until a full status brings a new one,
    // so the Test button does not empty and fill again with every push.
    const counted = useRef(0)
    // Subscribed only once the project is known, and with it the branch the channel is named after. Listening
    // before that subscribes to the wrong channel and throws away what it heard when the right one replaces it,
    // which on a project that compiles in a moment means hearing nothing at all.
    const status = useLiveProjectStatus(projectId, branch, enabled, initial, initialReadAt)
    const compiledModules = modulesOf(status)?.compiledModules
    const named = (compiledModules ?? []).includes(moduleName)
    // A module named as compiled stays compiled. A compilation reports its progress many times a second, and a
    // progress report carries only what it could read without waiting — often not the names — so a screen
    // reading this answer alone would open on the module and close again with the next report. Compiling this
    // module afresh is what makes the question open again.
    //
    // Remembered under the name of the module and the copy of the project it was compiled in, so that the
    // module switched to is answered for itself in the very render it is switched to, not for the one left -
    // and a module come back to is still ready, whatever the report at hand carries.
    const compiledKey = `${projectId} ${branch ?? ''} ${moduleName} ${reloadToken}`
    const [compiledKeys, setCompiledKeys] = useState<ReadonlySet<string>>(new Set())
    const ready = compiledKeys.has(compiledKey) || named

    useEffect(() => {
        if (named) {
            setCompiledKeys(keys => (keys.has(compiledKey) ? keys : new Set(keys).add(compiledKey)))
        }
    }, [named, compiledKey])

    // Asked for once per module, and once more for every refresh. Re-asking on each pushed status would restart
    // the very compilation the pushes are reporting on, and the session compiles one module at a time.
    //
    // Every request made is remembered, not just the last: a reader who refreshes one module, reads another and
    // comes back would otherwise be asking for that refresh again on every return, rebuilding the module from
    // its workbook each time.
    //
    // Remembered for one copy of the project, though. Switching the branch checks another copy out and drops
    // everything compiled from the first, so what was asked for before says nothing about what is compiled now:
    // a module opened again on a branch already visited is compiled again rather than waited on forever.
    const asked = useRef({ checkout: '', keys: new Set<string>() })
    useEffect(() => {
        const checkout = `${projectId} ${branch ?? ''}`
        if (asked.current.checkout !== checkout) {
            asked.current = { checkout, keys: new Set() }
        }
        const key = `${moduleName} ${reloadToken}`
        // A module already compiled needs no compiling — unless a refresh asked, which is exactly a request to
        // compile it again.
        if (!enabled || asked.current.keys.has(key) || (ready && reloadToken === 0)) {
            return
        }
        asked.current.keys.add(key)
        setFailure(null)
        startModuleCompilation(projectId, moduleName, rebuild && reloadToken > 0).catch((error: unknown) => {
            const failed = error instanceof Error ? error : new Error(String(error))
            errorHandler.logError(failed)
            setFailure(failed.message)
        })
    }, [projectId, branch, moduleName, reloadToken, ready, enabled, rebuild])

    counted.current = status?.compilation?.tests?.total ?? counted.current

    return {
        ready,
        compiled: modulesOf(status)?.compiled ?? 0,
        total: modulesOf(status)?.total ?? 0,
        failure,
        tests: counted.current,
        state: status?.compileState ?? 'idle',
        verifyNeeded: status?.manualCompileNeeded === true,
        status,
    }
}
