import { describe, expect, it } from 'vitest'
import { fileName, FORMATS, formatOf, kindOf, NAMED_FORMATS } from './fileFormats'

describe('fileFormats', () => {
    it('lists every extension once, so no format is both text and binary', () => {
        const extensions = Object.values(FORMATS).flatMap(groups => Object.values(groups).flat())

        expect(extensions.filter((extension, index) => extensions.indexOf(extension) !== index)).toEqual([])
        expect(extensions.every(extension => extension === extension.toLowerCase())).toBe(true)
    })

    it('knows the textual formats of a rules project and of the code beside it', () => {
        for (const path of ['notes.txt', 'docs/README.md', 'rules.xml', 'openapi.json', 'Main.java', 'scripts/run.groovy', 'bin/start.sh', 'bin/start.cmd', 'bin/start.bat', 'web/app.ts', 'web/app.js', 'i18n/message.properties', 'config.yaml', 'config.yml', 'pyproject.toml', 'data.csv']) {
            expect(kindOf(path)).toBe('text')
        }
    })

    it('knows the binary formats', () => {
        for (const path of ['lib/rates.zip', 'setup.exe', 'backup.7z', 'data.bin', 'Handbook.doc', 'Handbook.docx', 'rules/Main.xlsx', 'Guide.pdf', 'lib/engine.jar', 'Rate.class']) {
            expect(kindOf(path)).toBe('binary')
        }
    })

    it('knows the pictures a browser draws, and keeps the others binary', () => {
        for (const path of ['a.gif', 'a.jpg', 'a.jpeg', 'a.png', 'a.svg', 'a.webp', 'a.bmp', 'a.ico']) {
            expect(kindOf(path)).toBe('image')
        }
        expect(formatOf('scan.tiff')).toEqual({ kind: 'binary', icon: 'image' })
    })

    it('reads the extension in any case and the name after the last separator', () => {
        expect(formatOf('Rules/MAIN.XLSX')).toEqual({ kind: 'binary', icon: 'spreadsheet' })
        expect(formatOf('C:\\work\\Logo.PNG')).toEqual({ kind: 'image', icon: 'image' })
        expect(formatOf('archive.tar.gz')?.icon).toBe('archive')
    })

    it('knows a file without an extension by its whole name, and a dot file by what follows the dot', () => {
        expect(formatOf('docker/Dockerfile')).toEqual({ kind: 'text', icon: NAMED_FORMATS['dockerfile'] })
        expect(kindOf('LICENSE')).toBe('text')
        expect(kindOf('.gitignore')).toBe('text')
        expect(kindOf('.editorconfig')).toBe('text')
    })

    it('leaves a format it does not know unknown', () => {
        expect(formatOf('notes.rates')).toBeUndefined()
        expect(formatOf('UNTITLED')).toBeUndefined()
        expect(formatOf('folder/')).toBeUndefined()
    })

    it('knows no file by a name the list inherits rather than lists', () => {
        expect(formatOf('constructor')).toBeUndefined()
        expect(formatOf('docs/Constructor')).toBeUndefined()
        expect(formatOf('__proto__')).toBeUndefined()
        expect(formatOf('constructor.txt')).toEqual({ kind: 'text', icon: 'text' })
    })

    it('knows a file by its whole name before its extension', () => {
        expect(formatOf('module/pom.xml')).toEqual({ kind: 'text', icon: 'maven' })
        expect(formatOf('tags.properties')).toEqual({ kind: 'text', icon: 'tag' })
        expect(formatOf('rules.xml')).toEqual({ kind: 'text', icon: 'xml' })
    })

    it('names the file of a path written with either separator', () => {
        expect(fileName('rules/Main.xlsx')).toBe('Main.xlsx')
        expect(fileName('C:\\work\\Main.xlsx')).toBe('Main.xlsx')
        expect(fileName('Main.xlsx')).toBe('Main.xlsx')
    })
})
