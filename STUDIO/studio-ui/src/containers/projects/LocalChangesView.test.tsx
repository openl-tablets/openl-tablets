import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getLocalHistory, restoreLocalHistory } from '../../services/localHistory'
import { LocalChangesView } from './LocalChangesView'

vi.mock('../../services/localHistory', () => ({
    getLocalHistory: vi.fn(),
    restoreLocalHistory: vi.fn(),
}))

const { translate } = vi.hoisted(() => ({
    translate: (key: string, values?: { count?: number; modifiedOn?: string }) => {
        if (values?.modifiedOn) {
            return `${key}:${values.modifiedOn}`
        }
        return values?.count === undefined ? key : `${key}:${values.count}`
    },
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: translate }),
}))

vi.mock('antd', () => ({
    Alert: ({ title }: { title: React.ReactNode }) => <div role="alert">{title}</div>,
    Button: ({ children, disabled, onClick, ...props }: {
        children: React.ReactNode
        disabled?: boolean
        onClick?: () => void
        type?: string
        'data-testid'?: string
    }) => (
        <button
            data-testid={props['data-testid']}
            disabled={disabled}
            onClick={onClick}
            type="button"
        >
            {children}
        </button>
    ),
    Checkbox: ({ checked, onChange, ...props }: {
        checked?: boolean
        onChange?: (event: { target: { checked: boolean } }) => void
        'aria-label'?: string
    }) => (
        <input
            aria-label={props['aria-label']}
            checked={checked}
            onChange={event => onChange?.({ target: { checked: event.target.checked } })}
            type="checkbox"
        />
    ),
    Empty: Object.assign(
        ({ description }: { description: React.ReactNode }) => <div>{description}</div>,
        { PRESENTED_IMAGE_SIMPLE: 'simple' }
    ),
    Modal: ({ children, onOk, open, title }: {
        children: React.ReactNode
        onOk: () => void
        open: boolean
        title: React.ReactNode
    }) => open ? (
        <div role="dialog">
            <h2>{title}</h2>
            {children}
            <button onClick={onOk}>confirm restore</button>
        </div>
    ) : null,
    notification: { error: vi.fn(), success: vi.fn() },
    Spin: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    Typography: {
        Link: ({ children, onClick }: {
            children: React.ReactNode
            onClick?: React.MouseEventHandler<HTMLAnchorElement>
        }) => <a href="#restore" onClick={onClick}>{children}</a>,
    },
}))

const history = [
    { id: '200_current', modifiedOn: 'Today', current: true },
    { id: '100', modifiedOn: 'Yesterday' },
    { id: 'Revision Version', modifiedOn: 'Revision Version' },
]

describe('LocalChangesView', () => {
    const open = vi.fn()
    const reload = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getLocalHistory).mockResolvedValue(history)
        vi.mocked(restoreLocalHistory).mockResolvedValue()
        vi.stubGlobal('open', open)
        vi.stubGlobal('ws', { nav: { reload } })
    })

    it('loads the selected module and compares the two most recently selected versions', async () => {
        render(<LocalChangesView moduleName="Pricing" projectId="p1" />)

        expect(await screen.findByText('Yesterday')).toBeInTheDocument()
        expect(screen.getByRole('heading', { level: 1, name: 'browser.local_history.title' }))
            .toBeInTheDocument()
        expect(screen.getByTestId('local-changes-count')).toHaveTextContent('browser.local_history.summary:2')
        expect(getLocalHistory).toHaveBeenCalledWith('p1', 'Pricing')
        const compare = screen.getByTestId('compare-local-history')
        expect(compare).toBeDisabled()

        await userEvent.click(screen.getByLabelText('browser.local_history.select_version:Yesterday'))
        expect(compare).toBeEnabled()
        await userEvent.click(compare)

        expect(open).toHaveBeenCalledWith(
            expect.stringContaining('projectId=p1&module=Pricing&version1=100&version2=200_current'),
            'Compare',
            expect.stringContaining('width=1240')
        )
    })

    it('restores a version, refreshes history and reloads the legacy workspace', async () => {
        render(<LocalChangesView moduleName="Pricing" projectId="p1" />)
        await screen.findByText('Yesterday')

        await userEvent.click(screen.getAllByRole('link', { name: 'browser.local_history.restore' })[0]!)
        expect(screen.getByRole('dialog')).toHaveTextContent(
            'browser.local_history.confirm_restore_message:Yesterday'
        )
        await userEvent.click(screen.getByText('confirm restore'))

        await waitFor(() => expect(restoreLocalHistory).toHaveBeenCalledWith('p1', 'Pricing', '100'))
        await waitFor(() => expect(getLocalHistory).toHaveBeenCalledTimes(2))
        expect(notification.success).toHaveBeenCalledWith({
            title: 'browser.local_history.restore_succeeded',
        })
        expect(reload).toHaveBeenCalledWith(true)
    })

    it('shows the empty state when the module has no local changes', async () => {
        vi.mocked(getLocalHistory).mockResolvedValue([])

        render(<LocalChangesView moduleName="Pricing" projectId="p1" />)

        expect(await screen.findByText('browser.local_history.empty')).toBeInTheDocument()
        expect(screen.queryByTestId('compare-local-history')).toBeNull()
    })
})
