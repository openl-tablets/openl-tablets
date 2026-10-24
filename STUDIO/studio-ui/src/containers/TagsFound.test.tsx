import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { notification } from 'antd'
import * as services from 'services'
import { defineTags, getProjectTagCounts } from 'services/repositories'
import { Tags } from 'containers/Tags'

vi.mock('services', async () => ({ apiCall: vi.fn() }))
vi.mock('services/repositories', async () => ({ defineTags: vi.fn(), getProjectTagCounts: vi.fn() }))

vi.mock('react-i18next', async () => {
    const translation = { t: (key: string) => key, i18n: { language: 'en' } }
    return {
        useTranslation: () => translation,
        Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
    }
})

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({ ...actual, notification: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } })
})

vi.mock('containers/tags/FillTagsModal', () => ({ FillTagsModal: () => null }))

const mockApiCall = vi.mocked(services.apiCall)

const typesUrl = '/admin/tag-config/types'

describe('Tags found in projects', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockApiCall.mockImplementation(async url => {
            if (url === typesUrl) {
                return [{ id: 1, name: 'BLOB', extensible: false, nullable: true, tags: []}]
            }
            return url === '/admin/tag-config/templates' ? [] : undefined
        })
        vi.mocked(getProjectTagCounts).mockResolvedValue([
            { type: 'BLOB', values: [{ id: 'Banking', name: 'Banking', count: 2 }]},
            { type: 'Region', values: [{ id: 'EU', name: 'EU', count: 3 }, { id: 'US', name: 'US', count: 1 }]},
        ])
        vi.mocked(defineTags).mockResolvedValue(undefined)
    })

    const renderTags = () => render(<MemoryRouter><Tags /></MemoryRouter>)

    it('lists the values and the tag types the projects carry but the configuration does not', async () => {
        renderTags()

        expect(await screen.findByTestId('found-tag-BLOB-Banking')).toBeInTheDocument()
        const region = screen.getByTestId('found-type-Region')
        expect(within(region).getByTestId('found-tag-Region-EU')).toHaveTextContent('EU3')
        expect(within(region).getByTestId('found-tag-Region-US')).toBeInTheDocument()
    })

    it('adds a tag type with all its values, then reads the configuration again', async () => {
        renderTags()

        await userEvent.click(await screen.findByTestId('found-type-add-Region'))

        expect(defineTags).toHaveBeenCalledWith({ Region: ['EU', 'US']})
        await waitFor(() => expect(mockApiCall.mock.calls.filter(([url]) => url === typesUrl)).toHaveLength(2))
    })

    it('says when the tags could not be added', async () => {
        vi.mocked(defineTags).mockRejectedValue(new Error('not a valid name'))
        renderTags()

        await userEvent.click(await screen.findByTestId('found-tag-add-BLOB-Banking'))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'not a valid name' }))
    })

    it('finds nothing when the projects cannot be read', async () => {
        vi.mocked(getProjectTagCounts).mockRejectedValue(new Error('offline'))
        renderTags()

        // The configured BLOB is drawn once the page has read what it reads on opening.
        expect(await screen.findByTestId('tag-type-drag-BLOB')).toBeInTheDocument()
        expect(screen.queryByTestId('tag-types-found')).toBeNull()
        expect(screen.queryByTestId('found-tag-BLOB-Banking')).toBeNull()
    })
})
