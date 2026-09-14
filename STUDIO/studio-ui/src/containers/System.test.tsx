import React from 'react'
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { System } from './System'
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
    InputNumber: ({ label, name }: { label: string, name: string }) => (
        <label>
            {label}
            <input name={name} type="number" />
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
})
