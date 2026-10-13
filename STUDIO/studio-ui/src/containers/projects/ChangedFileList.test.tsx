import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ChangedFileList, FilePath } from './ChangedFileList'
import type { ProjectFileChange } from '../../services/projectStatus'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return {
        useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }),
    }
})

const files: ProjectFileChange[] = [
    { path: 'rules/Main.xlsx', type: 'modified' },
    { path: 'rules/New.xlsx', type: 'added' },
    { path: 'notes.txt', type: 'deleted' },
]

describe('ChangedFileList', () => {
    it('marks each file with what became of it', () => {
        render(<ChangedFileList files={files} testId="changes" />)

        const rows = screen.getAllByTestId('changes-file')
        expect(rows).toHaveLength(3)
        expect(rows[0]).toHaveTextContent('rules/Main.xlsx')
        expect(within(rows[0]!).getByLabelText('repository:browser.files.change.modified')).toBeInTheDocument()
        expect(within(rows[1]!).getByLabelText('repository:browser.files.change.added')).toBeInTheDocument()
        expect(within(rows[2]!).getByLabelText('repository:browser.files.change.deleted')).toBeInTheDocument()
    })

    it('compares a modified file only: an added or a deleted one has a single version', async () => {
        const open = vi.fn()
        render(<ChangedFileList compare={{ hint: 'hint', open }} files={files} testId="changes" />)

        const compare = screen.getAllByTestId('changes-compare')
        expect(compare).toHaveLength(1)
        await userEvent.click(compare[0]!)

        expect(open).toHaveBeenCalledWith(files[0])
    })

    it('offers no comparison when the list is not told how to compare', () => {
        render(<ChangedFileList files={files} testId="changes" />)

        expect(screen.queryByTestId('changes-compare')).not.toBeInTheDocument()
    })

    it('tells the details of a file under its row', () => {
        render(<ChangedFileList details={file => <span>{`details of ${file.path}`}</span>} files={files} testId="changes" />)

        expect(screen.getByText('details of rules/Main.xlsx')).toBeInTheDocument()
    })
})

describe('FilePath', () => {
    it('writes a path whole, its folder and the name of its file', () => {
        const { container } = render(<FilePath path="DESIGN/rules/Pricing/rules/Main.xlsx" />)

        expect(container).toHaveTextContent('DESIGN/rules/Pricing/rules/Main.xlsx')
    })

    it('writes a file at the root of the repository as it is', () => {
        const { container } = render(<FilePath path="README.md" />)

        expect(container).toHaveTextContent('README.md')
    })
})
