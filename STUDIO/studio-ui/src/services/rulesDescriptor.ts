// Reads and writes the declared text of a project's `rules.xml` on the client, the way the deploy
// descriptor is handled. Only the plain values the UI shows and edits are managed here; the parts that
// need the engine — the resolved modules and sources — come from the project response instead.

import { emptyOf, parseXmlRoot, readXml, writeXml, type XmlMapping } from './xmlDescriptor'

/** What the engine does with the OpenAPI file: validates the project against it, or generates tables from it. */
export type OpenApiMode = 'RECONCILIATION' | 'GENERATION'

/** The OpenAPI settings a project declares. */
export interface DescriptorOpenApi {
    path?: string
    mode?: OpenApiMode
    modelModuleName?: string
    algorithmModuleName?: string
}

/** A method-name filter: the patterns something includes and excludes. */
export interface MethodFilter {
    includes: string[]
    excludes: string[]
}

/** What OpenL Studio is told about a module, in the module's `<webstudioConfiguration>`. */
export interface WebStudioConfiguration {
    /**
     * Whether opening this module compiles it alone, leaving the modules it does not depend on out. It is
     * meant for a project too large to compile through — a project with heavy tests, above all.
     */
    compileThisModuleOnly: boolean
}

/** A module a project declares in its `<modules>` — its name, rules-root path and method filter. */
export interface ModuleDeclaration {
    name: string
    path: string
    methodFilter?: MethodFilter | undefined
    webstudioConfiguration?: WebStudioConfiguration | undefined
    /** Serialized `<module>` children the overview does not manage, kept as declared and written back
     * verbatim so an edit never loses them. */
    preserved?: string[] | undefined
}

/** A dependency a project declares in its `<dependencies>`. */
export interface DeclaredDependency {
    name: string
    /** Whether every module of the dependency is included automatically. */
    autoIncluded: boolean
    /** Kept as declared and carried through edits; the form does not change it. */
    mavenArtifact?: string | undefined
}

/** The declared text of a `rules.xml`, as the overview reads it. */
export interface RulesDescriptor {
    description: string
    versionPatterns: string[]
    /** The source path entries the file declares (its `<classpath>`), empty when it declares none. */
    sources: string[]
    /** The modules the file declares (its `<modules>`), before the engine resolves any wildcard. */
    moduleDeclarations: ModuleDeclaration[]
    /** The projects this one declares a dependency on (its `<dependencies>`). */
    dependencies: DeclaredDependency[]
    propertiesFileNameProcessor?: string | undefined
    exposedMethods?: MethodFilter | undefined
    openapi?: DescriptorOpenApi | undefined
    /** Serialized project-level elements the overview does not manage, kept as declared and written back
     * verbatim so an edit never loses them. */
    preserved: string[]
}

/**
 * How the declared text is kept in `rules.xml`, in the order the elements are written.
 *
 * <p>A module is a module by its name, its path or the parts the overview does not manage; a row added and
 * left empty names none, whatever else is ticked on it. A dependency is one by its name. The OpenAPI settings
 * say something once they name a file or a module: a mode on its own is not worth an element.
 */
const MAPPING: XmlMapping<RulesDescriptor> = {
    root: 'project',
    fields: {
        description: { kind: 'text', tag: 'comment' },
        moduleDeclarations: {
            kind: 'objects',
            wrapper: 'modules',
            item: 'module',
            fields: {
                name: { kind: 'text', tag: 'name' },
                path: { kind: 'text', tag: 'rules-root', attribute: 'path' },
                // The engine writes the block only when the module is compiled on its own and reads its
                // absence as the default, so clearing the flag leaves the file as it was before it was set.
                webstudioConfiguration: {
                    kind: 'object',
                    tag: 'webstudioConfiguration',
                    secondary: true,
                    fields: { compileThisModuleOnly: { kind: 'flag', tag: 'compileThisModuleOnly' } },
                },
                // A module's method filter keeps its patterns in wrapped lists:
                // `<includes><value>…</value></includes>` and `<excludes>` alike.
                methodFilter: {
                    kind: 'object',
                    tag: 'method-filter',
                    secondary: true,
                    fields: {
                        includes: { kind: 'list', wrapper: 'includes', item: 'value' },
                        excludes: { kind: 'list', wrapper: 'excludes', item: 'value' },
                    },
                },
                preserved: { kind: 'rest' },
            },
        },
        sources: { kind: 'list', wrapper: 'classpath', item: 'entry', attribute: 'path' },
        versionPatterns: { kind: 'list', item: 'properties-file-name-pattern' },
        propertiesFileNameProcessor: { kind: 'text', tag: 'properties-file-name-processor' },
        dependencies: {
            kind: 'objects',
            wrapper: 'dependencies',
            item: 'dependency',
            fields: {
                name: { kind: 'text', tag: 'name' },
                autoIncluded: { kind: 'flag', tag: 'autoIncluded', secondary: true },
                mavenArtifact: { kind: 'text', tag: 'mavenArtifact', secondary: true },
            },
        },
        exposedMethods: {
            kind: 'object',
            tag: 'exposed-methods',
            fields: {
                includes: { kind: 'list', item: 'include' },
                excludes: { kind: 'list', item: 'exclude' },
            },
        },
        openapi: {
            kind: 'object',
            tag: 'openapi',
            fields: {
                path: { kind: 'text', tag: 'path' },
                modelModuleName: { kind: 'text', tag: 'model-module-name' },
                algorithmModuleName: { kind: 'text', tag: 'algorithm-module-name' },
                mode: { kind: 'choice', tag: 'mode', values: ['RECONCILIATION', 'GENERATION'], secondary: true },
            },
        },
        preserved: { kind: 'rest' },
    },
}

export const EMPTY_RULES_DESCRIPTOR: RulesDescriptor = emptyOf(MAPPING)

/**
 * Reads the declared text of a `rules.xml`. A blank or unreadable file reads as an empty descriptor —
 * the overview shows nothing for it rather than an error, since the resolved values still come from the
 * project response.
 */
export const parseRulesDescriptor = (xml: string): RulesDescriptor => {
    const root = parseXmlRoot(xml, MAPPING.root)
    return root ? readXml(MAPPING, root) : EMPTY_RULES_DESCRIPTOR
}

/**
 * Writes the edited descriptor back to `rules.xml`: the managed elements — the modules, the classpath, the
 * dependencies and the other declared text — from the edited model, then the elements the overview does not
 * manage, as they were read. Within each module the parts the overview does not manage are kept the same
 * way, so nothing the editor does not touch is ever lost.
 */
export const serializeRulesDescriptor = (descriptor: RulesDescriptor): string => writeXml(MAPPING, descriptor)
