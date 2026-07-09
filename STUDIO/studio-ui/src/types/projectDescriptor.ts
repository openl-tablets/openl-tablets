/**
 * Editable content of a project's `rules.xml` (the project descriptor), as returned by
 * `GET /projects/{projectId}/descriptor`. This is the single source of truth for the fields
 * the Project page edits; `ProjectViewModel` (from `GET /projects/{projectId}`) stays read-only.
 *
 * Array and object fields are optional: the backend omits empty collections and null values
 * during serialization (e.g. a project with no `rules.xml` returns just `{ editable }`).
 */
export interface ProjectDescriptorView {
    name?: string
    comment?: string
    modules?: ProjectDescriptorModule[]
    dependencies?: ProjectDescriptorDependency[]
    classpath?: string[]
    openapi?: ProjectDescriptorOpenApi
    exposedMethods?: ProjectDescriptorExposedMethod[]
    propertiesFileNameProcessor?: string
    propertiesFileNamePatterns?: string[]
    /** Whether the current user may edit the descriptor (design-repo WRITE ACL). */
    editable: boolean
    /** Hash of the current `rules.xml`; echoed back on save for optimistic concurrency. */
    contentHash?: string
}

export interface ProjectDescriptorModule {
    name?: string
    rulesRootPath?: string
    /** Deprecated module-level method filter. Preserved on round-trip; not shown or edited. */
    methodFilter?: ProjectDescriptorMethodFilter
    /** Internal/experimental flag. Preserved on round-trip; not shown or edited. */
    compileThisModuleOnly?: boolean
    /** True when `rulesRootPath` contains a wildcard; such modules are read-only derived rows. */
    wildcard?: boolean
}

export interface ProjectDescriptorMethodFilter {
    includes?: string[]
    excludes?: string[]
}

export interface ProjectDescriptorDependency {
    name?: string
    autoIncluded?: boolean
    /** Preserved on round-trip; not edited by the current UI. */
    mavenArtifact?: string
}

export type ProjectDescriptorOpenApiMode = 'RECONCILIATION' | 'GENERATION'

export interface ProjectDescriptorOpenApi {
    path?: string
    mode?: ProjectDescriptorOpenApiMode
    modelModuleName?: string
    algorithmModuleName?: string
}

export type ProjectDescriptorExposedType = 'include' | 'exclude'

/** One exposed-method rule: a glob pattern matched against method names, and whether it includes or excludes. */
export interface ProjectDescriptorExposedMethod {
    pattern?: string
    type: ProjectDescriptorExposedType
}
