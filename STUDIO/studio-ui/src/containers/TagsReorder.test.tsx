import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi, type MockedFunction } from 'vitest'
import * as services from 'services'
import type { TagType } from 'containers/tags/TagTable'
import { Tags } from 'containers/Tags'
import { notification } from 'antd'

vi.mock('services', async () => ({ apiCall: vi.fn() }))

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({ ...actual, notification: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } })
})

vi.mock('react-i18next', async () => {
    const translation = { t: (key: string) => key, i18n: { language: 'en' } }
    return {
        useTranslation: () => translation,
        Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
    }
})

// The drag itself is covered by the table's own test; here the table hands back the order a drag left.
vi.mock('containers/tags/TagTable', () => ({
    TagTable: ({ tagTypes, reorderTagTypes }: { tagTypes: TagType[], reorderTagTypes: (types: TagType[]) => void }) => (
        <>
            <span data-testid="order">{tagTypes.map(type => type.name).join(',')}</span>
            <button onClick={() => reorderTagTypes(tagTypes.toReversed())} type="button">reverse</button>
        </>
    ),
}))

vi.mock('containers/tags/FillTagsModal', () => ({ FillTagsModal: () => null }))

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const tagType = (id: number, name: string): TagType => ({ id, name, extensible: false, nullable: true, tags: []})

describe('Tags reordering', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockApiCall.mockImplementation(async url => {
            if (url === '/admin/tag-config/types') {
                return [tagType(1, 'Domain'), tagType(2, 'LOB')]
            }
            return url === '/admin/tag-config/templates' ? [] : undefined
        })
    })

    it('shows the new order at once, stores it by the ids and reads the catalog again', async () => {
        // The order is still being stored while the test looks at the screen.
        let stored: (value: unknown) => void = () => {}
        const defaultCall = mockApiCall.getMockImplementation()!
        mockApiCall.mockImplementation((url, ...rest) => url === '/admin/tag-config/types/order'
            ? new Promise(resolve => { stored = resolve })
            : defaultCall(url, ...rest))
        render(<Tags />)
        expect(await screen.findByText('Domain,LOB')).toBeInTheDocument()

        await userEvent.click(screen.getByRole('button', { name: 'reverse' }))

        expect(screen.getByTestId('order')).toHaveTextContent('LOB,Domain')
        stored(undefined)
        // The page reports a refused order itself, so no error page covers it.
        expect(mockApiCall).toHaveBeenCalledWith('/admin/tag-config/types/order', expect.objectContaining({
            method: 'PUT',
            body: JSON.stringify([2, 1]),
        }), { throwError: true, suppressErrorPages: true })
        await waitFor(() => {
            expect(mockApiCall.mock.calls.filter(([url]) => url === '/admin/tag-config/types')).toHaveLength(2)
        })
    })

    it('says when the order is refused and shows the stored order again', async () => {
        const defaultCall = mockApiCall.getMockImplementation()!
        mockApiCall.mockImplementation((url, ...rest) => (url === '/admin/tag-config/types/order'
            ? Promise.reject(new Error('Tag type is not found'))
            : defaultCall(url, ...rest)))
        render(<Tags />)
        expect(await screen.findByText('Domain,LOB')).toBeInTheDocument()

        await userEvent.click(screen.getByRole('button', { name: 'reverse' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'Tag type is not found' }))
        expect(await screen.findByText('Domain,LOB')).toBeInTheDocument()
    })
})
