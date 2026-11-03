import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { RevisionPicker } from './RevisionPicker'
import { getProject, getProjectBranches } from 'services/repositories'
import { useProjectRevisions } from 'containers/projects/revisions'

vi.mock('services/repositories', () => ({ getProject: vi.fn(), getProjectBranches: vi.fn() }))
vi.mock('containers/projects/revisions', () => ({ useProjectRevisions: vi.fn() }))

// The branch is picked by the field every branch is picked by; the picker is asked here only for what
// it hands that field - the branches and the marks each of them carries.
vi.mock('containers/projects/BranchSelect', () => ({
    BranchSelect: ({ value, onChange, branchNames, marksOf, ...rest }: any) => (
        <select
            {...rest}
            data-marks={JSON.stringify(branchNames.map((name: string) => marksOf?.(name)))}
            onChange={event => onChange?.(event.target.value)}
            value={value ?? ''}
        >
            {branchNames.map((name: string) => <option key={name} value={name}>{name}</option>)}
        </select>
    ),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// Ant Design's Select needs layout jsdom does not run; the test is about what the picker asks for and
// reports, so it stands in as the plain control it is.
vi.mock('antd', () => ({
    Alert: ({ title, showIcon: _showIcon, type: _type, ...rest }: any) => (
        <div role="alert" {...rest}>{title}</div>
    ),
    Select: ({ onChange, options, value, loading: _loading, popupMatchSelectWidth: _width,
        notFoundContent: _notFound, placeholder, ...rest }: any) => (
        <select
            {...rest}
            data-placeholder={placeholder}
            onChange={event => onChange?.(event.target.value)}
            value={value ?? ''}
        >
            {(options ?? []).map((option: any) => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    ),
    Spin: () => <span data-testid="spin" />,
}))

const REVISIONS = [
    { value: 'rev-2', label: 'rev-2 · yesterday' },
    { value: 'rev-1', label: 'rev-1 · last week' },
]

describe('RevisionPicker', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProject).mockResolvedValue({ id: 'p1', name: 'Pricing', branch: 'master' } as never)
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'master', base: true },
            { name: 'dev', protected: true },
        ])
        vi.mocked(useProjectRevisions).mockReturnValue({
            revisions: [],
            options: REVISIONS,
            error: null,
            hasMore: false,
            loadMore: vi.fn(),
            loadingMore: false,
        })
    })

    const renderPicker = async (onChange = vi.fn()) => {
        render(<RevisionPicker onChange={onChange} projectId="p1" />)
        await waitFor(() => expect(screen.getByTestId('compare-branch')).toHaveValue('master'))
        return onChange
    }

    it('says the revisions are loading while the history is read, and why it could not be read', async () => {
        vi.mocked(useProjectRevisions).mockReturnValue({
            revisions: null,
            options: [],
            error: null,
            hasMore: false,
            loadMore: vi.fn(),
            loadingMore: false,
        })
        await renderPicker()

        expect(screen.getByTestId('compare-revision')).toHaveAttribute('data-placeholder', 'revisions_loading')

        vi.mocked(useProjectRevisions).mockReturnValue({
            revisions: [],
            options: [],
            error: 'History unavailable',
            hasMore: false,
            loadMore: vi.fn(),
            loadingMore: false,
        })
        fireEvent.change(screen.getByTestId('compare-branch'), { target: { value: 'dev' } })

        expect(await screen.findByTestId('compare-picker-error')).toHaveTextContent('History unavailable')
        expect(screen.getByTestId('compare-revision')).not.toHaveAttribute('data-placeholder')
    })

    it('offers the newest revision of the branch the project is on', async () => {
        const onChange = await renderPicker()

        expect(screen.getByTestId('compare-revision')).toHaveValue('rev-2')
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ branch: 'master', revision: 'rev-2' }))
    })

    it('asks for no file: the files changed are listed once the comparison starts', async () => {
        await renderPicker()

        expect(screen.queryByTestId('compare-working-file')).toBeNull()
        expect(screen.queryByTestId('compare-revision-file')).toBeNull()
    })

    it('reads the revisions of the branch that is picked', async () => {
        const onChange = await renderPicker()

        fireEvent.change(screen.getByTestId('compare-branch'), { target: { value: 'dev' } })

        expect(useProjectRevisions).toHaveBeenLastCalledWith({ id: 'p1' }, true, undefined, 'dev')
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ branch: 'dev', revision: 'rev-2' }))
    })

    it('reports the revision that is picked', async () => {
        const onChange = await renderPicker()

        fireEvent.change(screen.getByTestId('compare-revision'), { target: { value: 'rev-1' } })

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ branch: 'master', revision: 'rev-1' }))
    })

    it('hands the branch field the marks each branch carries', async () => {
        await renderPicker()

        // Default and protected read on the branch the same way they do everywhere else.
        expect(screen.getByTestId('compare-branch')).toHaveAttribute('data-marks',
            JSON.stringify([{ isDefault: true }, { isProtected: true }]))
    })

    it('says which project is compared', async () => {
        await renderPicker()

        expect(await screen.findByTestId('compare-project-title')).toBeInTheDocument()
    })

    it('reports nothing while the branch has no revision', async () => {
        vi.mocked(useProjectRevisions).mockReturnValue({
            revisions: [],
            options: [],
            error: null,
            hasMore: false,
            loadMore: vi.fn(),
            loadingMore: false,
        })

        const onChange = await renderPicker()

        expect(onChange).toHaveBeenLastCalledWith(null)
    })

    it('asks for no branch in a repository without branches', async () => {
        vi.mocked(getProjectBranches).mockResolvedValue([])
        vi.mocked(getProject).mockResolvedValue({ id: 'p1', name: 'Pricing', branch: null } as never)
        const onChange = vi.fn()

        render(<RevisionPicker onChange={onChange} projectId="p1" />)

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ branch: undefined, revision: 'rev-2' }))
        expect(screen.queryByTestId('compare-branch')).toBeNull()
    })

    it('says why the project could not be read', async () => {
        vi.mocked(getProject).mockRejectedValue(new Error('The project is not found'))

        render(<RevisionPicker onChange={vi.fn()} projectId="p1" />)

        expect(await screen.findByTestId('compare-picker-error')).toHaveTextContent('The project is not found')
    })
})
