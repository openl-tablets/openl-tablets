import apiCall, { asArray } from './apiCall'
import { Role } from '../constants'

export interface AclSubject {
    sid: string
    principal?: boolean
}

export interface AccessControlEntry {
    role: Role
    sub: AclSubject
    source?: 'project' | 'repository' | undefined
}

/** One ACL rule of a project as returned by the per-project endpoint (subject + role). */
interface AclProjectRule {
    sid: AclSubject
    role: Role
    source?: 'project' | 'repository' | undefined
}

/** Fetch the access-control entries (subject + role) assigned to a specific project. */
export async function getProjectAcl(projectId: string, options?: { inherited?: boolean }): Promise<AccessControlEntry[]> {
    const inherited = options?.inherited ? '?inherited=true' : ''
    const response = (await apiCall(
        `/acls/projects/${encodeURIComponent(projectId)}${inherited}`,
        undefined,
        { throwError: true }
    )) as AclProjectRule[]
    return asArray<AclProjectRule>(response).map(rule => ({ role: rule.role, source: rule.source, sub: rule.sid }))
}

/** Suggest users or groups for a project manager's Add access dialog. */
export async function searchProjectAclSubjects(
    projectId: string,
    principal: boolean,
    search: string,
    pageSize = 10
): Promise<string[]> {
    const params = new URLSearchParams()
    params.set('principal', String(principal))
    params.set('search', search.trim())
    params.set('pageSize', String(pageSize))
    const response = await apiCall(
        `/acls/projects/${encodeURIComponent(projectId)}/subjects?${params}`,
        undefined,
        { throwError: true }
    )
    return asArray<string>(response)
}

/**
 * Assign a role to a subject on a project. {@code principal} distinguishes a user (true) from a group
 * (false); the backend resolves the SID as a group when the flag is absent, so it must always be sent.
 */
export async function setProjectAcl(projectId: string, sid: string, role: Role, principal: boolean): Promise<void> {
    await apiCall(
        `/acls/projects/${encodeURIComponent(projectId)}?sid=${encodeURIComponent(sid)}&principal=${principal}`,
        { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ role }) },
        { throwError: true }
    )
}

/** Revoke a subject's access to a project. {@code principal} distinguishes a user (true) from a group. */
export async function removeProjectAcl(projectId: string, sid: string, principal: boolean): Promise<void> {
    await apiCall(
        `/acls/projects/${encodeURIComponent(projectId)}?sid=${encodeURIComponent(sid)}&principal=${principal}`,
        { method: 'DELETE' },
        { throwError: true }
    )
}
