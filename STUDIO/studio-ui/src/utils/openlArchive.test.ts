import { describe, expect, it } from 'vitest'
import { zipSync, strToU8, unzipSync } from 'fflate'
import { inspectOpenLArchive, zipProjectFolder } from './openlArchive'

/** Builds a File from a map of entry path → text content, as a real zip archive. */
const zipFile = (name: string, entries: Record<string, string>): File => {
    const data = Object.fromEntries(Object.entries(entries).map(([path, text]) => [path, strToU8(text)]))
    return new File([zipSync(data)], name, { type: 'application/zip' })
}

const RULES_XML = (projectName?: string) =>
    projectName === undefined ? '<project/>' : `<project><name>${projectName}</name></project>`

describe('inspectOpenLArchive', () => {
    it('takes the project name from a root rules.xml', async () => {
        const info = await inspectOpenLArchive(zipFile('archive.zip', {
            'rules.xml': RULES_XML('Pricing Rules'),
            'Main.xlsx': 'x',
        }))

        expect(info).toEqual({ readable: true, isOpenLProject: true, name: 'Pricing Rules' })
    })

    it('falls back to the archive file name when there is no rules.xml', async () => {
        const info = await inspectOpenLArchive(zipFile('My Project.zip', { 'Main.xlsx': 'x' }))

        expect(info).toEqual({ readable: true, isOpenLProject: true, name: 'My Project' })
    })

    it('falls back to the file name when rules.xml has no project name', async () => {
        const info = await inspectOpenLArchive(zipFile('Archived.zip', { 'rules.xml': RULES_XML() }))

        expect(info).toEqual({ readable: true, isOpenLProject: true, name: 'Archived' })
    })

    it('ignores __MACOSX junk when detecting Excel content at the root', async () => {
        const info = await inspectOpenLArchive(zipFile('proj.zip', {
            'Main.xlsx': 'x',
            '__MACOSX/._Main.xlsx': 'junk',
        }))

        expect(info.isOpenLProject).toBe(true)
    })

    it('flags an archive with neither rules.xml nor Excel as not an OpenL project', async () => {
        const info = await inspectOpenLArchive(zipFile('notes.zip', { 'readme.txt': 'hello' }))

        expect(info).toEqual({ readable: true, isOpenLProject: false, name: 'notes' })
    })

    it('accepts a project wrapped in a single top-level folder and names it from its rules.xml', async () => {
        const info = await inspectOpenLArchive(zipFile('wrapped.zip', {
            'MyProject/rules.xml': RULES_XML('Wrapped Rules'),
            'MyProject/Main.xlsx': 'x',
        }))

        expect(info).toEqual({ readable: true, isOpenLProject: true, name: 'Wrapped Rules' })
    })

    it('names a wrapped Excel-only project from its folder', async () => {
        const info = await inspectOpenLArchive(zipFile('archive.zip', { 'MyProject/Main.xlsx': 'x' }))

        expect(info).toEqual({ readable: true, isOpenLProject: true, name: 'MyProject' })
    })

    it('rejects an archive with two top-level folders as ambiguous', async () => {
        const info = await inspectOpenLArchive(zipFile('two.zip', {
            'A/rules.xml': RULES_XML('A'),
            'B/Main.xlsx': 'x',
        }))

        expect(info.isOpenLProject).toBe(false)
    })

    it('reports an unreadable file without throwing', async () => {
        const info = await inspectOpenLArchive(new File(['not a zip'], 'broken.zip'))

        expect(info).toEqual({ readable: false, isOpenLProject: false, name: 'broken' })
    })
})

/** A File that reports a folder-relative path, as a directory picker would. */
const folderFile = (relativePath: string, content: string): File => {
    const file = new File([content], relativePath.slice(relativePath.lastIndexOf('/') + 1))
    Object.defineProperty(file, 'webkitRelativePath', { value: relativePath })
    return file
}

describe('zipProjectFolder', () => {
    it('zips a picked folder into an archive the inspector accepts as a project', async () => {
        const zipped = await zipProjectFolder([
            folderFile('MyProj/rules.xml', RULES_XML('Folder Project')),
            folderFile('MyProj/rules/Main.xlsx', 'x'),
        ])

        expect(zipped.name).toBe('MyProj.zip')
        const info = await inspectOpenLArchive(zipped)
        expect(info).toEqual({ readable: true, isOpenLProject: true, name: 'Folder Project' })
    })

    it('strips the chosen folder so the project sits at the archive root', async () => {
        const zipped = await zipProjectFolder([
            folderFile('MyProj/rules.xml', RULES_XML('Folder Project')),
            folderFile('MyProj/rules/Main.xlsx', 'x'),
        ])

        // The server resolves the project at the zip root, so the wrapping folder must be gone.
        const names = Object.keys(unzipSync(new Uint8Array(await zipped.arrayBuffer())))
        expect(names.sort()).toEqual(['rules.xml', 'rules/Main.xlsx'])
    })

    it('keeps full paths when the files share no single top-level folder', async () => {
        const zipped = await zipProjectFolder([
            folderFile('Alpha/file.txt', 'a'),
            folderFile('Beta/file.txt', 'b'),
        ])

        // No common root to strip: nothing is cut and the archive falls back to the generic name.
        expect(zipped.name).toBe('project.zip')
        const names = Object.keys(unzipSync(new Uint8Array(await zipped.arrayBuffer())))
        expect(names.sort()).toEqual(['Alpha/file.txt', 'Beta/file.txt'])
    })

    it('flags a folder that is not an OpenL project, exactly like an uploaded archive', async () => {
        const zipped = await zipProjectFolder([folderFile('junk/notes.txt', 'nothing useful')])

        const info = await inspectOpenLArchive(zipped)
        expect(info.isOpenLProject).toBe(false)
    })
})
