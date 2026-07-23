import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FillTagsModal, type TagFillPreview } from './FillTagsModal'
import { apiCall } from '../../services'

vi.mock('../../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
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
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Typography = {
        Text: ({ children }: Record<string, unknown>) => <span>{children as never}</span>,
        Paragraph: ({ children }: Record<string, unknown>) => <p>{children as never}</p>,
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
    { projectName: 'Closed-rules', modifiable: false, tags: [{ type: 'Domain', derived: 'Closed', state: 'assign' }]},
]

const renderModal = async (onFilled = vi.fn()) => {
    render(<FillTagsModal open onClose={vi.fn()} onFilled={onFilled} />)
    // The mount-time loads land asynchronously; flush them before the assertions read the screen.
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0))
    })
    return onFilled
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

        expect(screen.getByTestId('fill-project-Closed-rules')).toBeDisabled()
        expect(screen.getByTestId('fill-project-Policy-rules')).toBeChecked()
    })

    it('fills only the picked projects', async () => {
        const onFilled = await renderModal()
        vi.mocked(apiCall).mockResolvedValue({ updated: 1, skipped: 0 } as never)

        await userEvent.click(screen.getByTestId('fill-apply'))

        await waitFor(() => expect(apiCall).toHaveBeenCalledWith(
            '/admin/tag-config/fill',
            expect.objectContaining({ method: 'POST', body: JSON.stringify(['Policy-rules']) }),
            expect.anything()
        ))
        await waitFor(() => expect(onFilled).toHaveBeenCalledWith(1))
    })

    it('says so when every matching project already carries its tags', async () => {
        vi.mocked(apiCall).mockResolvedValue([] as never)
        await renderModal()

        expect(screen.getByTestId('fill-empty')).toBeInTheDocument()
        expect(screen.getByTestId('fill-apply')).toBeDisabled()
    })
})
