import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConflictResolutionStep } from 'containers/MergeModal/ConflictResolutionStep'
import * as services from 'services'
import { ConflictDetails, ConflictGroup } from 'containers/MergeModal/types'

jest.mock('services', () => ({
    apiCall: jest.fn(),
    CONFIG: { CONTEXT: '' },
}))

jest.mock('react-i18next', () => {
    const t = (key: string, params?: Record<string, string>) => {
        if (params) {
            return Object.entries(params).reduce(
                (acc, [k, v]) => acc.replace(`{{${k}}}`, v),
                key
            )
        }
        return key
    }
    return {
        useTranslation: () => ({ t, i18n: { language: 'en' } }),
    }
})

// Ant Design Table causes infinite update loops in jsdom during act() flushes.
// Mock heavy components with simple HTML equivalents to avoid this.
jest.mock('antd', () => {
    const React = require('react')
    return {
        Space: ({ children }: any) => <div>{children}</div>,
        Button: ({ children, onClick, disabled, loading, icon, size, type: _, ...rest }: any) => (
            <button onClick={onClick} disabled={disabled || loading}>{icon}{children}</button>
        ),
        Alert: ({ title, type }: any) => <div role="alert" data-type={type}>{title}</div>,
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
                        role="radiogroup"
                        data-value={value}
                        onChange={(e: any) => onChange?.({ target: { value: e.target.value } })}
                    >
                        {children}
                    </div>
                ),
            }
        ),
        Typography: {
            Text: ({ children, strong, type: _, ellipsis, ...rest }: any) => (
                <span style={strong ? { fontWeight: 'bold' } : undefined}>{children}</span>
            ),
        },
        Input: Object.assign(
            (props: any) => <input {...props} />,
            {
                TextArea: ({ value, onChange, placeholder, autoSize, ...rest }: any) => (
                    <textarea
                        value={value}
                        onChange={onChange}
                        placeholder={placeholder}
                    />
                ),
            }
        ),
        Tooltip: ({ children, title }: any) => <>{children}</>,
        Upload: ({ children, beforeUpload, maxCount, showUploadList }: any) => (
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
        Spin: ({ children, size }: any) => <div data-testid="spin">{children}</div>,
        notification: {
            success: jest.fn(),
            warning: jest.fn(),
            info: jest.fn(),
        },
    }
})

const mockApiCall = services.apiCall as jest.MockedFunction<typeof services.apiCall>
const { notification } = require('antd')

const conflictGroups: ConflictGroup[] = [
    { projectName: 'Project A', projectPath: 'projectA', files: ['rules/Main.xlsx'] },
]

const multiFileGroups: ConflictGroup[] = [
    { projectName: 'Project A', projectPath: 'projectA', files: ['rules/Main.xlsx', 'rules/Helper.xlsx'] },
    { projectName: 'Project B', projectPath: 'projectB', files: ['config/Settings.xml'] },
]

const conflictDetails: ConflictDetails = {
    conflictGroups,
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

const defaultProps = () => ({
    projectId: 'proj-1',
    conflictGroups,
    onResolveSuccess: jest.fn(),
    onCancel: jest.fn(),
    onCompare: jest.fn(),
})

/**
 * Renders the component and waits for the async loadConflictDetails to complete.
 * Must use act + setTimeout because the state update from the useEffect's async callback
 * requires explicit flushing in jsdom.
 */
const renderAndLoad = async (props = defaultProps(), details: ConflictDetails = conflictDetails) => {
    mockApiCall.mockResolvedValueOnce(details)
    await act(async () => {
        render(<ConflictResolutionStep {...props} />)
        await new Promise(r => setTimeout(r, 50))
    })
    return props
}

describe('ConflictResolutionStep', () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {})

    beforeEach(() => {
        jest.clearAllMocks()
        consoleErrorSpy.mockClear()
    })

    afterAll(() => {
        consoleErrorSpy.mockRestore()
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

            await act(async () => {
                render(<ConflictResolutionStep {...defaultProps()} />)
                await new Promise(r => setTimeout(r, 50))
            })

            expect(screen.getByText('Load error')).toBeInTheDocument()
        })

        it('shows generic error when loading fails without message', async () => {
            mockApiCall.mockRejectedValueOnce({})

            await act(async () => {
                render(<ConflictResolutionStep {...defaultProps()} />)
                await new Promise(r => setTimeout(r, 50))
            })

            expect(screen.getByText('merge:errors.load_failed')).toBeInTheDocument()
        })
    })

    describe('conflict details display', () => {
        it('shows revision branch names', async () => {
            await renderAndLoad()

            expect(screen.getByText('feature')).toBeInTheDocument()
            expect(screen.getByText('main')).toBeInTheDocument()
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

        it('selects OURS resolution and shows resolved', async () => {
            await renderAndLoad()

            await userEvent.click(screen.getByDisplayValue('OURS'))

            expect(screen.getByText('merge:resolution.resolved')).toBeInTheDocument()
        })

        it('selects THEIRS resolution and shows resolved', async () => {
            await renderAndLoad()

            await userEvent.click(screen.getByDisplayValue('THEIRS'))

            expect(screen.getByText('merge:resolution.resolved')).toBeInTheDocument()
        })

        it('selects BASE resolution and shows resolved', async () => {
            await renderAndLoad()

            await userEvent.click(screen.getByDisplayValue('BASE'))

            expect(screen.getByText('merge:resolution.resolved')).toBeInTheDocument()
        })

        it('shows upload button when CUSTOM is selected', async () => {
            await renderAndLoad()

            await userEvent.click(screen.getByDisplayValue('CUSTOM'))

            expect(screen.getByText('merge:upload.select_file')).toBeInTheDocument()
        })
    })

    describe('compare and download', () => {
        it('calls onCompare when compare button clicked', async () => {
            const props = await renderAndLoad()

            await userEvent.click(screen.getByText('merge:compare.title'))

            expect(props.onCompare).toHaveBeenCalledWith('rules/Main.xlsx')
        })

        it('opens download URL for OURS version', async () => {
            const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null)
            try {
                await renderAndLoad()

                await userEvent.click(screen.getByText('merge:compare.download_yours'))

                expect(openSpy).toHaveBeenCalledWith(
                    expect.stringContaining('side=OURS'),
                    '_blank'
                )
            } finally {
                openSpy.mockRestore()
            }
        })

        it('opens download URL for THEIRS version', async () => {
            const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null)
            try {
                await renderAndLoad()

                await userEvent.click(screen.getByText('merge:compare.download_theirs'))

                expect(openSpy).toHaveBeenCalledWith(
                    expect.stringContaining('side=THEIRS'),
                    '_blank'
                )
            } finally {
                openSpy.mockRestore()
            }
        })

        it('opens download URL for BASE version', async () => {
            const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null)
            try {
                await renderAndLoad()

                await userEvent.click(screen.getByText('merge:compare.download_base'))

                expect(openSpy).toHaveBeenCalledWith(
                    expect.stringContaining('side=BASE'),
                    '_blank'
                )
            } finally {
                openSpy.mockRestore()
            }
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
            expect(props.onResolveSuccess).toHaveBeenCalled()
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
            const resolveCall = mockApiCall.mock.calls.find(
                call => call[0] === '/projects/proj-1/merge/conflicts/resolve'
            )
            const formData = resolveCall![1].body as FormData
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
            const resolveCall = mockApiCall.mock.calls.find(
                call => call[0] === '/projects/proj-1/merge/conflicts/resolve'
            )
            const formData = resolveCall![1].body as FormData
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
            const resolveCall = mockApiCall.mock.calls.find(
                call => call[0] === '/projects/proj-1/merge/conflicts/resolve'
            )
            const formData = resolveCall![1].body as FormData
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
        it('calls DELETE API and onCancel', async () => {
            const props = await renderAndLoad()

            mockApiCall.mockResolvedValueOnce({}) // delete

            await act(async () => {
                await userEvent.click(screen.getByText('merge:buttons.cancel'))
                await new Promise(r => setTimeout(r, 50))
            })

            expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/conflicts',
                { method: 'DELETE' },
                true
            )
            expect(props.onCancel).toHaveBeenCalled()
        })

        it('calls onCancel even when DELETE fails', async () => {
            const props = await renderAndLoad()

            mockApiCall.mockRejectedValueOnce(new Error('Delete error'))

            await act(async () => {
                await userEvent.click(screen.getByText('merge:buttons.cancel'))
                await new Promise(r => setTimeout(r, 50))
            })

            expect(props.onCancel).toHaveBeenCalled()
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
})
