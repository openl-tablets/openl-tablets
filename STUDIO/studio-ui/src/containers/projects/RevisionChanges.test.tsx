import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RevisionChanges } from './RevisionChanges'
import { getRevisionChanges, type ProjectRevision } from '../../services/repositories'
import { openFileRevisionsCompareWindow } from './compare'
import type { ProjectFileChange } from '../../services/projectStatus'

vi.mock('../../services/repositories', () => ({ getRevisionChanges: vi.fn() }))
vi.mock('./compare', () => ({ openFileRevisionsCompareWindow: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return {
        useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }),
    }
})

/** A revision as the history tells it. */
const revisionOf = (revisionNo: string): ProjectRevision => ({
    revisionNo,
    shortRevisionNo: null,
    createdAt: '2026-10-30T10:00:00Z',
    fullComment: `Revision ${revisionNo}`,
    deleted: false,
    technicalRevision: false,
})

const files = (count: number): ProjectFileChange[] =>
    Array.from({ length: count }, (_, i) => ({ path: `rules/File${String(i + 1).padStart(2, '0')}.xlsx`, type: 'modified' }))

describe('RevisionChanges', () => {
    it('reads the files the revision changed against the revision it was made on', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({ base: revisionOf('r1'), revision: revisionOf('r2'), files: files(2) })

        render(<RevisionChanges branch="main" projectId="p1" revision="r2" />)

        expect(await screen.findAllByTestId('revision-changes-file')).toHaveLength(2)
        expect(getRevisionChanges).toHaveBeenCalledWith('p1', 'r2', undefined)
    })

    it('shows ten files a page and pages through the rest', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({ base: revisionOf('r1'), revision: revisionOf('r2'), files: files(25) })

        render(<RevisionChanges projectId="p1" revision="r2" />)

        const rows = await screen.findAllByTestId('revision-changes-file')
        expect(rows).toHaveLength(10)
        expect(rows[0]).toHaveTextContent('rules/File01.xlsx')

        await userEvent.click(screen.getByTitle('3'))

        await waitFor(() => expect(screen.getAllByTestId('revision-changes-file')).toHaveLength(5))
        expect(screen.getAllByTestId('revision-changes-file')[0]).toHaveTextContent('rules/File21.xlsx')
    })

    it('offers no pages when the files fit on one', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({ base: revisionOf('r1'), revision: revisionOf('r2'), files: files(10) })

        render(<RevisionChanges projectId="p1" revision="r2" />)

        expect(await screen.findAllByTestId('revision-changes-file')).toHaveLength(10)
        expect(screen.queryByTitle('2')).not.toBeInTheDocument()
    })

    it('compares a modified file before and after the revision in the comparison window', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({
            base: revisionOf('r1'),
            revision: revisionOf('r2'),
            files: [{ path: 'rules/Main.xlsx', type: 'modified' }],
        })

        render(<RevisionChanges branch="main" projectId="p1" revision="r2" />)
        await userEvent.click(await screen.findByTestId('revision-changes-compare'))

        expect(openFileRevisionsCompareWindow)
            .toHaveBeenCalledWith('p1', 'rules/Main.xlsx', 'main', 'r1', 'r2', 'modified')
    })

    it('compares an added and a deleted file too, against nothing on the side that does not hold it', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({
            base: revisionOf('r1'),
            revision: revisionOf('r2'),
            files: [{ path: 'rules/New.xlsx', type: 'added' }, { path: 'notes.txt', type: 'deleted' }],
        })

        render(<RevisionChanges branch="main" projectId="p1" revision="r2" />)
        const compare = await screen.findAllByTestId('revision-changes-compare')
        await userEvent.click(compare[0]!)
        await userEvent.click(compare[1]!)

        expect(openFileRevisionsCompareWindow)
            .toHaveBeenCalledWith('p1', 'rules/New.xlsx', 'main', 'r1', 'r2', 'added')
        expect(openFileRevisionsCompareWindow)
            .toHaveBeenCalledWith('p1', 'notes.txt', 'main', 'r1', 'r2', 'deleted')
    })

    it('compares with the base it was given', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({ base: revisionOf('r1'), revision: revisionOf('r5'), files: []})

        render(<RevisionChanges base="r1" projectId="p1" revision="r5" />)

        await waitFor(() => expect(getRevisionChanges).toHaveBeenCalledWith('p1', 'r5', 'r1'))
    })

    it('says that the revision changed no file of the project', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({ base: revisionOf('r1'), revision: revisionOf('r2'), files: []})

        render(<RevisionChanges projectId="p1" revision="r2" />)

        expect(await screen.findByText('browser.history.no_changes')).toBeInTheDocument()
    })

    it('shows a file the first revision added against nothing, as there is no revision before it', async () => {
        vi.mocked(getRevisionChanges).mockResolvedValue({
            base: null,
            revision: revisionOf('r1'),
            files: [{ path: 'rules.xml', type: 'added' }],
        })

        render(<RevisionChanges projectId="p1" revision="r1" />)
        await userEvent.click(await screen.findByTestId('revision-changes-compare'))

        expect(openFileRevisionsCompareWindow).toHaveBeenCalledWith('p1', 'rules.xml', undefined, null, 'r1', 'added')
    })

    it('says that the files could not be read', async () => {
        vi.mocked(getRevisionChanges).mockRejectedValue(new Error('boom'))

        render(<RevisionChanges projectId="p1" revision="r2" />)

        expect(await screen.findByText('browser.history.changes_error')).toBeInTheDocument()
    })
})
