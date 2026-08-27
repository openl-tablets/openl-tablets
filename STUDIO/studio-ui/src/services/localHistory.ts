import apiCall, { LOCAL_LOAD_API_OPTIONS, asArray } from './apiCall'
import { toUrlSafeId } from './projectId'

export interface LocalHistoryItem {
    id: string
    modifiedOn: string
    current?: boolean
}

export const getLocalHistory = async (projectId: string, moduleName?: string): Promise<LocalHistoryItem[]> => {
    const query = moduleName ? `?module=${encodeURIComponent(moduleName)}` : ''
    const response = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/local-history${query}`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    )
    return asArray<LocalHistoryItem>(response)
}

export const restoreLocalHistory = async (
    projectId: string,
    moduleName: string | undefined,
    version: string
): Promise<void> => {
    const query = moduleName ? `?module=${encodeURIComponent(moduleName)}` : ''
    await apiCall(`/projects/${toUrlSafeId(projectId)}/local-history/restore${query}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ version }),
    }, { throwError: true, suppressErrorPages: true })
}
