import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import type { Rolldown } from 'vite'
import { libraryNotices } from './vite.config'

const LIST = 'licenses/frontend-licenses.json'

/** Runs the plugin over a bundle of the modules given, holding the list as Vite writes it, and reads the list back. */
const noticesOf = (modules: string[], libraries: object[]) => {
    const bundle = {
        [LIST]: { type: 'asset', source: JSON.stringify(libraries) },
        'index.js': { type: 'chunk', moduleIds: modules },
    } as unknown as Rolldown.OutputBundle
    const { handler } = libraryNotices().generateBundle as { handler: (options: unknown, bundle: unknown) => void }
    handler({}, bundle)
    return JSON.parse((bundle[LIST] as Rolldown.OutputAsset).source as string) as object[]
}

describe('libraryNotices', () => {
    let root: string

    /** Writes a package of the name and the version given, with the files given, and returns a module of it. */
    const writePackage = (folder: string, version: string, files: Record<string, string> = {}): string => {
        const dir = join(root, folder)
        mkdirSync(join(dir, 'lib'), { recursive: true })
        writeFileSync(join(dir, 'package.json'), JSON.stringify({ name: folder.split('node_modules/').pop(), version }))
        Object.entries(files).forEach(([file, text]) => writeFileSync(join(dir, file), text))
        return join(dir, 'lib', 'index.js')
    }

    beforeEach(() => {
        root = mkdtempSync(join(tmpdir(), 'library-notices-'))
    })

    afterEach(() => rmSync(root, { recursive: true, force: true }))

    it('adds the NOTICE each bundled package ships, by its name and version', () => {
        const apache = writePackage('node_modules/@scope/apache', '1.0.0', { 'NOTICE': ' Apache Notice\n' })
        const nested = writePackage('node_modules/@scope/apache/node_modules/inner', '1.0.0', { 'notice.md': 'Inner' })
        const top = writePackage('node_modules/inner', '2.0.0', { 'LICENSE': 'MIT License' })

        expect(noticesOf([apache, `${nested}?commonjs-es-import`, top, '\0virtual', join(root, 'src', 'app.ts')], [
            { name: '@scope/apache', version: '1.0.0', identifier: 'Apache-2.0' },
            { name: 'inner', version: '1.0.0', identifier: 'MIT' },
            { name: 'inner', version: '2.0.0', identifier: 'MIT' },
        ])).toEqual([
            { name: '@scope/apache', version: '1.0.0', identifier: 'Apache-2.0', notice: 'Apache Notice' },
            { name: 'inner', version: '1.0.0', identifier: 'MIT', notice: 'Inner' },
            { name: 'inner', version: '2.0.0', identifier: 'MIT' },
        ])
    })

    it('leaves a bundle without the list as it is', () => {
        const bundle = { 'index.js': { type: 'chunk', moduleIds: [] } } as unknown as Rolldown.OutputBundle
        const { handler } = libraryNotices().generateBundle as { handler: (options: unknown, bundle: unknown) => void }
        handler({}, bundle)

        expect(Object.keys(bundle)).toEqual(['index.js'])
    })
})
