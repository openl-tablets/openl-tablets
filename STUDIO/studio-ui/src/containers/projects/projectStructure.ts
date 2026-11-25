import type { Project, ProjectModule } from '../../types/projects'
import { BUNDLE_FOLDER } from '../../services/localization'
import { basename } from './projectPaths'

/**
 * The part a folder plays in a rules project: it holds the modules, the test modules, the code the rules run or the
 * libraries they use, or the messages of the project.
 */
export type FolderRole = 'rules' | 'tests' | 'sources' | 'libraries' | 'messages'

/** The folders of a project that play a part in it, by their path in the project. */
export type ProjectStructure = ReadonlyMap<string, FolderRole>

/** Where the engine looks for the modules when `rules.xml` declares none. */
const DEFAULT_MODULES: ProjectModule[] = [{ path: 'rules/**/*.xlsx' }, { path: 'tests/**/*.xlsx' }]

/** What the engine puts on the classpath when `rules.xml` declares nothing. */
const DEFAULT_SOURCES = ['groovy/', 'lib/*.jar']

const WILDCARD = /[*?[{]/

/** The segments of a path of `rules.xml`, written with either separator, without a leading `./`. */
const segmentsOf = (path: string): string[] =>
    path.replaceAll('\\', '/').split('/').filter(segment => segment !== '' && segment !== '.')

/**
 * The folder a path of `rules.xml` starts from: the folders before the first one with a wildcard, or the folder of
 * the file the path names. Empty for the root of the project.
 */
const rootOf = (path: string): string => {
    const segments = segmentsOf(path)
    const wildcard = segments.findIndex(segment => WILDCARD.test(segment))
    return (wildcard < 0 ? segments.slice(0, -1) : segments.slice(0, wildcard)).join('/')
}

/** Whether a classpath entry names jars rather than a folder of classes or scripts. */
const namesJars = (entry: string): boolean => entry.toLowerCase().endsWith('.jar')

/** The folder a classpath entry names: the folder of its jars, or the folder itself. */
const classpathRootOf = (entry: string): string =>
    (namesJars(entry) || WILDCARD.test(entry) ? rootOf(entry) : segmentsOf(entry).join('/'))

/** A folder of modules is a folder of tests when it is named so, as the engine's own `tests` is. */
const moduleRole = (root: string): FolderRole => (/^tests?$/i.test(basename(root)) ? 'tests' : 'rules')

/**
 * The folders that play a part in a project, as its `rules.xml` declares them.
 *
 * <p>The folders of the modules are those the module paths start from: a pattern stands for its folder, not for the
 * folders of the modules it matched. A folder of modules named `test` or `tests` holds tests. The classpath names the
 * folders of the code the rules run, and the folders of the jars they use. The messages are read from `i18n`.
 *
 * <p>A project whose `rules.xml` declares no modules, or no classpath, takes the engine's defaults: `rules` and
 * `tests`, `groovy` and `lib`; so does a project whose `rules.xml` could not be read. A folder keeps the first part
 * found for it, and the root of the project plays none.
 */
export const structureOf = (descriptor: Project['descriptor']): ProjectStructure => {
    const structure = new Map<string, FolderRole>()
    const add = (folder: string, role: FolderRole) => {
        if (folder !== '' && !structure.has(folder)) {
            structure.set(folder, role)
        }
    }
    const modules = descriptor?.modules?.length ? descriptor.modules : DEFAULT_MODULES
    for (const module of modules) {
        if (module.path) {
            const root = rootOf(module.path)
            add(root, moduleRole(root))
        }
    }
    const sources = descriptor?.sources?.length ? descriptor.sources : DEFAULT_SOURCES
    for (const entry of sources) {
        add(classpathRootOf(entry), namesJars(entry) ? 'libraries' : 'sources')
    }
    add(BUNDLE_FOLDER, 'messages')
    return structure
}
