import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import i18n from '../i18n'
import '.'

// Component tests mock `t`, so a lookup of a key that no bundle defines passes them and renders the
// key itself in the UI (EPBDS-16602). Every literal lookup in the sources is resolved here against
// the registered bundles instead.

const SRC = join(import.meta.dirname, '..')

/** A `t('ns:key')` or `i18nKey="ns:key"` literal; the namespace travels inside the key. */
const QUALIFIED = /(?:\bt\(\s*|i18nKey=\{?)(['"])([a-z0-9_]+:[^'"`]+)\1/g
/** A `t('key')` or `i18nKey="key"` literal that relies on the namespace the file passes to `useTranslation`. */
const UNQUALIFIED = /(?:\bt\(\s*|i18nKey=\{?)(['"])([^'"`:]+)\1/g
const DECLARED_NS = /useTranslation\(\s*'([a-z0-9_]+)'\s*\)/g

interface Lookup {
    file: string
    key: string
    /** Namespaces to search when the key carries none. */
    ns: string[]
}

const sourceFiles = (dir: string): string[] =>
    readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
        const path = join(dir, entry.name)
        if (entry.isDirectory()) return entry.name === 'locales' ? [] : sourceFiles(path)
        return /\.tsx?$/.test(entry.name) && !/\.(test|d)\.tsx?$/.test(entry.name) ? [path] : []
    })

const captures = (source: string, regex: RegExp, group: number) =>
    [...source.matchAll(regex)].flatMap(match => match[group] ?? [])

const registeredNamespaces = () => Object.keys(i18n.getDataByLanguage('en') ?? {})

/** A file that declares no namespace receives `t` from a caller, so its keys may live in any bundle. */
const lookupsIn = (file: string, allNamespaces: string[]): Lookup[] => {
    const source = readFileSync(file, 'utf8')
    const declared = captures(source, DECLARED_NS, 1)
    const ns = declared.length > 0 ? declared : allNamespaces
    return [
        ...captures(source, QUALIFIED, 2).map(key => ({ file, key, ns: []})),
        ...captures(source, UNQUALIFIED, 2).map(key => ({ file, key, ns })),
    ]
}

const resolves = ({ key, ns }: Lookup) => i18n.exists(key, { ns }) || i18n.exists(key, { ns, count: 1 })

const describeMissing = ({ file, key }: Lookup) => `${file.slice(SRC.length + 1)}: ${key}`

describe('translation lookups', () => {
    const allNamespaces = registeredNamespaces()
    const lookups = sourceFiles(SRC).flatMap(file => lookupsIn(file, allNamespaces))

    it('scans the sources', () => {
        expect(lookups.length).toBeGreaterThan(500)
    })

    it('resolve a namespace-qualified key in that bundle', () => {
        const missing = lookups.filter(lookup => lookup.key.includes(':') && !resolves(lookup))

        expect(missing.map(describeMissing)).toEqual([])
    })

    it('resolve a bare key in a namespace the file declares', () => {
        const missing = lookups.filter(lookup => !lookup.key.includes(':') && !resolves(lookup))

        expect(missing.map(describeMissing)).toEqual([])
    })
})
