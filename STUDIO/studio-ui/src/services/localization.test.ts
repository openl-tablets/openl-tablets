import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall from './apiCall'
import {
    applyMessageActions,
    checkMessageFiles,
    fetchMessageKeys,
    fetchMessages,
    isMessageFilePath,
    type MessageQuery,
} from './localization'

vi.mock('./apiCall', async (importOriginal) => {
    const original = await importOriginal<typeof import('./apiCall')>()
    return { ...original, default: vi.fn() }
})

const ANY: MessageQuery = { search: '', onlyProblems: false, notTranslatedInto: [], order: null }
const READ = { throwError: true, suppressErrorPages: true }

describe('localization', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('tells the localization files of a project from its other files', () => {
        expect(isMessageFilePath('i18n/message.properties')).toBe(true)
        expect(isMessageFilePath('i18n/message_de_LU.properties')).toBe(true)
        expect(isMessageFilePath('i18n/message_de__POSIX.properties')).toBe(true)
        // A name that holds no language tag is no localization file.
        expect(isMessageFilePath('i18n/message_backup.properties')).toBe(false)
        expect(isMessageFilePath('i18n/message_de_.properties')).toBe(false)
        expect(isMessageFilePath('i18n/messages.properties')).toBe(false)
        expect(isMessageFilePath('i18n/sub/message.properties')).toBe(false)
        expect(isMessageFilePath('message.properties')).toBe(false)
        expect(isMessageFilePath('i18n/message.txt')).toBe(false)
    })

    it('reads every key for the tree', async () => {
        vi.mocked(apiCall).mockResolvedValue([{ key: 'greeting' }, { key: 'total' }])

        await expect(fetchMessageKeys('P1')).resolves.toEqual(['greeting', 'total'])
        expect(apiCall).toHaveBeenLastCalledWith('/projects/P1/localization/keys', undefined, READ)
    })

    it('narrows a page down and orders it as the query asks', async () => {
        vi.mocked(apiCall).mockResolvedValue({})

        await fetchMessages('P1', { search: '  pay ', onlyProblems: true, notTranslatedInto: ['de_LU', 'fr'], order: 'desc' },
            { page: 0, size: 250 })
        expect(apiCall).toHaveBeenLastCalledWith(
            '/projects/P1/localization?search=pay&problems=true&untranslated=de_LU&untranslated=fr&sort=desc&page=0&size=250',
            undefined, READ)
    })

    it('reads a page of the messages from the root of the localization of the project', async () => {
        const page = { languages: [{ tag: '' }], content: [], pageNumber: 1, pageSize: 250, numberOfElements: 0 }
        vi.mocked(apiCall).mockResolvedValue(page)

        await expect(fetchMessages('P1', { ...ANY, search: 'total' }, { page: 1, size: 250 })).resolves.toBe(page)
        expect(apiCall).toHaveBeenCalledWith('/projects/P1/localization?search=total&page=1&size=250', undefined, READ)
    })

    it('reads a key alone, whatever the query', async () => {
        vi.mocked(apiCall).mockResolvedValue({})

        await fetchMessages('P1', { search: 'total', onlyProblems: true, notTranslatedInto: ['fr'], order: 'asc' },
            { key: 'a b&c', page: 0, size: 1 })
        expect(apiCall).toHaveBeenCalledWith('/projects/P1/localization?key=a+b%26c&page=0&size=1', undefined, READ)
    })

    it('sends the changes as one batch of actions', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)
        const actions = [
            { operation: 'setValue' as const, key: 'greeting', language: '', value: 'Hi' },
            { operation: 'addKey' as const, key: 'note', values: { '': 'A note' } },
        ]

        await applyMessageActions('P1', actions)
        expect(apiCall).toHaveBeenCalledWith('/projects/P1/localization/actions', {
            method: 'POST',
            body: JSON.stringify({ actions }),
            headers: { 'Content-Type': 'application/json' },
        }, { throwError: true })
    })

    it('checks the files with the places counted as asked', async () => {
        vi.mocked(apiCall).mockResolvedValue([])

        await expect(checkMessageFiles('P1', 'file')).resolves.toEqual([])
        expect(apiCall).toHaveBeenCalledWith('/projects/P1/localization/problems?positions=file', undefined, READ)
    })
})
