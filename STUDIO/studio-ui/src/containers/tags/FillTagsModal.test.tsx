import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FillTagsModal, type TagFillPreview, type TagFillResult } from './FillTagsModal'
import { apiCall } from '../../services'

vi.mock('../../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    // The options follow the key, so an assertion can see who and what a message names.
    const t = (key: string, options?: Record<string, unknown>) => options ? `${key} ${JSON.stringify(options)}` : key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: (_target, name) => String(name) }), cx: () => '' }),
}))

vi.mock('@ant-design/icons', () => ({ ArrowRightOutlined: () => <span data-testid="arrow" /> }))

// AntD's Table and Modal do not settle in jsdom; simple equivalents keep the assertions on the component.
vi.mock('antd', () => {
    interface Column { key: string, dataIndex: string, title: unknown, render?: (value: unknown, row: unknown) => unknown }
    const Modal = ({ open, title, children, onOk, okText, okButtonProps }: Record<string, unknown>) => open
        ? (
            <div>
                {title as never}
                {children as never}
                <button
                    data-testid="fill-apply"
                    disabled={(okButtonProps as { disabled?: boolean } | undefined)?.disabled}
                    onClick={onOk as never}
                >
                    {okText as never}
                </button>
            </div>
        )
        : null
    const Table = ({ columns, dataSource, ...rest }: Record<string, unknown>) => {
        const { pagination, rowKey, size, ...dom } = rest
        void pagination; void rowKey; void size
        return (
            <table {...dom}>
                <tbody>
                    {(dataSource as Record<string, unknown>[]).map(row => (
                        <tr key={row['projectName'] as string}>
                            {(columns as Column[]).map(column => (
                                <td key={column.key}>
                                    {(column.render ? column.render(row[column.dataIndex], row) : row[column.dataIndex]) as never}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        )
    }
    const Checkbox = ({ children, checked, disabled, onChange, ...rest }: Record<string, unknown>) => (
        <label>
            <input
                checked={checked as boolean}
                disabled={disabled as boolean}
                onChange={onChange as never}
                type="checkbox"
                {...rest}
            />
            {children as never}
        </label>
    )
    const Tag = ({ children, className, ...rest }: Record<string, unknown>) => (
        <span className={className as string} {...rest}>{children as never}</span>
    )
    const Alert = ({ title, showIcon, type, ...rest }: Record<string, unknown>) => {
        void showIcon; void type
        return <div {...rest}>{title as never}</div>
    }
    const Empty = ({ description, ...rest }: Record<string, unknown>) => <div {...rest}>{description as never}</div>
    const Skeleton = () => <div data-testid="fill-loading" />
    const Tooltip = ({ children, title }: Record<string, unknown>) => (
        <span data-tooltip={title as string | undefined}>{children as never}</span>
    )
    const Typography = {
        Text: ({ children, className, type, ...rest }: Record<string, unknown>) => {
            void className; void type
            return <span {...rest}>{children as never}</span>
        },
        Paragraph: ({ children, type, ...rest }: Record<string, unknown>) => {
            void type
            return <p {...rest}>{children as never}</p>
        },
    }
    return { Alert, Checkbox, Empty, Modal, Skeleton, Table, Tag, Tooltip, Typography }
})

const previews: TagFillPreview[] = [
    {
        projectName: 'Policy-rules',
        modifiable: true,
        tags: [
            { type: 'Domain', derived: 'Policy', state: 'assign' },
            { type: 'LOB', derived: 'Auto', state: 'create' },
            { type: 'Region', derived: 'Mars', state: 'rejected' },
            { type: 'Team', current: 'Payroll', derived: 'Payroll', state: 'keep' },
        ],
    },
    {
        projectName: 'Locked-rules',
        modifiable: false,
        blocker: { reason: 'locked', lockedBy: 'jdoe' },
        tags: [{ type: 'Domain', derived: 'Locked', state: 'assign' }],
    },
]

const renderModal = async (onFilled = vi.fn(), onClose = vi.fn()) => {
    render(<FillTagsModal open onClose={onClose} onFilled={onFilled} />)
    // The mount-time loads land asynchronously; flush them before the assertions read the screen.
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0))
    })
    return onFilled
}

/** Fills the picked projects, the backend answering with the given results. */
const fillWith = async (results: TagFillResult[]) => {
    vi.mocked(apiCall).mockResolvedValue(results as never)
    await userEvent.click(screen.getByTestId('fill-apply'))
    await waitFor(() => expect(screen.getByTestId('fill-result-table')).toBeInTheDocument())
}

describe('FillTagsModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(apiCall).mockResolvedValue(previews as never)
    })

    it('colors every derived tag by what filling does with it', async () => {
        await renderModal()

        expect(screen.getByTestId('fill-derived-Policy-rules-Domain')).toHaveClass('assign')
        expect(screen.getByTestId('fill-derived-Policy-rules-LOB')).toHaveClass('create')
        expect(screen.getByTestId('fill-derived-Policy-rules-Region')).toHaveClass('rejected')
        // Nothing changes for a tag the project already carries: only the grey value it has is shown.
        expect(screen.getByTestId('fill-current-Policy-rules-Team')).toHaveClass('keep')
        expect(screen.queryByTestId('fill-derived-Policy-rules-Team')).not.toBeInTheDocument()
    })

    it('cannot pick a project that is not modifiable', async () => {
        await renderModal()

        expect(screen.getByTestId('fill-project-Locked-rules')).toBeDisabled()
        expect(screen.getByTestId('fill-project-Policy-rules')).toBeChecked()
        // The project says why it cannot be picked, naming who holds it.
        const reason = screen.getByText('Locked-rules').getAttribute('data-tooltip')
        expect(reason).toContain('fill_blocker.locked')
        expect(reason).toContain('jdoe')
    })

    it('fills only the picked projects', async () => {
        const onFilled = await renderModal()

        await fillWith([{ projectName: 'Policy-rules', outcome: 'updated', tags: { Domain: 'Policy' } }])

        expect(apiCall).toHaveBeenCalledWith(
            '/admin/tag-config/fill',
            expect.objectContaining({ method: 'POST', body: JSON.stringify(['Policy-rules']) }),
            expect.anything()
        )
        // Filling may have created tag values, so whoever shows the catalog reads it again.
        expect(onFilled).toHaveBeenCalled()
    })

    it('shows what filling did to each project', async () => {
        await renderModal()

        await fillWith([
            {
                projectName: 'Policy-rules',
                outcome: 'updated',
                tags: { Domain: 'Policy', LOB: 'Auto' },
                rejected: { Region: 'Mars' },
            },
            { projectName: 'Claims-rules', outcome: 'nothingToAssign', rejected: { Domain: 'Claims' } },
            {
                projectName: 'Rating-rules',
                outcome: 'notModifiable',
                blocker: { reason: 'branchProtected', branch: 'release' },
            },
            { projectName: 'Pricing-rules', outcome: 'notModifiable', blocker: { reason: 'locked' } },
            { projectName: 'Home-rules', outcome: 'notModifiable', blocker: { reason: 'lockedByYou' } },
            { projectName: 'Billing-rules', outcome: 'failed' },
        ])

        expect(screen.getByTestId('fill-result-Policy-rules-Domain')).toHaveTextContent('Policy')
        expect(screen.getByTestId('fill-result-Policy-rules-LOB')).toHaveTextContent('Auto')
        // A value the project could not get is shown in red, and the project is not reported as left alone.
        expect(screen.getByTestId('fill-rejected-Policy-rules-Region')).toHaveClass('rejected')
        expect(screen.queryByTestId('fill-result-Policy-rules')).not.toBeInTheDocument()
        // A project left alone says why, and shows the values it could not get.
        expect(screen.getByTestId('fill-rejected-Claims-rules-Domain')).toHaveTextContent('Claims')
        expect(screen.getByTestId('fill-result-Claims-rules')).toHaveTextContent('fill_result.reason.nothingToAssign')
        expect(screen.getByTestId('fill-result-Rating-rules')).toHaveTextContent('fill_blocker.branchProtected')
        expect(screen.getByTestId('fill-result-Rating-rules')).toHaveTextContent('release')
        // A lock of someone unknown still says the project is being edited.
        expect(screen.getByTestId('fill-result-Pricing-rules')).toHaveTextContent('fill_blocker.another_user')
        // A lock the user still holds is theirs to release.
        expect(screen.getByTestId('fill-result-Home-rules')).toHaveTextContent('fill_blocker.lockedByYou')
        expect(screen.getByTestId('fill-result-Billing-rules')).toHaveTextContent('fill_result.reason.failed')
        expect(screen.getByTestId('fill-result-summary')).toHaveTextContent('fill_result.summary')
    })

    it('only closes once the projects are filled', async () => {
        const onClose = vi.fn()
        await renderModal(vi.fn(), onClose)

        await fillWith([{ projectName: 'Policy-rules', outcome: 'notModifiable' }])
        expect(screen.getByTestId('fill-apply')).toHaveTextContent('fill_result.close')
        await userEvent.click(screen.getByTestId('fill-apply'))

        expect(onClose).toHaveBeenCalled()
        // Nothing is filled a second time.
        expect(apiCall).toHaveBeenCalledTimes(2)
    })

    it('says so when filling found nothing left to do', async () => {
        await renderModal()

        vi.mocked(apiCall).mockResolvedValue([] as never)
        await userEvent.click(screen.getByTestId('fill-apply'))

        await waitFor(() => expect(screen.getByTestId('fill-result-empty')).toBeInTheDocument())
    })

    it('says so when every matching project already carries its tags', async () => {
        vi.mocked(apiCall).mockResolvedValue([] as never)
        await renderModal()

        expect(screen.getByTestId('fill-empty')).toBeInTheDocument()
        expect(screen.getByTestId('fill-apply')).toBeDisabled()
    })

    it('keeps a reopened window free of what an earlier fill answers', async () => {
        const onFilled = vi.fn()
        const view = render(<FillTagsModal open onClose={vi.fn()} onFilled={onFilled} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        let answer: (results: TagFillResult[]) => void = () => undefined
        vi.mocked(apiCall).mockReturnValueOnce(new Promise(resolve => {
            answer = resolve
        }) as never)
        await userEvent.click(screen.getByTestId('fill-apply'))

        // The window is closed while the fill runs, and opened again.
        view.rerender(<FillTagsModal onClose={vi.fn()} onFilled={onFilled} open={false} />)
        view.rerender(<FillTagsModal open onClose={vi.fn()} onFilled={onFilled} />)
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        await act(async () => {
            answer([{ projectName: 'Policy-rules', outcome: 'updated', tags: { Domain: 'Policy' } }])
        })

        // The reopened window still offers its own preview, while the catalog is read again all the same.
        expect(screen.queryByTestId('fill-result-table')).not.toBeInTheDocument()
        expect(screen.getByTestId('fill-preview-table')).toBeInTheDocument()
        expect(screen.getByTestId('fill-apply')).toHaveTextContent('fill_preview.apply')
        expect(onFilled).toHaveBeenCalled()
    })
})
