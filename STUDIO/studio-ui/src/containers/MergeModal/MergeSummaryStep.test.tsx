import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MergeSummaryStep } from 'containers/MergeModal/MergeSummaryStep'
import type { MergeSummary } from 'containers/MergeModal/types'
import { openFileRevisionsCompareWindow } from 'containers/projects/compare'

vi.mock('react-i18next', () => {
    const t = (key: string, options?: Record<string, unknown>) =>
        options ? `${key}:${JSON.stringify(options)}` : key
    return {
        useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }),
    }
})

vi.mock('containers/projects/compare', () => ({
    openFileRevisionsCompareWindow: vi.fn(),
}))

const summary: MergeSummary = {
    sourceBranch: 'feature',
    targetBranch: 'main',
    before: 'b1',
    after: 'a1',
    projects: [
        {
            id: 'cHJpY2luZw',
            name: 'Pricing',
            path: 'DESIGN/rules/Pricing',
            current: true,
            status: 'modified',
            files: [
                {
                    path: 'DESIGN/rules/Pricing/rules/Main.xlsx',
                    type: 'modified',
                    sheets: [
                        { name: 'Rates', takenFrom: 'source' },
                        { name: 'Taxes', takenFrom: 'target' },
                        { name: 'Rules', takenFrom: 'both' },
                    ],
                },
                { path: 'DESIGN/rules/Pricing/rules.xml', type: 'modified' },
                { path: 'DESIGN/rules/Pricing/rules/New.xlsx', type: 'added' },
            ],
        },
        {
            current: false,
            files: [{ path: 'README.md', type: 'deleted' }],
        },
    ],
    hiddenProjects: 2,
}

describe('MergeSummaryStep', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('says which branch was merged into which', () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={summary} />)

        expect(screen.getByTestId('merge-summary-result'))
            .toHaveTextContent('merge:summary.merged:{"source":"feature","target":"main"}')
    })

    it('says the conflicts were resolved when the merge ended with resolving them', () => {
        render(<MergeSummaryStep resolved onClose={vi.fn()} summary={summary} />)

        expect(screen.getByTestId('merge-summary-result')).toHaveTextContent('merge:summary.resolved')
    })

    it('lists the files of the current project by their path from the root of the repository', () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={summary} />)

        const files = screen.getAllByTestId('merge-summary-file')
        expect(files).toHaveLength(3)
        expect(within(files[0]!).getByText('Main.xlsx')).toBeInTheDocument()
        expect(within(files[0]!).getByText('DESIGN/rules/Pricing/rules')).toBeInTheDocument()
        expect(within(files[2]!).getByLabelText('repository:browser.files.change.added')).toBeInTheDocument()
    })

    it('tells which branch each sheet of a workbook merged automatically was taken from', () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={summary} />)

        const sheets = screen.getByTestId('merge-summary-sheets')
        expect(within(sheets).getByText('merge:summary.sheet_from:{"sheet":"Rates","branch":"feature"}'))
            .toBeInTheDocument()
        expect(within(sheets).getByText('merge:summary.sheet_from:{"sheet":"Taxes","branch":"main"}'))
            .toBeInTheDocument()
        expect(within(sheets).getByText('merge:summary.sheet_both:{"sheet":"Rules"}')).toBeInTheDocument()
    })

    it('compares a modified file before and after the merge', async () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={summary} />)

        // The workbook and the descriptor were modified; an added file has no past to compare with.
        const compare = screen.getAllByTestId('merge-summary-compare')
        expect(compare).toHaveLength(2)
        await userEvent.click(compare[0]!)
        await userEvent.click(compare[1]!)

        expect(openFileRevisionsCompareWindow).toHaveBeenCalledWith('cHJpY2luZw', 'rules/Main.xlsx', 'main', 'b1', 'a1')
        expect(openFileRevisionsCompareWindow).toHaveBeenCalledWith('cHJpY2luZw', 'rules.xml', 'main', 'b1', 'a1')
    })

    it('counts the projects the user may not read', () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={summary} />)

        expect(screen.getByTestId('merge-summary-hidden')).toHaveTextContent('merge:scope.hidden:{"count":2}')
    })

    it('names the files outside projects as a group of their own', () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={summary} />)

        expect(screen.getByText('merge:scope.outside')).toBeInTheDocument()
    })

    it('says the merge changed nothing the user can read', () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={{ ...summary, projects: [], hiddenProjects: 0 }} />)

        expect(screen.getByText('merge:summary.nothing')).toBeInTheDocument()
    })

    it('says the merge changed only projects the user cannot read, in one sentence', () => {
        render(<MergeSummaryStep onClose={vi.fn()} resolved={false} summary={{ ...summary, projects: []}} />)

        expect(screen.getByText('merge:summary.hidden_only:{"count":2}')).toBeInTheDocument()
        expect(screen.queryByText('merge:summary.nothing')).not.toBeInTheDocument()
        expect(screen.queryByTestId('merge-summary-hidden')).not.toBeInTheDocument()
    })

    it('closes the dialog', async () => {
        const onClose = vi.fn()
        render(<MergeSummaryStep onClose={onClose} resolved={false} summary={summary} />)

        await userEvent.click(screen.getByTestId('merge-summary-close'))

        expect(onClose).toHaveBeenCalled()
    })
})
