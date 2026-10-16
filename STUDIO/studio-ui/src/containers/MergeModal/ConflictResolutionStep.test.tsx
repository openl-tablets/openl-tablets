import React from 'react'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConflictResolutionStep } from 'containers/MergeModal/ConflictResolutionStep'
import * as services from 'services'
import { saveFile } from 'utils/download'
import { BranchInfo, ConflictDetails, ConflictFileAvailability, ConflictGroup } from 'containers/MergeModal/types'
import type { MockedFunction } from 'vitest'

vi.mock('services', () => ({
    apiCall: vi.fn(),
    CONFIG: { CONTEXT: '' },
}))

// The editor merges a text file of its own; the step is asked here only what it opens it with and what it does
// with the merged text.
const mergeEditor = vi.fn()
const MERGED = {
    text: 'b=merged\n',
    decisions: [{ choice: 'theirs', lines: 'b=merged\n' }],
    blocks: [{ kind: 'conflict', ours: 'b=ours\n', base: 'b=1\n', theirs: 'b=merged\n' }],
}
vi.mock('containers/MergeModal/TextMergeEditor', () => ({
    TextMergeEditor: (props: any) => {
        mergeEditor(props)
        return (
            <div data-testid="text-merge-editor">
                <button
                    onClick={() => props.onApply(MERGED)}
                >
                    apply
                </button>
                <button onClick={props.onCancel}>leave</button>
            </div>
        )
    },
}))

// The editor of a workbook merges it on the server; the step is asked here only what it opens it with and what it
// does with the merged workbook.
const workbookEditor = vi.fn()
const MERGED_WORKBOOK = {
    file: new Blob(['merged workbook'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    decisions: { 'Rules/A1:B2': 'theirs' },
    workbook: { sheets: [], palette: false },
}
vi.mock('containers/MergeModal/WorkbookMergeEditor', () => ({
    WorkbookMergeEditor: (props: any) => {
        workbookEditor(props)
        return (
            <div data-testid="workbook-merge-editor">
                <button onClick={() => props.onApply(MERGED_WORKBOOK)}>apply</button>
                <button onClick={props.onCancel}>leave</button>
            </div>
        )
    },
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string, params?: Record<string, string>) => {
        if (params) {
            return Object.entries(params).reduce(
                (acc, [k, v]) => acc.replace(`{{${k}}}`, v),
                key
            )
        }
        return key
    }
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
    }
})

// Ant Design Table causes infinite update loops in jsdom during act() flushes.
// Mock heavy components with simple HTML equivalents to avoid this.
vi.mock('antd', async () => {
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        Space: ({ children }: any) => <div>{children}</div>,
        Button: ({ children, onClick, disabled, loading, icon }: any) => (
            <button disabled={disabled || loading} onClick={onClick}>{icon}{children}</button>
        ),
        Alert: ({ title, type }: any) => <div data-type={type} role="alert">{title}</div>,
        Radio: Object.assign(
            ({ children, value, ...rest }: any) => (
                <label>
                    <input type="radio" value={value} {...rest} />
                    {children}
                </label>
            ),
            {
                Group: ({ children, onChange, value }: any) => (
                    <div
                        data-value={value}
                        onChange={(e: any) => onChange?.({ target: { value: e.target.value } })}
                        role="radiogroup"
                    >
                        {children}
                    </div>
                ),
            }
        ),
        Typography: {
            Text: ({ children, strong }: any) => (
                <span style={strong ? { fontWeight: 'bold' } : undefined}>{children}</span>
            ),
        },
        Input: Object.assign(
            (props: any) => <input {...props} />,
            {
                TextArea: ({ value, onChange, placeholder }: any) => (
                    <textarea
                        onChange={onChange}
                        placeholder={placeholder}
                        value={value}
                    />
                ),
            }
        ),
        Tag: ({ children, ...rest }: any) => <span {...rest}>{children}</span>,
        Tooltip: ({ children }: any) => <>{children}</>,
        Upload: ({ children }: any) => (
            <div data-testid="upload">{children}</div>
        ),
        Table: ({ dataSource, columns }: any) => (
            <table>
                <tbody>
                    {dataSource?.map((row: any) => (
                        <tr key={row.key}>
                            {columns?.map((col: any) => (
                                <td key={col.key}>
                                    {col.render
                                        ? col.render(row[col.dataIndex], row)
                                        : row[col.dataIndex]}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        ),
        Descriptions: Object.assign(
            ({ children }: any) => <dl>{children}</dl>,
            {
                Item: ({ label, children }: any) => (
                    <React.Fragment>
                        <dt>{label}</dt>
                        <dd>{children}</dd>
                    </React.Fragment>
                ),
            }
        ),
        Spin: ({ children }: any) => <div data-testid="spin">{children}</div>,
        notification: {
            success: vi.fn(),
            warning: vi.fn(),
            info: vi.fn(),
        },
    })
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

/** The form the dialog sent to resolve the conflicts. */
const resolvedForm = () => mockApiCall.mock.calls
    .find(call => call[0] === '/projects/proj-1/merge/conflicts/resolve')![1]!.body as FormData

const conflictGroups: ConflictGroup[] = [
    { projectName: 'Project A', projectPath: 'projectA', files: ['rules/Main.xlsx']},
]

const multiFileGroups: ConflictGroup[] = [
    { projectName: 'Project A', projectPath: 'projectA', files: ['rules/Main.xlsx', 'rules/Helper.xlsx']},
    { projectName: 'Project B', projectPath: 'projectB', files: ['config/Settings.xml']},
]

const conflictDetails: ConflictDetails = {
    conflictGroups,
    fileAvailability: {
        'rules/Main.xlsx': { ours: true, theirs: true, base: true, text: false },
    },
    oursRevision: {
        commit: 'abc123',
        branch: 'feature',
        author: 'Alice',
        modifiedAt: '2026-04-09T10:00:00Z',
        exists: true,
    },
    theirsRevision: {
        commit: 'def456',
        branch: 'main',
        author: 'Bob',
        modifiedAt: '2026-04-08T15:00:00Z',
        exists: true,
    },
    baseRevision: {
        commit: 'ghi789',
        branch: null,
        author: null,
        modifiedAt: null,
        exists: true,
    },
    defaultMessage: 'Merge feature into main',
}

const branches: BranchInfo[] = [
    { name: 'main', protected: true, base: true },
    { name: 'feature', protected: false },
]

const defaultProps = () => ({
    projectId: 'proj-1',
    conflictGroups,
    branches,
    onResolveSuccess: vi.fn(),
    onCancel: vi.fn(),
    onCompare: vi.fn(),
    onSavingChange: vi.fn(),
})

/**
 * Renders the component and waits for the async loadConflictDetails to complete.
 * The spinner stands in for the whole step until the details are read.
 */
const renderAndLoad = async (props = defaultProps(), details: ConflictDetails = conflictDetails) => {
    mockApiCall.mockResolvedValueOnce(details)
    render(<ConflictResolutionStep {...props} />)
    await waitFor(() => expect(screen.queryByTestId('spin')).not.toBeInTheDocument())
    return props
}

describe('ConflictResolutionStep', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    describe('loading state', () => {
        it('loads conflict details on mount', async () => {
            await renderAndLoad()

            expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/conflicts',
                { method: 'GET' },
                true
            )
        })

        it('shows error when loading fails', async () => {
            mockApiCall.mockRejectedValueOnce(new Error('Load error'))

            render(<ConflictResolutionStep {...defaultProps()} />)

            expect(await screen.findByText('Load error')).toBeInTheDocument()
        })

        it('shows generic error when loading fails without message', async () => {
            mockApiCall.mockRejectedValueOnce({})

            render(<ConflictResolutionStep {...defaultProps()} />)

            expect(await screen.findByText('merge:errors.load_failed')).toBeInTheDocument()
        })
    })

    describe('conflict details display', () => {
        it('shows revision branch names', async () => {
            await renderAndLoad()

            expect(screen.getByText('feature')).toBeInTheDocument()
            expect(screen.getByText('main')).toBeInTheDocument()
        })

        it('lays the versions out as merge tools do: yours, the base, theirs', async () => {
            await renderAndLoad()

            const labels = [...document.querySelectorAll('dt')].map(term => term.textContent)
            expect(labels).toEqual(['merge:revisions.yours', 'merge:revisions.base', 'merge:revisions.theirs'])
            const downloads = screen.getAllByText(/merge:compare\.download_/).map(link => link.textContent)
            expect(downloads.slice(0, 3)).toEqual([
                'merge:compare.download_yours',
                'merge:compare.download_base',
                'merge:compare.download_theirs',
            ])
        })

        it('keeps a long revision branch on one line and exposes its full name', async () => {
            const branch = 'Example3-AutoPolicyCalculation/openl/20260907'
            const details = {
                ...conflictDetails,
                theirsRevision: { ...conflictDetails.theirsRevision, branch },
            }
            await renderAndLoad(defaultProps(), details)

            expect(screen.getByTitle(branch)).toHaveTextContent(branch)
        })

        it('marks a revision branch as the default and protected one', async () => {
            await renderAndLoad()

            expect(screen.getByTestId('conflict-branch-main-default')).toBeInTheDocument()
            expect(screen.getByTestId('conflict-branch-main-protected')).toBeInTheDocument()
            expect(screen.queryByTestId('conflict-branch-feature-protected')).not.toBeInTheDocument()
        })

        it('shows "not exists" for non-existing revision', async () => {
            const details = {
                ...conflictDetails,
                baseRevision: { ...conflictDetails.baseRevision, exists: false },
            }
            await renderAndLoad(defaultProps(), details)

            expect(screen.getByText('merge:revisions.not_exists')).toBeInTheDocument()
        })

        it('populates merge message from API', async () => {
            await renderAndLoad()

            expect(screen.getByDisplayValue('Merge feature into main')).toBeInTheDocument()
        })

        it('shows conflict files in table', async () => {
            await renderAndLoad()

            expect(screen.getByText('Main.xlsx')).toBeInTheDocument()
        })

        it('shows project name as group header', async () => {
            await renderAndLoad()

            expect(screen.getByText('Project A')).toBeInTheDocument()
        })

        it('shows multiple groups and files', async () => {
            const props = defaultProps()
            props.conflictGroups = multiFileGroups
            await renderAndLoad(props, { ...conflictDetails, conflictGroups: multiFileGroups })

            expect(screen.getByText('Project A')).toBeInTheDocument()
            expect(screen.getByText('Project B')).toBeInTheDocument()
            expect(screen.getByText('Main.xlsx')).toBeInTheDocument()
            expect(screen.getByText('Helper.xlsx')).toBeInTheDocument()
            expect(screen.getByText('Settings.xml')).toBeInTheDocument()
        })
    })

    describe('resolution selection', () => {
        it('renders radio buttons for each resolution strategy', async () => {
            await renderAndLoad()

            expect(screen.getByText('merge:resolution.use_yours')).toBeInTheDocument()
            expect(screen.getByText('merge:resolution.use_theirs')).toBeInTheDocument()
            expect(screen.getByText('merge:resolution.use_base')).toBeInTheDocument()
            expect(screen.getByText('merge:resolution.upload_custom')).toBeInTheDocument()
        })

        it.each([
            ['selects OURS resolution and shows resolved', 'OURS', 'merge:resolution.resolved'],
            ['selects THEIRS resolution and shows resolved', 'THEIRS', 'merge:resolution.resolved'],
            ['selects BASE resolution and shows resolved', 'BASE', 'merge:resolution.resolved'],
            ['shows upload button when CUSTOM is selected', 'CUSTOM', 'merge:upload.select_file'],
        ])('%s', async (_behaviour, resolution, shown) => {
            await renderAndLoad()

            await userEvent.click(screen.getByDisplayValue(resolution))

            expect(screen.getByText(shown)).toBeInTheDocument()
        })
    })

    describe('compare and download', () => {
        it('calls onCompare when compare button clicked', async () => {
            const props = await renderAndLoad()

            await userEvent.click(screen.getByText('merge:compare.title'))

            expect(props.onCompare).toHaveBeenCalledWith('rules/Main.xlsx')
        })

        it('opens download URL for OURS version', async () => {
            const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
            try {
                await renderAndLoad()

                await userEvent.click(screen.getByText('merge:compare.download_yours'))

                expect(openSpy).toHaveBeenCalledWith(
                    expect.stringContaining('side=ours'),
                    '_blank'
                )
            } finally {
                openSpy.mockRestore()
            }
        })

        it('opens download URL for THEIRS version', async () => {
            const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
            try {
                await renderAndLoad()

                await userEvent.click(screen.getByText('merge:compare.download_theirs'))

                expect(openSpy).toHaveBeenCalledWith(
                    expect.stringContaining('side=theirs'),
                    '_blank'
                )
            } finally {
                openSpy.mockRestore()
            }
        })

        it('opens download URL for BASE version', async () => {
            const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
            try {
                await renderAndLoad()

                await userEvent.click(screen.getByText('merge:compare.download_base'))

                expect(openSpy).toHaveBeenCalledWith(
                    expect.stringContaining('side=base'),
                    '_blank'
                )
            } finally {
                openSpy.mockRestore()
            }
        })

        it.each([
            ['oursRevision', 'merge:compare.download_yours', 'merge:compare.deleted_yours'],
            ['theirsRevision', 'merge:compare.download_theirs', 'merge:compare.deleted_theirs'],
            ['baseRevision', 'merge:compare.download_base', 'merge:compare.deleted_base'],
        ] as const)('shows a deleted status instead of the download action when %s does not contain the file',
            async (revision, downloadLabel, deletedLabel) => {
                const side = revision.replace('Revision', '') as keyof ConflictFileAvailability
                const availability = conflictDetails.fileAvailability['rules/Main.xlsx']!
                const details = {
                    ...conflictDetails,
                    fileAvailability: {
                        'rules/Main.xlsx': { ...availability, [side]: false },
                    },
                }
                await renderAndLoad(defaultProps(), details)

                expect(screen.queryByText(downloadLabel)).not.toBeInTheDocument()
                expect(screen.getByText(deletedLabel)).toBeInTheDocument()
                expect(screen.queryByRole('button', { name: deletedLabel })).not.toBeInTheDocument()
                expect(screen.getByText('merge:compare.title')).toBeInTheDocument()
            })

        it('uses availability of each file when one conflict contains different deleted sides', async () => {
            const groups = [{
                projectName: 'Project A',
                projectPath: 'projectA',
                files: ['rules/Main.xlsx', 'rules/Helper.xlsx'],
            }]
            const details = {
                ...conflictDetails,
                conflictGroups: groups,
                fileAvailability: {
                    'rules/Main.xlsx': { ours: false, theirs: true, base: true, text: false },
                    'rules/Helper.xlsx': { ours: true, theirs: false, base: true, text: false },
                },
            }
            await renderAndLoad({ ...defaultProps(), conflictGroups: groups }, details)

            const mainRow = screen.getByText('Main.xlsx').closest('tr')!
            expect(within(mainRow).getByText('merge:compare.deleted_yours')).toBeInTheDocument()
            expect(within(mainRow).getByText('merge:compare.download_theirs')).toBeInTheDocument()

            const helperRow = screen.getByText('Helper.xlsx').closest('tr')!
            expect(within(helperRow).getByText('merge:compare.download_yours')).toBeInTheDocument()
            expect(within(helperRow).getByText('merge:compare.deleted_theirs')).toBeInTheDocument()
        })
    })

    describe('resolve action', () => {
        it('resolve button is disabled when no resolutions selected', async () => {
            await renderAndLoad()

            expect(screen.getByText('merge:buttons.resolve').closest('button')).toBeDisabled()
        })

        it('resolve button is enabled when all files resolved', async () => {
            await renderAndLoad()

            await userEvent.click(screen.getByDisplayValue('OURS'))

            expect(screen.getByText('merge:buttons.resolve').closest('button')).not.toBeDisabled()
        })

        it('submits resolutions and calls onResolveSuccess', async () => {
            const props = await renderAndLoad()

            mockApiCall.mockResolvedValueOnce({}) // resolve

            await userEvent.click(screen.getByDisplayValue('OURS'))
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => {
                expect(mockApiCall).toHaveBeenCalledWith(
                    '/projects/proj-1/merge/conflicts/resolve',
                    expect.objectContaining({ method: 'POST' }),
                    true
                )
            })
            // onResolveSuccess fires AFTER `await apiCall(...)` resolves inside
            // handleSave — the mockApiCall assertion above passes at call time,
            // which is one microtask earlier than the success callback. Wait for
            // the callback itself to avoid a timing race.
            await waitFor(() => {
                expect(props.onResolveSuccess).toHaveBeenCalled()
            })
            // The dialog stays open while the resolution is saved.
            await waitFor(() => {
                expect(props.onSavingChange.mock.calls).toEqual([[true], [false]])
            })
        })

        it('hands what the merge changed on to the dialog', async () => {
            const props = await renderAndLoad()
            const summary = {
                sourceBranch: 'feature',
                targetBranch: 'main',
                before: 'b1',
                after: 'a1',
                projects: [],
                hiddenProjects: 0,
            }
            mockApiCall.mockResolvedValueOnce({ status: 'success', resolvedFiles: [], summary })

            await userEvent.click(screen.getByDisplayValue('OURS'))
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => expect(props.onResolveSuccess).toHaveBeenCalledWith(summary))
        })

        it('sends FormData with resolution entries', async () => {
            await renderAndLoad()

            mockApiCall.mockResolvedValueOnce({}) // resolve

            await userEvent.click(screen.getByDisplayValue('THEIRS'))
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => {
                expect(mockApiCall).toHaveBeenCalledWith(
                    '/projects/proj-1/merge/conflicts/resolve',
                    expect.anything(),
                    true
                )
            })
            const formData = resolvedForm()
            expect(formData.get('resolutions[0].filePath')).toBe('rules/Main.xlsx')
            expect(formData.get('resolutions[0].strategy')).toBe('THEIRS')
        })

        it('includes modified merge message in FormData', async () => {
            await renderAndLoad()

            mockApiCall.mockResolvedValueOnce({}) // resolve

            const textarea = screen.getByDisplayValue('Merge feature into main')
            await userEvent.clear(textarea)
            await userEvent.type(textarea, 'Custom merge message')

            await userEvent.click(screen.getByDisplayValue('OURS'))
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => {
                expect(mockApiCall).toHaveBeenCalledWith(
                    '/projects/proj-1/merge/conflicts/resolve',
                    expect.anything(),
                    true
                )
            })
            const formData = resolvedForm()
            expect(formData.get('message')).toBe('Custom merge message')
        })

        it('does not include message in FormData when unchanged', async () => {
            await renderAndLoad()

            mockApiCall.mockResolvedValueOnce({}) // resolve

            await userEvent.click(screen.getByDisplayValue('OURS'))
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => {
                expect(mockApiCall).toHaveBeenCalledWith(
                    '/projects/proj-1/merge/conflicts/resolve',
                    expect.anything(),
                    true
                )
            })
            const formData = resolvedForm()
            expect(formData.get('message')).toBeNull()
        })

        it('shows error when resolve fails', async () => {
            await renderAndLoad()

            mockApiCall.mockRejectedValueOnce(new Error('Resolve failed'))

            await userEvent.click(screen.getByDisplayValue('OURS'))
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => {
                expect(screen.getByText('Resolve failed')).toBeInTheDocument()
            })
        })

        it('shows generic error when resolve fails without message', async () => {
            await renderAndLoad()

            mockApiCall.mockRejectedValueOnce({})

            await userEvent.click(screen.getByDisplayValue('OURS'))
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => {
                expect(screen.getByText('merge:errors.resolve_failed')).toBeInTheDocument()
            })
        })
    })

    describe('cancel action', () => {
        it('leaves the conflicts to the dialog, which discards the merge', async () => {
            const props = await renderAndLoad()

            await userEvent.click(screen.getByText('merge:buttons.cancel'))

            expect(props.onCancel).toHaveBeenCalled()
            expect(mockApiCall).not.toHaveBeenCalledWith('/projects/proj-1/merge/conflicts', { method: 'DELETE' }, true)
        })
    })

    describe('merge message', () => {
        it('allows editing merge message', async () => {
            await renderAndLoad()

            const textarea = screen.getByDisplayValue('Merge feature into main')
            await userEvent.clear(textarea)
            await userEvent.type(textarea, 'New message')

            expect(screen.getByDisplayValue('New message')).toBeInTheDocument()
        })
    })

    describe('merging a text file line by line', () => {
        const MESSAGES = 'projectA/i18n/message.properties'
        const textDetails = (availability = { ours: true, theirs: true, base: true, text: true }): ConflictDetails => ({
            ...conflictDetails,
            conflictGroups: [{ projectName: 'Project A', projectPath: 'projectA', files: [MESSAGES]}],
            fileAvailability: { [MESSAGES]: availability },
        })
        const textProps = () => ({
            ...defaultProps(),
            conflictGroups: [{ projectName: 'Project A', projectPath: 'projectA', files: [MESSAGES]}],
        })

        /** Picks merging the file line by line and applies what the editor merged. */
        const mergeInPlace = async () => {
            await userEvent.click(screen.getByDisplayValue('MERGE'))
            await userEvent.click(within(screen.getByTestId('text-merge-editor')).getByText('apply'))
        }

        it('offers it for a file the server can merge line by line', async () => {
            await renderAndLoad(textProps(), textDetails())

            expect(screen.getByText('merge:resolution.merge_in_place')).toBeInTheDocument()
        })

        it('does not offer it for a workbook, which is merged table by table instead', async () => {
            await renderAndLoad()

            expect(screen.queryByText('merge:resolution.merge_in_place')).toBeNull()
            expect(screen.getByText('merge:resolution.merge_tables')).toBeInTheDocument()
        })

        it('does not offer it for a file that is not text, such as a library', async () => {
            await renderAndLoad(textProps(), textDetails({ ours: true, theirs: true, base: true, text: false }))

            expect(screen.queryByText('merge:resolution.merge_in_place')).toBeNull()
        })

        it('does not offer it for a file a version no longer holds', async () => {
            await renderAndLoad(textProps(), textDetails({ ours: true, theirs: false, base: true, text: true }))

            expect(screen.queryByText('merge:resolution.merge_in_place')).toBeNull()
        })

        it('merges the file in the editor and sends the merged text as the merged file', async () => {
            const props = await renderAndLoad(textProps(), textDetails())

            await userEvent.click(screen.getByDisplayValue('MERGE'))

            expect(mergeEditor).toHaveBeenLastCalledWith(expect.objectContaining({
                filePath: MESSAGES,
                projectId: 'proj-1',
                merged: undefined,
            }))
            // The columns of the editor name the branches the versions come from.
            const { versionLabels } = mergeEditor.mock.lastCall![0]
            expect(versionLabels.ours.props.name).toBe(textDetails().oursRevision!.branch)
            expect(versionLabels.theirs.props.name).toBe(textDetails().theirsRevision!.branch)
            await userEvent.click(within(screen.getByTestId('text-merge-editor')).getByText('apply'))

            expect(screen.queryByTestId('text-merge-editor')).toBeNull()
            expect(screen.getByText('merge:resolution.merged_in_place')).toBeInTheDocument()
            mockApiCall.mockResolvedValueOnce({})
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => expect(props.onResolveSuccess).toHaveBeenCalled())
            const formData = resolvedForm()
            expect(formData.get('resolutions[0].strategy')).toBe('CUSTOM')
            const file = formData.get('resolutions[0].file') as File
            expect(file.name).toBe('message.properties')
            expect(await file.text()).toBe('b=merged\n')
        })

        it('opens the editor again on the decisions made', async () => {
            await renderAndLoad(textProps(), textDetails())
            await mergeInPlace()

            await userEvent.click(screen.getByText('merge:resolution.edit_merge'))

            // The editor gets the decisions with the parts of the file they were made for.
            expect(mergeEditor).toHaveBeenLastCalledWith(expect.objectContaining({ merged: MERGED }))
        })

        it('leaves the resolution as it was when the editor is left', async () => {
            await renderAndLoad(textProps(), textDetails())

            await userEvent.click(screen.getByDisplayValue('MERGE'))
            await userEvent.click(within(screen.getByTestId('text-merge-editor')).getByText('leave'))

            expect(screen.queryByTestId('text-merge-editor')).toBeNull()
            expect(screen.getByText('merge:buttons.resolve')).toBeDisabled()
        })

        it('lets another resolution take the place of the merged text', async () => {
            await renderAndLoad(textProps(), textDetails())
            await mergeInPlace()

            await userEvent.click(screen.getByDisplayValue('OURS'))

            expect(screen.queryByText('merge:resolution.merged_in_place')).toBeNull()
            mockApiCall.mockResolvedValueOnce({})
            await userEvent.click(screen.getByText('merge:buttons.resolve'))
            await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/conflicts/resolve', expect.anything(), true))
            const formData = resolvedForm()
            expect(formData.get('resolutions[0].strategy')).toBe('OURS')
            expect(formData.get('resolutions[0].file')).toBeNull()
        })

        it('asks for a file again when the upload takes the place of the merged text', async () => {
            await renderAndLoad(textProps(), textDetails())
            await mergeInPlace()

            await userEvent.click(screen.getByDisplayValue('CUSTOM'))

            expect(screen.getByText('merge:upload.select_file')).toBeInTheDocument()
            expect(screen.getByText('merge:buttons.resolve')).toBeDisabled()
        })
    })

    describe('merging a workbook table by table', () => {
        const withAvailability = (availability: ConflictFileAvailability): ConflictDetails => ({
            ...conflictDetails,
            fileAvailability: { 'rules/Main.xlsx': availability },
        })

        /** Picks merging the workbook table by table and applies what the editor merged. */
        const mergeTables = async () => {
            await userEvent.click(screen.getByDisplayValue('MERGE'))
            await userEvent.click(within(screen.getByTestId('workbook-merge-editor')).getByText('apply'))
        }

        it('is offered for a workbook every version holds, and not for one a version no longer holds', async () => {
            await renderAndLoad(defaultProps(), withAvailability({ ours: true, theirs: true, base: false, text: false }))

            expect(screen.queryByText('merge:resolution.merge_tables')).toBeNull()
        })

        it('merges the workbook in the editor and sends it as the merged file', async () => {
            const props = await renderAndLoad()

            await userEvent.click(screen.getByDisplayValue('MERGE'))

            expect(workbookEditor).toHaveBeenLastCalledWith(expect.objectContaining({
                filePath: 'rules/Main.xlsx',
                projectId: 'proj-1',
                merged: undefined,
            }))
            const { versionLabels } = workbookEditor.mock.lastCall![0]
            expect(versionLabels.ours.props.name).toBe(conflictDetails.oursRevision.branch)
            expect(screen.queryByTestId('text-merge-editor')).toBeNull()
            await userEvent.click(within(screen.getByTestId('workbook-merge-editor')).getByText('apply'))

            expect(screen.getByText('merge:resolution.merged_tables')).toBeInTheDocument()
            mockApiCall.mockResolvedValueOnce({})
            await userEvent.click(screen.getByText('merge:buttons.resolve'))

            await waitFor(() => expect(props.onResolveSuccess).toHaveBeenCalled())
            const formData = resolvedForm()
            expect(formData.get('resolutions[0].strategy')).toBe('CUSTOM')
            const file = formData.get('resolutions[0].file') as File
            expect(file.name).toBe('Main.xlsx')
            expect(file.type).toBe(MERGED_WORKBOOK.file.type)
            expect(await file.text()).toBe('merged workbook')
        })

        it('downloads the merged workbook before it is saved', async () => {
            await renderAndLoad()
            await mergeTables()

            await userEvent.click(screen.getByText('merge:resolution.download_merged'))

            expect(saveFile).toHaveBeenCalledWith(MERGED_WORKBOOK.file, 'Main.xlsx')
        })

        it('opens the editor again on the decisions made', async () => {
            await renderAndLoad()
            await mergeTables()

            await userEvent.click(screen.getByText('merge:resolution.edit_merge'))

            expect(workbookEditor).toHaveBeenLastCalledWith(expect.objectContaining({ merged: MERGED_WORKBOOK }))
        })

        it('lets another resolution take the place of the merged workbook', async () => {
            await renderAndLoad()
            await mergeTables()

            await userEvent.click(screen.getByDisplayValue('THEIRS'))

            expect(screen.queryByText('merge:resolution.merged_tables')).toBeNull()
            expect(screen.queryByText('merge:resolution.download_merged')).toBeNull()
        })
    })
})
