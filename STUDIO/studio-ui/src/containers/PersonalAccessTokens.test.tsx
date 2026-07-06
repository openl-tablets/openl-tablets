import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { PersonalAccessTokens } from './PersonalAccessTokens'
import { SystemContext } from '../contexts'
import * as services from '../services'
import type { MockedFunction } from 'vitest'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('./PersonalAccessTokens.styles', () => ({
    useStyles: () => ({ styles: {}, cx: (...names: unknown[]) => names.filter(Boolean).join(' ') }),
}))

// Ant Design Table hangs act() in jsdom (see AGENTS.md) — mock antd with plain HTML equivalents.
vi.mock('antd', () => {
    const formInstance = {
        submit: () => formInstance._onFinish?.({ name: 'ci-token', expirationOption: '7_days' }),
        resetFields: vi.fn(),
        _onFinish: undefined as ((values: unknown) => void) | undefined,
    }
    const Form = ({ children, onFinish }: { children?: React.ReactNode, onFinish?: (values: unknown) => void }) => {
        formInstance._onFinish = onFinish
        return <form>{children}</form>
    }
    Form.Item = ({ children, label }: { children?: React.ReactNode, label?: React.ReactNode }) => (
        <div>
            {label}
            {children}
        </div>
    )
    Form.useForm = () => [formInstance]
    const Typography = {
        Title: ({ children }: { children?: React.ReactNode }) => <h4>{children}</h4>,
        Paragraph: ({ children }: { children?: React.ReactNode }) => <p>{children}</p>,
        Text: ({ children }: { children?: React.ReactNode }) => <span>{children}</span>,
    }
    return {
        Alert: ({ title }: { title?: React.ReactNode }) => <div>{title}</div>,
        Button: ({ children, icon, onClick, ...rest }: {
            children?: React.ReactNode
            icon?: React.ReactNode
            onClick?: () => void
            'data-testid'?: string
            'aria-label'?: string
        }) => (
            <button
                aria-label={rest['aria-label']}
                data-testid={rest['data-testid']}
                onClick={onClick}
                type="button"
            >
                {icon}
                {children}
            </button>
        ),
        DatePicker: () => <input />,
        Drawer: ({ open, title, extra, children }: {
            open?: boolean
            title?: React.ReactNode
            extra?: React.ReactNode
            children?: React.ReactNode
        }) => (open ? (
            <div role="dialog">
                {title}
                {extra}
                {children}
            </div>
        ) : null),
        Empty: ({ description, children }: { description?: React.ReactNode, children?: React.ReactNode }) => (
            <div>
                {description}
                {children}
            </div>
        ),
        Form,
        Input: () => <input />,
        Modal: { confirm: vi.fn() },
        notification: { error: vi.fn(), success: vi.fn() },
        Select: () => <select />,
        Space: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
        Table: () => <table data-testid="pat-table" />,
        Tag: ({ children }: { children?: React.ReactNode }) => <span>{children}</span>,
        Tooltip: ({ open, title, children }: {
            open?: boolean
            title?: React.ReactNode
            children?: React.ReactNode
        }) => (
            <>
                {children}
                {open ? <div>{title}</div> : null}
            </>
        ),
        Typography,
    }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const writeText = vi.fn()

const systemContextValue = {
    isPersonalAccessTokenEnabled: true,
} as React.ContextType<typeof SystemContext>

const renderPat = async (contextValue = systemContextValue) => {
    await act(async () => {
        render(
            <SystemContext.Provider value={contextValue}>
                <PersonalAccessTokens />
            </SystemContext.Provider>
        )
    })
}

describe('PersonalAccessTokens', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        writeText.mockResolvedValue(undefined)
        Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } })
    })

    afterEach(() => {
        Object.defineProperty(navigator, 'clipboard', { configurable: true, value: undefined })
    })

    it('renders nothing when personal access tokens are disabled', async () => {
        mockApiCall.mockResolvedValue([])
        const disabled = { isPersonalAccessTokenEnabled: false } as React.ContextType<typeof SystemContext>
        await renderPat(disabled)
        expect(screen.queryByText('pat:title')).not.toBeInTheDocument()
    })

    it('creates a token and copies it to clipboard with feedback', async () => {
        mockApiCall.mockResolvedValueOnce([]) // initial token list

        await renderPat()
        expect(screen.getByText('pat:no_tokens')).toBeInTheDocument()

        // Open the create drawer and submit the form
        await userEvent.click(screen.getByRole('button', { name: /pat:create_token/ }))
        const createdToken = {
            publicId: 'id-1',
            name: 'ci-token',
            loginName: 'admin',
            createdAt: '2026-07-06T00:00:00Z',
            expiresAt: null,
            token: 'SECRET123',
        }
        mockApiCall.mockResolvedValueOnce(createdToken) // POST create
        mockApiCall.mockResolvedValueOnce([createdToken]) // refetch token list
        await userEvent.click(screen.getByRole('button', { name: /common:btn.create/ }))

        // The created token is shown once and can be copied
        await waitFor(() => expect(screen.getByText('SECRET123')).toBeInTheDocument())
        await userEvent.click(screen.getByTestId('pat-copy-token'))

        await waitFor(() => expect(writeText).toHaveBeenCalledWith('SECRET123'))
        // The "copied" tooltip driven by the shared hook state is shown
        await waitFor(() => expect(screen.getByText('pat:token_copied')).toBeInTheDocument())
        expect(notification.error).not.toHaveBeenCalled()
    })
})
