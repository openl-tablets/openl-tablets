import React from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { RevisionPicker } from './RevisionPicker'
import { getProjectCompareFiles } from 'services/compare'
import { getProject, getProjectBranches } from 'services/repositories'
import { useProjectRevisions } from 'containers/projects/revisions'

vi.mock('services/compare', () => ({ getProjectCompareFiles: vi.fn() }))
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
        placeholder: _placeholder, ...rest }: any) => (
        <select {...rest} onChange={event => onChange?.(event.target.value)} value={value ?? ''}>
            {(options ?? []).map((option: any) => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    ),
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
        vi.mocked(getProjectCompareFiles).mockResolvedValue(['rules/Main.xlsx', 'rules/Rates.xlsx'])
    })

    const renderPicker = async (onChange = vi.fn()) => {
        await act(async () => {
            render(<RevisionPicker onChange={onChange} projectId="p1" />)
        })
        return onChange
    }

    it('offers the newest revision of the branch the project is on, with its files', async () => {
        const onChange = await renderPicker()

        await waitFor(() => expect(getProjectCompareFiles)
            .toHaveBeenCalledWith('p1', { branch: 'master', revision: 'rev-2' }))
        // The working copy is read as the project has it now, without a branch or a revision.
        expect(getProjectCompareFiles).toHaveBeenCalledWith('p1', {})
        expect(screen.getByTestId('compare-branch')).toHaveValue('master')
        expect(screen.getByTestId('compare-revision')).toHaveValue('rev-2')

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({
            first: { path: 'rules/Main.xlsx' },
            second: { path: 'rules/Main.xlsx', branch: 'master', revision: 'rev-2' },
        }))
    })

    it('reads the revisions and the files of the branch that is picked', async () => {
        await renderPicker()
        await waitFor(() => expect(screen.getByTestId('compare-revision')).toHaveValue('rev-2'))

        await act(async () => {
            fireEvent.change(screen.getByTestId('compare-branch'), { target: { value: 'dev' } })
        })

        expect(useProjectRevisions).toHaveBeenLastCalledWith({ id: 'p1' }, true, undefined, 'dev')
        await waitFor(() => expect(getProjectCompareFiles)
            .toHaveBeenCalledWith('p1', { branch: 'dev', revision: 'rev-2' }))
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
        // The side of the working copy says which revision it is, as the old screen did.
        expect(screen.getByTestId('compare-working-revision')).toHaveTextContent('user_workspace')
    })

    it('says nothing to compare while a side has no file', async () => {
        vi.mocked(getProjectCompareFiles).mockResolvedValue([])

        const onChange = await renderPicker()

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(null))
    })

    it('says why the files could not be read', async () => {
        vi.mocked(getProjectCompareFiles).mockRejectedValue(new Error('The project is not found'))

        await renderPicker()

        expect(await screen.findByTestId('compare-picker-error')).toHaveTextContent('The project is not found')
    })

    it('picks the same file on both sides whichever of them answers first', async () => {
        // The revision answers before the working copy, so the file it picks cannot be read off it yet.
        let answerWorkingCopy!: (files: string[]) => void
        vi.mocked(getProjectCompareFiles).mockImplementation((_id: string, where: any) => (
            where?.revision
                ? Promise.resolve(['rules/Other.xlsx', 'rules/Main.xlsx'])
                : new Promise<string[]>(resolve => {
                    answerWorkingCopy = () => resolve(['rules/Main.xlsx', 'rules/Other.xlsx'])
                })))

        const onChange = await renderPicker()
        await act(async () => {
            answerWorkingCopy(['rules/Main.xlsx', 'rules/Other.xlsx'])
        })

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({
            first: { path: 'rules/Main.xlsx' },
            second: { path: 'rules/Main.xlsx', branch: 'master', revision: 'rev-2' },
        }))
    })

    it('keeps the failure of a read that was abandoned off the screen', async () => {
        // The branch is switched while the revision of the old branch is still being read, and that
        // read answers last, with a revision the new branch does not have.
        let refuseTheOldRevision!: () => void
        vi.mocked(getProjectCompareFiles).mockImplementation((_id: string, where: any) => {
            if (where?.branch === 'master' && where?.revision) {
                return new Promise<string[]>((_resolve, reject) => {
                    refuseTheOldRevision = () => reject(new Error('The requested version is not found'))
                })
            }
            return Promise.resolve(['rules/Main.xlsx'])
        })

        await renderPicker()
        await act(async () => {
            fireEvent.change(screen.getByTestId('compare-branch'), { target: { value: 'dev' } })
        })
        await act(async () => {
            refuseTheOldRevision()
        })

        expect(screen.queryByTestId('compare-picker-error')).toBeNull()
    })

    it('leaves the failure behind once the files are read', async () => {
        vi.mocked(getProjectCompareFiles).mockRejectedValue(new Error('The revision is not found'))
        await renderPicker()
        await screen.findByTestId('compare-picker-error')

        vi.mocked(getProjectCompareFiles).mockResolvedValue(['rules/Main.xlsx'])
        await act(async () => {
            fireEvent.change(screen.getByTestId('compare-revision'), { target: { value: 'rev-1' } })
        })

        await waitFor(() => expect(screen.queryByTestId('compare-picker-error')).toBeNull())
    })
})
