import apiCall, { asArray } from './apiCall'
import type { Repository } from '../types/repositories'

export interface Deployment {
    id: string
    name: string
}

/** The revision a deployed project has in the design repository it was built from. */
export interface DesignRevision {
    revision: string
    modifiedBy?: string
    modifiedAt?: string
}

export interface DeploymentItem {
    name: string
    modifiedBy?: string
    modifiedAt?: string
    /** Absent while the design repository is still being indexed, and for projects deployed elsewhere. */
    designRevision?: DesignRevision
}

export interface DeploymentDetail extends Deployment {
    repository: string
    items: DeploymentItem[]
}

/** List the production (deployment) repositories the current user may read. */
export async function getProductionRepositories(): Promise<Repository[]> {
    const response = await apiCall('/production-repos', undefined, { throwError: true })
    return asArray(response)
}

/**
 * Whether the user may read any deployment repository at all — the question the navigation asks before
 * offering the Deployments tab.
 *
 * A successful answer holds for the whole session (repository access is granted by an administrator), so it
 * is asked once. A failed request answers `true` — a transient error must not take a tab out of the
 * navigation — but that guess is not remembered: the next caller probes again, so a one-off error never
 * leaves the tab wrongly offered for the rest of the session.
 */
let deploymentAccess: Promise<boolean> | undefined
export function hasDeploymentRepositories(): Promise<boolean> {
    deploymentAccess ??= getProductionRepositories()
        .then(repositories => repositories.length > 0)
        .catch(() => {
            deploymentAccess = undefined
            return true
        })
    return deploymentAccess
}

/** List the deployments in a production repository. */
export async function getDeployments(repositoryId: string): Promise<Deployment[]> {
    const response = await apiCall(
        `/deployments?repository=${encodeURIComponent(repositoryId)}`,
        undefined,
        { throwError: true }
    )
    return asArray(response)
}

/** List deployments in a production repository that contain a specific project, including matching items. */
export async function getProjectDeployments(repositoryId: string, projectName: string): Promise<DeploymentDetail[]> {
    const response = await apiCall(
        `/deployments?repository=${encodeURIComponent(repositoryId)}&project=${encodeURIComponent(projectName)}`,
        undefined,
        { throwError: true }
    )
    return asArray(response)
}

/** Get a single deployment with its deployed projects (drill-down level). */
export async function getDeployment(id: string): Promise<DeploymentDetail> {
    return await apiCall(`/deployments/${encodeURIComponent(id)}`, undefined, { throwError: true }) as DeploymentDetail
}
