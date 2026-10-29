import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getConflictText } from '../../services/compare'
import { TextMergeEditor } from './TextMergeEditor'
import type { ConflictText } from './types'

vi.mock('../../services/compare', () => ({
    getConflictText: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string, options?: Record<string, unknown>) =>
        options ? `${key} ${Object.values(options).join('/')}` : key
    return { useTranslation: () => ({ t }) }
})

// CodeMirror draws nothing a test can read in jsdom, so a cell of the editor is a text box here, which holds a text
// with the line breaks of Unix as the editor does.
vi.mock('../projects/CodeEditor', () => ({
    CodeEditor: ({ firstLine, onChange, readOnly, value }: {
        firstLine?: number
        onChange?: (value: string) => void
        readOnly?: boolean
        value: string
    }) => (
        <textarea
            data-first-line={firstLine}
            data-testid={readOnly ? 'code-lines' : 'code-result'}
            onChange={event => onChange?.(event.target.value)}
            readOnly={readOnly}
            value={value.replace(/\r\n?/g, '\n')}
        />
    ),
}))

const TEXT: ConflictText = {
    blocks: [
        { kind: 'merged', text: 'a=1\n' },
        { kind: 'conflict', ours: 'b=ours\n', base: 'b=2\n', theirs: 'b=theirs\n' },
        { kind: 'merged', text: 'c=3\n' },
    ],
}

const renderEditor = (props: Partial<React.ComponentProps<typeof TextMergeEditor>> = {}) => {
    const onApply = vi.fn()
    const onCancel = vi.fn()
    render(
        <TextMergeEditor
            filePath="Project/i18n/message.properties"
            onApply={onApply}
            onCancel={onCancel}
            projectId="p1"
            {...props}
        />
    )
    return { onApply, onCancel }
}

/** The values of the read-only cells, column by column and part by part. */
const shownLines = () => screen.getAllByTestId('code-lines').map(cell => (cell as HTMLTextAreaElement).value)

/** The result of a conflict, as its cell holds it. */
const resultOf = (number: number) =>
    within(screen.getByTestId(`text-merge-result-${number}`)).getByTestId('code-result')

describe('TextMergeEditor', () => {
    beforeEach(() => {
        vi.mocked(getConflictText).mockResolvedValue(TEXT)
    })

    it('lays your version, the result and their version side by side, and applies the version taken', async () => {
        const { onApply } = renderEditor({ versionLabels: { ours: 'master', theirs: 'feature' } })

        const conflict = await screen.findByTestId('text-merge-conflict-1')
        expect(getConflictText).toHaveBeenCalledWith('p1', 'Project/i18n/message.properties')
        expect(screen.getByText('merge:editor.your_version')).toBeInTheDocument()
        expect(screen.getByText('master')).toBeInTheDocument()
        expect(screen.getByText('merge:editor.result')).toBeInTheDocument()
        expect(screen.getByText('feature')).toBeInTheDocument()
        // Lines no branch changed read alike in every column; the conflict shows yours left and theirs right.
        expect(shownLines()).toEqual(['a=1', 'a=1', 'a=1', 'b=ours', 'b=theirs', 'c=3', 'c=3', 'c=3'])
        // An unresolved conflict shows the base lines as its result, and keeps the file from being merged.
        expect(resultOf(1)).toHaveValue('b=2')
        expect(within(conflict).getByText('merge:editor.unresolved')).toBeInTheDocument()
        expect(screen.getByTestId('text-merge-progress')).toHaveTextContent('merge:editor.progress 0/1')
        expect(screen.getByTestId('text-merge-apply')).toBeDisabled()

        await userEvent.click(within(conflict).getByTestId('text-merge-take-theirs'))

        expect(resultOf(1)).toHaveValue('b=theirs')
        expect(within(conflict).getByText('merge:editor.status_theirs')).toBeInTheDocument()
        expect(within(conflict).getByTestId('text-merge-take-theirs')).toBeDisabled()
        expect(within(conflict).getByTestId('text-merge-take-ours')).toHaveTextContent('merge:editor.add_yours')
        expect(screen.getByTestId('text-merge-progress')).toHaveTextContent('merge:editor.progress 1/1')
        await userEvent.click(screen.getByTestId('text-merge-apply'))
        // The decisions go with the parts of the file they were made for.
        expect(onApply).toHaveBeenCalledWith({
            text: 'a=1\nb=theirs\nc=3\n',
            decisions: [{ choice: 'theirs', lines: 'b=theirs\n' }],
            blocks: TEXT.blocks,
        })
    })

    it('adds the other version after the one taken, and takes the base lines', async () => {
        const { onApply } = renderEditor()
        const conflict = await screen.findByTestId('text-merge-conflict-1')

        await userEvent.click(within(conflict).getByTestId('text-merge-take-ours'))
        await userEvent.click(within(conflict).getByTestId('text-merge-take-theirs'))

        expect(resultOf(1)).toHaveValue('b=ours\nb=theirs')
        expect(within(conflict).getByText('merge:editor.status_yours_then_theirs')).toBeInTheDocument()
        expect(within(conflict).getByTestId('text-merge-take-ours')).toBeDisabled()
        expect(within(conflict).getByTestId('text-merge-take-theirs')).toBeDisabled()
        await userEvent.click(screen.getByTestId('text-merge-apply'))
        expect(onApply).toHaveBeenLastCalledWith(expect.objectContaining({ text: 'a=1\nb=ours\nb=theirs\nc=3\n' }))

        await userEvent.click(within(conflict).getByTestId('text-merge-take-base'))

        expect(resultOf(1)).toHaveValue('b=2')
        expect(within(conflict).getByText('merge:editor.status_base')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('text-merge-apply'))
        expect(onApply).toHaveBeenLastCalledWith(expect.objectContaining({ text: 'a=1\nb=2\nc=3\n' }))

        // Taking a version after the base lines starts over from it.
        await userEvent.click(within(conflict).getByTestId('text-merge-take-theirs'))
        expect(resultOf(1)).toHaveValue('b=theirs')
    })

    it('takes the lines the user writes in the result, with the line breaks of the file', async () => {
        vi.mocked(getConflictText).mockResolvedValue({
            blocks: [
                { kind: 'merged', text: 'a=1\r\n' },
                { kind: 'conflict', ours: 'b=ours\r\n', base: 'b=2\r\n', theirs: 'b=theirs\r\n' },
                { kind: 'merged', text: 'c=3\r\n' },
            ],
        })
        const { onApply } = renderEditor()
        const conflict = await screen.findByTestId('text-merge-conflict-1')

        // The editor is given the line breaks it writes itself, so what it holds and what it is given agree.
        await userEvent.click(within(conflict).getByTestId('text-merge-take-ours'))
        await userEvent.click(within(conflict).getByTestId('text-merge-take-theirs'))
        expect(resultOf(1)).toHaveValue('b=ours\nb=theirs')

        // A conflict is resolved by writing its result as well.
        fireEvent.change(resultOf(1), { target: { value: 'b=mine\nb=too' } })

        expect(within(conflict).getByText('merge:editor.status_edited')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('text-merge-apply'))
        expect(onApply).toHaveBeenCalledWith(expect.objectContaining({
            text: 'a=1\r\nb=mine\r\nb=too\r\nc=3\r\n',
            decisions: [{ choice: 'edited', lines: 'b=mine\r\nb=too\r\n' }],
        }))
    })

    it('numbers the lines of each column as the version of the column numbers them', async () => {
        vi.mocked(getConflictText).mockResolvedValue({
            blocks: [
                { kind: 'merged', text: 'a=1\n' },
                { kind: 'conflict', ours: 'b=ours\nb2=ours\n', base: 'b=2\n', theirs: '' },
                { kind: 'merged', text: 'c=3\n' },
            ],
        })
        renderEditor()
        const conflict = await screen.findByTestId('text-merge-conflict-1')
        const firstLines = () => screen.getAllByTestId('code-lines').map(cell => cell.dataset['firstLine'])

        // Their version holds no line in place of the conflict, and says so.
        expect(within(conflict).getByText('merge:editor.no_lines')).toBeInTheDocument()
        expect(resultOf(1).dataset['firstLine']).toBe('2')
        expect(firstLines()).toEqual(['1', '1', '1', '2', '4', '3', '2'])

        await userEvent.click(within(conflict).getByTestId('text-merge-take-ours'))

        // The lines after the conflict follow the lines the result takes.
        expect(firstLines()).toEqual(['1', '1', '1', '2', '4', '4', '2'])
    })

    it('shows lines one branch changed in its column, under the version they were taken from', async () => {
        vi.mocked(getConflictText).mockResolvedValue({
            blocks: [
                { kind: 'merged', text: 'a=ours\n', ours: 'a=ours\n', base: 'a=1\n', theirs: 'a=1\n' },
                { kind: 'merged', text: 'b=2\n' },
                { kind: 'merged', text: 'c=theirs\n', ours: 'c=3\n', base: 'c=3\n', theirs: 'c=theirs\n' },
                { kind: 'merged', text: 'd=4\n' },
                { kind: 'merged', text: 'e=both\n', ours: 'e=both\n', base: 'e=5\n', theirs: 'e=both\n' },
            ],
        })
        const { onApply } = renderEditor()

        expect(await screen.findByText('merge:editor.taken_from_yours')).toBeInTheDocument()
        expect(screen.getByText('merge:editor.taken_from_theirs')).toBeInTheDocument()
        expect(screen.getByText('merge:editor.changed_alike')).toBeInTheDocument()
        expect(shownLines()).toEqual([
            'a=ours', 'a=ours', 'a=1',
            'b=2', 'b=2', 'b=2',
            'c=3', 'c=theirs', 'c=theirs',
            'd=4', 'd=4', 'd=4',
            'e=both', 'e=both', 'e=both',
        ])
        // A file with no conflict is merged as it stands.
        expect(screen.getByTestId('text-merge-apply')).toBeEnabled()
        await userEvent.click(screen.getByTestId('text-merge-apply'))
        expect(onApply).toHaveBeenCalledWith(expect.objectContaining({
            text: 'a=ours\nb=2\nc=theirs\nd=4\ne=both\n',
            decisions: [],
        }))
    })

    it('starts from the parts and the decisions of the file merged here before, without reading it again', async () => {
        const decisions = [{ choice: 'ours' as const, lines: 'b=ours\n' }]
        const { onApply } = renderEditor({ merged: { text: 'a=1\nb=ours\nc=3\n', decisions, blocks: TEXT.blocks } })

        await screen.findByTestId('text-merge-conflict-1')
        expect(getConflictText).not.toHaveBeenCalled()
        expect(resultOf(1)).toHaveValue('b=ours')
        expect(screen.getByTestId('text-merge-apply')).toBeEnabled()
        await userEvent.click(screen.getByTestId('text-merge-apply'))
        expect(onApply).toHaveBeenCalledWith(expect.objectContaining({ text: 'a=1\nb=ours\nc=3\n' }))
    })

    it('shows the ends of a long run of unchanged lines and the rest on request', async () => {
        const lines = Array.from({ length: 20 }, (_, index) => `key${index}=value${index}\n`).join('')
        vi.mocked(getConflictText).mockResolvedValue({
            blocks: [{ kind: 'merged', text: lines }, TEXT.blocks[1]!],
        })
        renderEditor()

        const more = await screen.findByText('merge:editor.unchanged_lines 14')
        expect(shownLines().join('\n')).not.toContain('key10=value10')
        // The last lines keep the numbers they have in the file.
        expect(screen.getAllByTestId('code-lines')[3]!.dataset['firstLine']).toBe('18')

        await userEvent.click(more)

        expect(shownLines().join('\n')).toContain('key10=value10')
    })

    it('says why the file could not be merged', async () => {
        vi.mocked(getConflictText).mockRejectedValue(new Error('The file is not UTF-8 text'))

        renderEditor()

        expect(await screen.findByTestId('text-merge-error')).toHaveTextContent('The file is not UTF-8 text')
        expect(screen.getByTestId('text-merge-apply')).toBeDisabled()
    })

    it('leaves without merging', async () => {
        const { onApply, onCancel } = renderEditor()
        await screen.findByTestId('text-merge-conflict-1')

        await userEvent.click(screen.getByText('merge:buttons.cancel'))

        await waitFor(() => expect(onCancel).toHaveBeenCalled())
        expect(onApply).not.toHaveBeenCalled()
    })
})
