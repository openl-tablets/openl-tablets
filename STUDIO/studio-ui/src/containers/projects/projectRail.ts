import { useEffect } from 'react'
import { useOutletContext } from 'react-router'
import type { ProjectFileChangeType } from '../../services/projectStatus'

/** What the page of a project tells the tree beside it, which the Projects screen keeps across its pages. */
export interface ProjectRailState {
    /** The project the address names, which the report is about: a report about another one is stale. */
    address?: string | undefined
    /** The project shown. */
    currentProjectId?: string | undefined
    /** Its files changed and not saved yet, by path; none until the page has read the project. */
    currentFileChanges?: ReadonlyMap<string, ProjectFileChangeType> | undefined
    /** Empty folders added to it in the browser. */
    currentFolders?: string[] | undefined
}

/** What the Projects screen hands the page of a project as its outlet context. */
export interface ProjectRail {
    /** Takes what the page shows. */
    report: (state: ProjectRailState) => void
    /** The page read its project again: the tree reads its files again too. */
    reloaded: () => void
}

/**
 * Tells the tree of the Projects screen what the page of a project shows, that it shows nothing once the page goes,
 * and that the page read its project again — every read after the first one, counted by `reads`. A page shown outside
 * the screen has nowhere to report to, and reports nothing.
 */
export const useReportProjectRail = (state: ProjectRailState, reads: number): void => {
    const rail = useOutletContext<ProjectRail | undefined>()
    const { address, currentProjectId, currentFileChanges, currentFolders } = state
    useEffect(() => {
        rail?.report({ address, currentProjectId, currentFileChanges, currentFolders })
    }, [address, currentFileChanges, currentFolders, currentProjectId, rail])
    useEffect(() => () => rail?.report({}), [rail])
    // The tree reads the files of a project itself when it is opened: only a read again has it read them again.
    useEffect(() => {
        if (reads > 1) {
            rail?.reloaded()
        }
    }, [rail, reads])
}
