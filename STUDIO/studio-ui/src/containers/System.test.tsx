import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Form, Input as AntdInput, Modal, notification } from 'antd'
import type { FormInstance } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PERIOD_RULE, System, THREAD_COUNT_RULE } from './System'
import { SystemContext } from '../contexts'
import * as services from '../services'
import type { MockedFunction } from 'vitest'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        ...actual,
        Modal: { ...actual.Modal, confirm: vi.fn() },
        notification: { success: vi.fn(), error: vi.fn() },
    })
})

/** The rules each mocked text field was given, by its name. */
const inputRules = vi.hoisted(() => new Map<string, unknown[]>())

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return {
        Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
        useTranslation: () => ({ t }),
    }
})

vi.mock('../components', () => ({
    Checkbox: ({ label, name, tooltip }: { label: string, name: string, tooltip?: string }) => (
        <label title={tooltip}>
            {label}
            <input name={name} type="checkbox" />
        </label>
    ),
    Input: ({ label, name, rules }: { label: string, name: string | string[], rules?: unknown[] }) => {
        inputRules.set(String(name), rules ?? [])
        return (
            <label>
                {label}
                <input name={String(name)} type="text" />
            </label>
        )
    },
    InputNumber: ({ label, name }: { label: string, name: string | string[] }) => (
        <label>
            {label}
            <input name={String(name)} type="number" />
        </label>
    ),
    InputPassword: () => null,
}))

vi.mock('@ant-design/icons', () => ({ WarningFilled: () => null }))

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

describe('System', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockApiCall.mockResolvedValue({
            autoCompile: true,
            datePattern: 'MM/dd/yyyy',
            detectProjectsByExcelFiles: false,
            dispatchingValidationEnabled: true,
            formatTablesOnSave: true,
            projectHistoryCount: 100,
            testRunThreadCount: 4,
            timeFormat: 'hh:mm:ss a',
            updateSystemProperties: false,
            db: {
                maximumPoolSize: 50,
                password: '',
                url: 'jdbc:h2:mem:users-db',
                user: '',
            },
        })
    })

    it('shows Excel-based project detection in the Projects section with a performance warning', async () => {
        render(<System />)

        expect(await screen.findByText('system:projects')).toBeInTheDocument()
        const historyCount = screen.getByRole('spinbutton', { name: 'system:maximum_count_of_changes' })
        const clearHistory = screen.getByRole('button', { name: 'system:clear_all_history' })
        const projectHistorySettings = screen.getByTestId('project-history-settings')
        expect(projectHistorySettings).toContainElement(historyCount)
        expect(projectHistorySettings).toContainElement(clearHistory)
        const checkbox = screen.getByRole('checkbox', { name: 'system:detect_projects_by_excel_files' })
        expect(checkbox).toHaveAttribute('name', 'detectProjectsByExcelFiles')
        expect(checkbox.closest('label')).toHaveAttribute(
            'title',
            'system:detect_projects_by_excel_files_info'
        )
    })

    it('offers to format every table saved, first among the other settings, and says what it does', async () => {
        render(<System />)

        const format = await screen.findByRole('checkbox', { name: 'system:format_tables_on_save' })
        expect(format).toHaveAttribute('name', 'formatTablesOnSave')
        expect(format.closest('label')).toHaveAttribute('title', 'system:format_tables_on_save_info')
        const update = screen.getByRole('checkbox', { name: 'system:update_table_properties' })
        expect(format.compareDocumentPosition(update) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('keeps the test threads as typed and says when they are not a whole number of one at least', async () => {
        render(<System />)

        const threads = await screen.findByRole('textbox', { name: 'system:thread_number_for_tests' })
        expect(threads).toHaveAttribute('name', 'testRunThreadCount')
        expect(inputRules.get('testRunThreadCount')).toEqual([
            { ...THREAD_COUNT_RULE, message: 'system:thread_number_invalid' },
        ])
    })

    it('offers how long the audit log keeps its events and its copy to the server log where the log is kept', async () => {
        const context = { isExternalAuthSystem: false, isUserManagementEnabled: true, isGroupsManagementEnabled: true,
            isPersonalAccessTokenEnabled: true, isAuditLogEnabled: true }
        render(<SystemContext.Provider value={context}><System /></SystemContext.Provider>)

        const retention = await screen.findByRole('textbox', { name: 'system:audit_retention' })
        expect(retention).toHaveAttribute('name', 'auditRetention')
        expect(inputRules.get('auditRetention')).toEqual([
            { ...PERIOD_RULE, message: 'system:audit_retention_invalid' },
        ])
        const copy = screen.getByRole('checkbox', { name: 'system:audit_outputs_console' })
        expect(copy).toHaveAttribute('name', 'auditOutputsConsole')
        expect(copy.closest('label')).toHaveAttribute('title', 'system:audit_outputs_console_info')
    })

    it('leaves the audit log out where it is not kept, as in the single-user mode', async () => {
        render(<System />)

        expect(await screen.findByText('system:database_configuration')).toBeInTheDocument()
        expect(screen.queryByText('system:audit_log')).not.toBeInTheDocument()
        expect(screen.queryByRole('textbox', { name: 'system:audit_retention' })).not.toBeInTheDocument()
    })

    it('reports a refused Apply as an error with the reason, not with the class that carries it', async () => {
        render(<System />)
        await userEvent.click(await screen.findByRole('button', { name: 'common:btn.apply' }))
        mockApiCall.mockRejectedValueOnce(Object.assign(new Error('Cannot be empty.'), { name: 'ApiHttpError' }))

        await act(async () => {
            vi.mocked(Modal.confirm).mock.calls[0]![0].onOk!()
        })

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'common:error: Cannot be empty.' }))
    })
})

describe('THREAD_COUNT_RULE', () => {
    const validates = async (threads: number | string) => {
        let form: FormInstance | undefined
        const Harness = () => {
            const [instance] = Form.useForm()
            form = instance
            return (
                <Form form={instance} initialValues={{ threads }}>
                    <Form.Item name="threads" rules={[THREAD_COUNT_RULE]}>
                        <AntdInput />
                    </Form.Item>
                </Form>
            )
        }
        render(<Harness />)
        let valid = false
        await act(async () => {
            valid = await form!.validateFields().then(() => true, () => false)
        })
        return valid
    }

    // The stored count arrives as a number; a typed one is text, which the field keeps as it is.
    it.each<[number | string, boolean]>([
        [4, true], ['4', true], ['1', true], ['12', true], ['999999999', true],
        ['0', false], ['-5', false], ['1.1', false], ['aaa', false], ['#%', false], ['', false], [' 2', false],
        ['1000000000', false], ['9'.repeat(400), false],
    ])(
        'judges %s threads valid: %s',
        async (threads, valid) => {
            expect(await validates(threads)).toBe(valid)
        }
    )
})

describe('PERIOD_RULE', () => {
    const validates = async (retention: string | undefined) => {
        let form: FormInstance | undefined
        const Harness = () => {
            const [instance] = Form.useForm()
            form = instance
            return (
                <Form form={instance} initialValues={{ retention }}>
                    <Form.Item name="retention" rules={[PERIOD_RULE]}>
                        <AntdInput />
                    </Form.Item>
                </Form>
            )
        }
        render(<Harness />)
        let valid = false
        await act(async () => {
            valid = await form!.validateFields().then(() => true, () => false)
        })
        return valid
    }

    // A bare number names no unit: it is refused rather than read as days, months or years.
    it.each<[string | undefined, boolean]>([
        [undefined, true], ['', true], ['  ', true], ['90d', true], ['2W', true], ['6m', true], ['1y', true],
        ['P1Y6M', true], ['p10d', true], [' 1y ', true],
        ['90', false], ['0d', false], ['P', false], ['-1d', false], ['1 year', false], ['1d2y', false], ['abc', false],
    ])(
        'judges a retention of %s valid: %s',
        async (retention, valid) => {
            expect(await validates(retention)).toBe(valid)
        }
    )
})
