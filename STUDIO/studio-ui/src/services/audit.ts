import apiCall from './apiCall'

/**
 * The actions of the audit log by the category they belong to, as the REST API names them. The categories and the
 * actions of each come in the order the audit log offers them.
 */
export const AUDIT_ACTIONS = {
    authentication: ['signIn', 'signOut'],
    user: [
        'userCreated',
        'userUpdated',
        'passwordChanged',
        'userDeleted',
        'groupMemberAdded',
        'groupMemberRemoved',
        'externalGroupsChanged',
    ],
    group: ['groupCreated', 'groupUpdated', 'groupPrivilegesChanged', 'groupDeleted'],
    token: ['tokenCreated', 'tokenDeleted'],
    permission: ['roleGranted', 'roleChanged', 'roleRevoked'],
    access: ['accessDenied'],
    project: [
        'projectCreated',
        'projectCopied',
        'projectSaved',
        'projectDeleted',
        'projectUnlocked',
        'branchCreated',
        'branchDeleted',
        'branchMerged',
        'protectionBypassed',
    ],
    deployment: ['projectDeployed'],
    audit: ['auditLogRead', 'auditLogPurged'],
} as const
export const AUDIT_OUTCOMES = ['success', 'failure', 'denied'] as const
export const AUDIT_AUTHENTICATIONS = ['form', 'basic', 'token', 'bearer', 'oauth2', 'saml'] as const

/** The area an audit event belongs to, as the REST API names it. */
export type AuditCategory = keyof typeof AUDIT_ACTIONS

/** What an audit event records, as the REST API names it. */
export type AuditAction = (typeof AUDIT_ACTIONS)[AuditCategory][number]

/** How the action of an audit event ended. */
export type AuditOutcome = (typeof AUDIT_OUTCOMES)[number]

/** How the user of an audit event signed in, or tried to. */
export type AuditAuthentication = (typeof AUDIT_AUTHENTICATIONS)[number]

/** An event of the audit log. */
export interface AuditEvent {
    id: number
    time: string
    actor?: string
    authentication?: AuditAuthentication
    clientAddress?: string
    category: AuditCategory
    action: AuditAction
    outcome: AuditOutcome
    targetType?: string
    target?: string
    subjectType?: string
    subject?: string
    details?: Record<string, unknown>
}

/** A page of the audit log, the newest events first. */
export interface AuditEventsPage {
    content?: AuditEvent[]
    pageNumber: number
    pageSize: number
    total?: number
    /** Whether more events match than the total tells: the server stops counting at its limit. */
    totalLimited?: boolean
}

/** Which events to read. A criterion left out matches every event. */
export interface AuditQuery {
    /** First moment of the period, in ISO 8601; the period includes it. */
    from?: string | undefined
    /** Moment the period ends, in ISO 8601; the period does not include it. */
    to?: string | undefined
    actor?: string | undefined
    actions?: AuditAction[] | undefined
    outcome?: AuditOutcome | undefined
    authentication?: AuditAuthentication | undefined
    target?: string | undefined
    subject?: string | undefined
    clientAddress?: string | undefined
}

const AS_JSON = { headers: { Accept: 'application/json' } }
const AS_CSV = { headers: { Accept: 'text/csv' } }

/** The query string of the criteria, leaving out the ones without a value. */
export const auditQueryParams = (query: AuditQuery): URLSearchParams => {
    const params = new URLSearchParams()
    const add = (name: string, value?: string) => {
        if (value?.trim()) {
            params.append(name, value.trim())
        }
    }
    add('from', query.from)
    add('to', query.to)
    add('actor', query.actor)
    query.actions?.forEach(action => params.append('action', action))
    add('outcome', query.outcome)
    add('authentication', query.authentication)
    add('target', query.target)
    add('subject', query.subject)
    add('clientAddress', query.clientAddress)
    return params
}

/** Reads a page of the events the query matches, the newest first. */
export async function getAuditEvents(query: AuditQuery, page: number, size: number): Promise<AuditEventsPage> {
    const params = auditQueryParams(query)
    params.set('page', String(page))
    params.set('size', String(size))
    return apiCall(`/admin/audit?${params}`, AS_JSON, { throwError: true })
}

/** Reads every event the query matches as a CSV file. */
export async function exportAuditEvents(query: AuditQuery): Promise<Blob> {
    const params = auditQueryParams(query)
    params.set('unpaged', 'true')
    return apiCall(`/admin/audit?${params}`, AS_CSV, { throwError: true, responseType: 'blob' })
}
