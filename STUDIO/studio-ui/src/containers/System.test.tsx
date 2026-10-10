import { act, render, screen } from '@testing-library/react'
import { Form, Input as AntdInput } from 'antd'
import type { FormInstance } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { System, THREAD_COUNT_RULE } from './System'
import * as services from '../services'
import type { MockedFunction } from 'vitest'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

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

    it('keeps the test threads as typed and says when they are not a whole number of one at least', async () => {
        render(<System />)

        const threads = await screen.findByRole('textbox', { name: 'system:thread_number_for_tests' })
        expect(threads).toHaveAttribute('name', 'testRunThreadCount')
        expect(inputRules.get('testRunThreadCount')).toEqual([
            { ...THREAD_COUNT_RULE, message: 'system:thread_number_invalid' },
        ])
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
