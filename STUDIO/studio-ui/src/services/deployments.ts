import apiCall, { asArray } from './apiCall'
import type { Repository } from '../types/repositories'

export interface Deployment {
    id: string
    name: string
}

export interface DeploymentItem {
    name: string
    modifiedBy?: string
    modifiedAt?: string
    revision?: string
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
