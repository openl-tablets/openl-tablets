// Reads and writes the declared text of a project's `rules.xml` on the client, the way the deploy
// descriptor is handled. Only the plain values the UI shows and edits are managed here; the parts that
// need the engine — the resolved modules and sources — come from the project response instead.

import { escapeXml } from '../utils/escapeXml'
import { childValue, childValues, directChild, parseXmlRoot, preservedChildren, unmanagedChildren } from './xmlDescriptor'

const ROOT = 'project'

// The elements this editor owns and rewrites on save. The modules, the classpath and the dependencies are
// rebuilt from the model the overview edits; anything else at the project level is carried over verbatim.
const MANAGED = new Set([
    'comment',
    'classpath',
    'modules',
    'dependencies',
    'properties-file-name-pattern',
    'properties-file-name-processor',
    'exposed-methods',
    'openapi',
])

// Inside a rebuilt <module>, these are the parts the overview reads and writes. Every other child — a
// <webstudio-configuration> block above all — is preserved so an edit-and-save never drops it.
const MODULE_MANAGED = new Set(['name', 'rules-root', 'method-filter'])

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

/** A module a project declares in its `<modules>` — its name, rules-root path and method filter. */
export interface ModuleDeclaration {
    name: string
    path: string
    methodFilter?: MethodFilter | undefined
    /** Serialized `<module>` children the overview does not manage (e.g. `<webstudio-configuration>`),
     * kept as declared and written back verbatim so an edit never loses them. */
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
}

export const EMPTY_RULES_DESCRIPTOR: RulesDescriptor = {
    description: '',
    versionPatterns: [],
    sources: [],
    moduleDeclarations: [],
    dependencies: [],
}

const readExposedMethods = (root: Element): MethodFilter | undefined => {
    const element = directChild(root, 'exposed-methods')
    if (!element) {
        return undefined
    }
    const includes = childValues(element, 'include')
    const excludes = childValues(element, 'exclude')
    return includes.length > 0 || excludes.length > 0 ? { includes, excludes } : undefined
}

/** The source path entries declared in `<classpath>`: the `path` of each `<entry>`. */
const readSources = (root: Element): string[] => {
    const classpath = directChild(root, 'classpath')
    if (!classpath) {
        return []
    }
    return Array.from(classpath.children)
        .filter(child => child.tagName === 'entry')
        .map(entry => entry.getAttribute('path')?.trim() ?? '')
        .filter(Boolean)
}

/** The projects declared under `<dependencies>`, each with its auto-included flag. */
const readDependencies = (root: Element): DeclaredDependency[] => {
    const dependencies = directChild(root, 'dependencies')
    if (!dependencies) {
        return []
    }
    return Array.from(dependencies.children)
        .filter(child => child.tagName === 'dependency')
        .map(dependency => {
            const maven = childValue(dependency, 'mavenArtifact')
            return {
                name: childValue(dependency, 'name'),
                autoIncluded: childValue(dependency, 'autoIncluded') === 'true',
                ...(maven ? { mavenArtifact: maven } : {}),
            }
        })
        .filter(dependency => dependency.name)
}

/** The values of a wrapped list, e.g. `<includes><value>..</value></includes>`. */
const wrappedValues = (parent: Element, wrapper: string): string[] => {
    const element = directChild(parent, wrapper)
    return element ? childValues(element, 'value') : []
}

/** The method-filter of a `<module>`, or undefined when it declares none. */
const readMethodFilter = (module: Element): MethodFilter | undefined => {
    const filter = directChild(module, 'method-filter')
    if (!filter) {
        return undefined
    }
    const includes = wrappedValues(filter, 'includes')
    const excludes = wrappedValues(filter, 'excludes')
    return includes.length > 0 || excludes.length > 0 ? { includes, excludes } : undefined
}

/** The modules declared in `<modules>`: the name, rules-root path, method filter and unmanaged parts of each. */
const readModuleDeclarations = (root: Element): ModuleDeclaration[] => {
    const modules = directChild(root, 'modules')
    if (!modules) {
        return []
    }
    return Array.from(modules.children)
        .filter(child => child.tagName === 'module')
        .map(module => {
            const filter = readMethodFilter(module)
            const preserved = unmanagedChildren(module, MODULE_MANAGED)
            return {
                name: childValue(module, 'name'),
                path: directChild(module, 'rules-root')?.getAttribute('path')?.trim() ?? '',
                ...(filter ? { methodFilter: filter } : {}),
                ...(preserved.length > 0 ? { preserved } : {}),
            }
        })
        .filter(module => module.path || module.name || module.preserved)
}

const readOpenApi = (root: Element): DescriptorOpenApi | undefined => {
    const element = directChild(root, 'openapi')
    if (!element) {
        return undefined
    }
    const openapi: DescriptorOpenApi = {}
    const path = childValue(element, 'path')
    const mode = childValue(element, 'mode').toUpperCase()
    const model = childValue(element, 'model-module-name')
    const algorithm = childValue(element, 'algorithm-module-name')
    if (path) {
        openapi.path = path
    }
    if (mode === 'RECONCILIATION' || mode === 'GENERATION') {
        openapi.mode = mode
    }
    if (model) {
        openapi.modelModuleName = model
    }
    if (algorithm) {
        openapi.algorithmModuleName = algorithm
    }
    return Object.keys(openapi).length > 0 ? openapi : undefined
}

/**
 * Reads the declared text of a `rules.xml`. A blank or unreadable file reads as an empty descriptor —
 * the overview shows nothing for it rather than an error, since the resolved values still come from the
 * project response.
 */
export const parseRulesDescriptor = (xml: string): RulesDescriptor => {
    const root = parseXmlRoot(xml, ROOT)
    if (!root) {
        return EMPTY_RULES_DESCRIPTOR
    }
    const processor = childValue(root, 'properties-file-name-processor')
    const exposedMethods = readExposedMethods(root)
    const openapi = readOpenApi(root)
    return {
        description: childValue(root, 'comment'),
        versionPatterns: childValues(root, 'properties-file-name-pattern'),
        sources: readSources(root),
        moduleDeclarations: readModuleDeclarations(root),
        dependencies: readDependencies(root),
        ...(processor ? { propertiesFileNameProcessor: processor } : {}),
        ...(exposedMethods ? { exposedMethods } : {}),
        ...(openapi ? { openapi } : {}),
    }
}

const scalar = (tag: string, value: string | undefined): string[] => {
    const trimmed = value?.trim()
    return trimmed ? [`    <${tag}>${escapeXml(trimmed)}</${tag}>`] : []
}

const clean = (values: string[]): string[] => values.map(value => value.trim()).filter(Boolean)

const exposedMethodsXml = (filter: MethodFilter | undefined): string[] => {
    const includes = clean(filter?.includes ?? [])
    const excludes = clean(filter?.excludes ?? [])
    if (includes.length === 0 && excludes.length === 0) {
        return []
    }
    return [
        '    <exposed-methods>',
        ...includes.map(value => `        <include>${escapeXml(value)}</include>`),
        ...excludes.map(value => `        <exclude>${escapeXml(value)}</exclude>`),
        '    </exposed-methods>',
    ]
}

const methodFilterXml = (filter: MethodFilter | undefined, indent: string): string[] => {
    const includes = clean(filter?.includes ?? [])
    const excludes = clean(filter?.excludes ?? [])
    if (includes.length === 0 && excludes.length === 0) {
        return []
    }
    return [
        `${indent}<method-filter>`,
        ...(includes.length > 0 ? [`${indent}    <includes>`, ...includes.map(v => `${indent}        <value>${escapeXml(v)}</value>`), `${indent}    </includes>`] : []),
        ...(excludes.length > 0 ? [`${indent}    <excludes>`, ...excludes.map(v => `${indent}        <value>${escapeXml(v)}</value>`), `${indent}    </excludes>`] : []),
        `${indent}</method-filter>`,
    ]
}

const modulesXml = (modules: ModuleDeclaration[]): string[] => {
    // A blank row the user added and left empty is dropped; a module the file declared is kept even
    // without a path, so its preserved parts (a webstudio-configuration, say) are not lost.
    const declared = modules.filter(module => module.path.trim() || module.name.trim() || module.preserved?.length)
    if (declared.length === 0) {
        return []
    }
    return [
        '    <modules>',
        ...declared.flatMap(module => [
            '        <module>',
            ...(module.name.trim() ? [`            <name>${escapeXml(module.name.trim())}</name>`] : []),
            ...(module.path.trim() ? [`            <rules-root path="${escapeXml(module.path.trim())}"/>`] : []),
            ...methodFilterXml(module.methodFilter, '            '),
            ...(module.preserved ?? []).map(child => `            ${child}`),
            '        </module>',
        ]),
        '    </modules>',
    ]
}

const dependenciesXml = (dependencies: DeclaredDependency[]): string[] => {
    const declared = dependencies.filter(dependency => dependency.name.trim())
    if (declared.length === 0) {
        return []
    }
    return [
        '    <dependencies>',
        ...declared.flatMap(dependency => [
            '        <dependency>',
            `            <name>${escapeXml(dependency.name.trim())}</name>`,
            ...(dependency.autoIncluded ? ['            <autoIncluded>true</autoIncluded>'] : []),
            ...(dependency.mavenArtifact?.trim() ? [`            <mavenArtifact>${escapeXml(dependency.mavenArtifact.trim())}</mavenArtifact>`] : []),
            '        </dependency>',
        ]),
        '    </dependencies>',
    ]
}

const openApiXml = (openapi: DescriptorOpenApi | undefined): string[] => {
    // A mode without a file or a module says nothing, so it is not worth an element of its own.
    if (!openapi || ![openapi.path, openapi.modelModuleName, openapi.algorithmModuleName].some(value => value?.trim())) {
        return []
    }
    const lines = [
        ...scalar('path', openapi.path).map(line => `    ${line}`),
        ...scalar('model-module-name', openapi.modelModuleName).map(line => `    ${line}`),
        ...scalar('algorithm-module-name', openapi.algorithmModuleName).map(line => `    ${line}`),
        ...scalar('mode', openapi.mode).map(line => `    ${line}`),
    ]
    return lines.length > 0 ? ['    <openapi>', ...lines, '    </openapi>'] : []
}

/**
 * Writes the edited descriptor back to `rules.xml`. The managed elements — the modules, the classpath, the
 * dependencies and the other declared text — are rewritten from the edited model; every other project-level
 * element is carried over from the original document verbatim. Within each rebuilt module the parts the
 * overview does not manage are carried over too, so nothing the editor does not touch is ever lost.
 *
 * The managed elements are written first, in a stable order, then the preserved ones. A blank or
 * unreadable original starts from a bare {@code <project>}.
 */
export const serializeRulesDescriptor = (descriptor: RulesDescriptor, originalXml: string): string => {
    const preserved = preservedChildren(originalXml, ROOT, MANAGED)

    const sources = clean(descriptor.sources)
    const classpath = sources.length > 0
        ? ['    <classpath>', ...sources.map(p => `        <entry path="${escapeXml(p)}"/>`), '    </classpath>']
        : []
    const managed = [
        ...scalar('comment', descriptor.description),
        ...modulesXml(descriptor.moduleDeclarations),
        ...classpath,
        ...clean(descriptor.versionPatterns).map(p => `    <properties-file-name-pattern>${escapeXml(p)}</properties-file-name-pattern>`),
        ...scalar('properties-file-name-processor', descriptor.propertiesFileNameProcessor),
        ...dependenciesXml(descriptor.dependencies),
        ...exposedMethodsXml(descriptor.exposedMethods),
        ...openApiXml(descriptor.openapi),
    ]

    return `${['<project>', ...managed, ...preserved, '</project>'].join('\n')}\n`
}
