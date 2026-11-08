import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { checkMessageFiles } from '../../services/localization'
import type { FilePlace, ProjectStatusDetailedMessage } from '../../services/projectStatus'
import { useMessageFileProblems } from './useMessageFileProblems'

vi.mock('../../services/localization', async importOriginal => ({
    ...(await importOriginal<typeof import('../../services/localization')>()),
    checkMessageFiles: vi.fn(),
}))

const message = (id: number, path: string, places?: FilePlace[]): ProjectStatusDetailedMessage => ({
    id,
    summary: `problem ${id}`,
    severity: 'WARN',
    stacktrace: false,
    location: { type: 'localization', path, line: 1, ...(places === undefined ? {} : { places }) },
})

describe('useMessageFileProblems', () => {
    beforeEach(() => {
        vi.mocked(checkMessageFiles).mockReset()
    })

    it('marks each problem of the file at every place it is in, and one about the whole file nowhere', async () => {
        vi.mocked(checkMessageFiles).mockResolvedValue([
            message(1, 'i18n/message_fr.properties', [
                { line: 2, column: 4, endLine: 2, endColumn: 5 },
                { line: 2, column: 9, endLine: 2, endColumn: 10 },
            ]),
            message(2, 'i18n/message.properties', [{ line: 1, column: 0, endLine: 1, endColumn: 3 }]),
            message(3, 'i18n/message_fr.properties'),
        ])

        const { result } = renderHook(() => useMessageFileProblems('p1', 'i18n/message_fr.properties', 'text'))

        await waitFor(() => expect(result.current).toBeDefined())
        // An editor of the file marks the places of the file.
        expect(checkMessageFiles).toHaveBeenCalledWith('p1', 'file')
        // The problems of the other files are theirs.
        expect(result.current).toEqual([
            { message: 'problem 1', severity: 'warning', place: { line: 2, column: 4, endLine: 2, endColumn: 5 } },
            { message: 'problem 1', severity: 'warning', place: { line: 2, column: 9, endLine: 2, endColumn: 10 } },
            { message: 'problem 3', severity: 'warning' },
        ])
    })

    it('checks the files again when the text the file holds changes', async () => {
        vi.mocked(checkMessageFiles).mockResolvedValue([])
        const { rerender } = renderHook(({ text }) => useMessageFileProblems('p1', 'i18n/message.properties', text),
            { initialProps: { text: 'a = b' } })
        await waitFor(() => expect(checkMessageFiles).toHaveBeenCalledTimes(1))

        rerender({ text: 'a = c' })

        await waitFor(() => expect(checkMessageFiles).toHaveBeenCalledTimes(2))
    })

    it('asks nothing while the text of the file is read, and once it is', async () => {
        vi.mocked(checkMessageFiles).mockResolvedValue([])
        const { rerender, result } = renderHook(
            ({ text }: { text: string | null }) => useMessageFileProblems('p1', 'i18n/message.properties', text),
            { initialProps: { text: null as string | null } })

        expect(result.current).toBeUndefined()
        expect(checkMessageFiles).not.toHaveBeenCalled()

        rerender({ text: 'a = b' })

        await waitFor(() => expect(checkMessageFiles).toHaveBeenCalledTimes(1))
    })

    it('gives no problems for a file that is no localization file, and asks nothing for it', () => {
        const { result } = renderHook(() => useMessageFileProblems('p1', 'rules.xml', '<project/>'))

        expect(result.current).toBeUndefined()
        expect(checkMessageFiles).not.toHaveBeenCalled()
    })

    it('marks nothing when the check fails', async () => {
        vi.mocked(checkMessageFiles).mockRejectedValue(new Error('down'))

        const { result } = renderHook(() => useMessageFileProblems('p1', 'i18n/message.properties', 'a = b'))

        await waitFor(() => expect(result.current).toEqual([]))
    })
})
