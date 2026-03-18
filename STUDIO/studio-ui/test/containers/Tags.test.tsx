import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import * as services from 'services'

jest.mock('antd/es/input/TextArea', () => {
    const React = require('react')
    return {
        __esModule: true,
        default: React.forwardRef((props: React.TextareaHTMLAttributes<HTMLTextAreaElement>, ref: React.Ref<HTMLTextAreaElement>) =>
            <textarea ref={ref} {...props} />
        ),
    }
})

jest.mock('services', () => ({
    apiCall: jest.fn(),
}))

// Must import Tags after mocks are set up
// eslint-disable-next-line @typescript-eslint/no-require-imports
const { Tags } = require('containers/Tags')

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, opts?: Record<string, unknown>) => {
            if (opts?.count !== undefined) return `${key} (${opts.count})`
            return key
        },
        i18n: { language: 'en' },
    }),
    Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
}))

jest.mock('antd', () => {
    const actual = jest.requireActual('antd')
    return {
        ...actual,
        notification: {
            success: jest.fn(),
            error: jest.fn(),
            warning: jest.fn(),
        },
    }
})

const { notification } = jest.requireMock('antd')
const mockApiCall = services.apiCall as jest.MockedFunction<typeof services.apiCall>

const mockTagTypes = [
    {
        id: 1,
        name: 'Domain',
        extensible: false,
        nullable: false,
        tags: [{ id: 1, name: 'Policy', tagTypeId: 1 }],
    },
]

describe('Tags', () => {
    beforeEach(() => {
        jest.clearAllMocks()
        mockApiCall
            .mockResolvedValueOnce(mockTagTypes)   // fetchTagTypes
            .mockResolvedValueOnce(['%Domain%-*'])  // fetchTemplates
    })

    it('renders and loads tag types and templates on mount', async () => {
        await act(async () => {
            render(<Tags />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/tag-config/types')
            expect(mockApiCall).toHaveBeenCalledWith('/admin/tag-config/templates')
        })
    })

    it('loads templates into textarea', async () => {
        await act(async () => {
            render(<Tags />)
        })

        await waitFor(() => {
            const textarea = document.querySelector('textarea')
            expect(textarea).toBeInTheDocument()
            expect(textarea?.value).toBe('%Domain%-*')
        })
    })

    it('calls save templates API on Save Templates click', async () => {
        mockApiCall
            .mockResolvedValueOnce(undefined) // saveTemplatesRequest (PUT)

        await act(async () => {
            render(<Tags />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledTimes(2) // initial fetches
        })

        const saveButton = screen.getByRole('button', { name: /tags:save_templates/i })
        await userEvent.click(saveButton)

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/admin/tag-config/templates',
                expect.objectContaining({ method: 'PUT' }),
                { throwError: true }
            )
            expect(notification.success).toHaveBeenCalledWith({
                message: 'tags:templates_saved',
            })
        })
    })

    it('shows server error message on save failure', async () => {
        mockApiCall
            .mockRejectedValueOnce(new Error('Invalid tag template: Cannot find tag type \'Foo\'.'))

        await act(async () => {
            render(<Tags />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledTimes(2)
        })

        const saveButton = screen.getByRole('button', { name: /tags:save_templates/i })
        await userEvent.click(saveButton)

        await waitFor(() => {
            expect(notification.error).toHaveBeenCalledWith({
                message: 'Invalid tag template: Cannot find tag type \'Foo\'.',
            })
        })
    })

    it('calls fill endpoint on Fill Tags click', async () => {
        mockApiCall
            .mockResolvedValueOnce(undefined) // saveTemplatesRequest (PUT)
            .mockResolvedValueOnce({ updated: 3, skipped: 1 }) // fill (POST)

        await act(async () => {
            render(<Tags />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledTimes(2)
        })

        const fillButton = screen.getByRole('button', { name: /tags:fill_tags_for_project/i })
        await userEvent.click(fillButton)

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/admin/tag-config/fill',
                expect.objectContaining({ method: 'POST' }),
                { throwError: true }
            )
            expect(notification.success).toHaveBeenCalledWith({
                message: 'tags:fill_tags_success (3)',
            })
        })
    })

    it('handles fetchTagTypes failure gracefully', async () => {
        mockApiCall.mockReset()
        mockApiCall
            .mockResolvedValueOnce(undefined) // fetchTagTypes returns undefined on error
            .mockResolvedValueOnce([])        // fetchTemplates

        await act(async () => {
            render(<Tags />)
        })

        // Should not crash — tagTypes stays as empty array
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledTimes(2)
        })
    })
})
