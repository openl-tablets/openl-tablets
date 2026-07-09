import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectRevisionCompareModal } from './ProjectRevisionCompareModal'
import {
    listProjectRevisionExcelFiles,
    openProjectRevisionFileCompare,
} from './projectRevisionCompare'
import type { ProjectRevision } from '../../services/repositories'

vi.mock('./projectRevisionCompare', () => ({
    listProjectRevisionExcelFiles: vi.fn(),
    openProjectRevisionFileCompare: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd', () => {
    const Modal = ({
        cancelText,
        children,
        okButtonProps,
        okText,
        onCancel,
        onOk,
        open,
        title,
    }: Record<string, unknown>) => open ? (
        <div role="dialog">
            <h4>{title as never}</h4>
            <div>{children as never}</div>
            <button onClick={onCancel as never}>{cancelText as never}</button>
            <button {...(okButtonProps as Record<string, unknown>)} onClick={onOk as never}>{okText as never}</button>
        </div>
    ) : null
    const Select = ({ onChange, options, value, ...rest }: Record<string, unknown>) => (
        <select
            onChange={event => (onChange as (next: string) => void)(event.target.value)}
            value={value as string}
            {...rest}
        >
            {(options as Array<{ label: string, value: string }>).map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    const Skeleton = () => <div>loading</div>
    const Alert = ({ title, ...rest }: Record<string, unknown>) => <div {...rest}>{title as never}</div>
    const Empty = ({ description, ...rest }: Record<string, unknown>) => {
        const { image, ...dom } = rest
        void image
        return <div {...dom}>{description as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    const notification = { error: vi.fn() }
    return { Alert, Empty, Modal, notification, Select, Skeleton }
})

const revision = (revisionNo: string): ProjectRevision => ({
    revisionNo,
    shortRevisionNo: revisionNo.slice(0, 7),
    createdAt: '2024-01-01T00:00:00Z',
    fullComment: revisionNo,
    deleted: false,
    technicalRevision: false,
})

describe('ProjectRevisionCompareModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(openProjectRevisionFileCompare).mockResolvedValue()
    })

    it('opens legacy compare for the selected common Excel file', async () => {
        vi.mocked(listProjectRevisionExcelFiles).mockResolvedValue(['Main.xlsx', 'rules/Other.xlsm'])
        const onClose = vi.fn()

        await act(async () => {
            render(
                <ProjectRevisionCompareModal
                    open
                    fromRevision={revision('old-rev')}
                    onClose={onClose}
                    projectId="p1"
                    toRevision={revision('new-rev')}
                />
            )
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        await waitFor(() => expect(listProjectRevisionExcelFiles).toHaveBeenCalledWith('p1', 'old-rev', 'new-rev'))
        await userEvent.selectOptions(screen.getByTestId('revision-compare-file'), 'rules/Other.xlsm')
        await userEvent.click(screen.getByTestId('revision-compare-submit'))

        await waitFor(() => expect(openProjectRevisionFileCompare).toHaveBeenCalledWith(
            'p1',
            'rules/Other.xlsm',
            'old-rev',
            'new-rev'
        ))
        await waitFor(() => expect(onClose).toHaveBeenCalled())
    })

    it('disables compare when selected revisions have no common Excel files', async () => {
        vi.mocked(listProjectRevisionExcelFiles).mockResolvedValue([])

        await act(async () => {
            render(
                <ProjectRevisionCompareModal
                    open
                    fromRevision={revision('old-rev')}
                    onClose={vi.fn()}
                    projectId="p1"
                    toRevision={revision('new-rev')}
                />
            )
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.getByTestId('revision-compare-empty')).toBeTruthy()
        expect((screen.getByTestId('revision-compare-submit') as HTMLButtonElement).disabled).toBe(true)
    })
})
