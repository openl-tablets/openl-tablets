import { fireEvent, render, screen } from '@testing-library/react'
import { Form } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import { DesignRepositoryCommentsConfiguration } from './DesignRepositoryCommentsConfiguration'

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key, i18n: { language: 'en' } }),
}))

const renderTemplates = () => render(
    <Form initialValues={{ settings: { useCustomComments: true } }}>
        <DesignRepositoryCommentsConfiguration />
    </Form>
)

describe('DesignRepositoryCommentsConfiguration', () => {
    it('refuses a user message template longer than a comment', async () => {
        renderTemplates()

        fireEvent.change(await screen.findByLabelText('repository:save_project'), { target: { value: 'a'.repeat(256) } })

        expect(await screen.findByText('repository:user_message_template_too_long')).toBeInTheDocument()
    })

    it('takes a user message template as long as a comment', async () => {
        renderTemplates()

        fireEvent.change(await screen.findByLabelText('repository:save_project'), { target: { value: 'a'.repeat(255) } })
        fireEvent.change(screen.getByLabelText('repository:create_project'), { target: { value: 'a'.repeat(256) } })

        expect(await screen.findAllByText('repository:user_message_template_too_long')).toHaveLength(1)
    })
})
