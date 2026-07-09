import apiCall from './apiCall'
import type { ProjectDescriptorView } from '../types/projectDescriptor'

const DESCRIPTOR_API_OPTIONS = { throwError: true, suppressErrorPages: true } as const

/**
 * Reads the editable project descriptor (`rules.xml`) for a project. The descriptor is the
 * source of truth for the Project page's editable fields; the response also carries an
 * `editable` flag and a `contentHash` used for optimistic concurrency on save.
 */
export function fetchProjectDescriptor(projectId: string): Promise<ProjectDescriptorView> {
    return apiCall(
        `/projects/${encodeURIComponent(projectId)}/descriptor`,
        {
            method: 'GET',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' },
        }
    ) as Promise<ProjectDescriptorView>
}

/**
 * Writes the edited descriptor as a whole document and returns the fresh view (with a new content
 * hash). Throws an {@code ApiHttpError} with status 409 when `rules.xml` changed on the server since
 * it was loaded; the caller confirms and retries with `force` to overwrite.
 */
export function updateProjectDescriptor(
    projectId: string,
    descriptor: ProjectDescriptorView,
    force = false
): Promise<ProjectDescriptorView> {
    const query = force ? '?force=true' : ''
    return apiCall(
        `/projects/${encodeURIComponent(projectId)}/descriptor${query}`,
        {
            method: 'PUT',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(descriptor),
        },
        DESCRIPTOR_API_OPTIONS
    ) as Promise<ProjectDescriptorView>
}

/**
 * Generates the OpenAPI schema from the project's compiled rules and datatypes, writes it as a
 * project file, and points the descriptor's `<openapi>` at it in reconciliation mode. Returns the
 * fresh descriptor view. Throws an {@code ApiHttpError} with status 409 when the project has no
 * modules or does not compile.
 */
export function generateProjectOpenApiSchema(projectId: string): Promise<ProjectDescriptorView> {
    return apiCall(
        `/projects/${encodeURIComponent(projectId)}/openapi`,
        {
            method: 'POST',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' },
        },
        DESCRIPTOR_API_OPTIONS
    ) as Promise<ProjectDescriptorView>
}
