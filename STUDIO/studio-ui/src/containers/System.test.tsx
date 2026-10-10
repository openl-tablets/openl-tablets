import { act, render, screen } from '@testing-library/react'
import { Form, InputNumber as AntdInputNumber } from 'antd'
import type { FormInstance } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { System, THREAD_COUNT_RULE } from './System'
import * as services from '../services'
import type { MockedFunction } from 'vitest'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

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
    Input: () => null,
    InputNumber: ({ label, name, rules }: { label: string, name: string | string[], rules?: unknown[] }) => (
        <label data-rules={JSON.stringify(rules ?? [])}>
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

    it('accepts a whole number of test threads, one at least, and says so otherwise', async () => {
        render(<System />)

        const threads = await screen.findByRole('spinbutton', { name: 'system:thread_number_for_tests' })
        expect(JSON.parse(threads.closest('label')!.dataset['rules']!)).toEqual([
            { ...THREAD_COUNT_RULE, message: 'system:thread_number_invalid' },
        ])
    })
})

describe('THREAD_COUNT_RULE', () => {
    const validates = async (threads: number) => {
        let form: FormInstance | undefined
        const Harness = () => {
            const [instance] = Form.useForm()
            form = instance
            return (
                <Form form={instance} initialValues={{ threads }}>
                    <Form.Item name="threads" rules={[THREAD_COUNT_RULE]}>
                        <AntdInputNumber />
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

    it.each([[4, true], [1, true], [0, false], [-5, false], [1.1, false]])(
        'judges %s threads valid: %s',
        async (threads, valid) => {
            expect(await validates(threads)).toBe(valid)
        }
    )
})
