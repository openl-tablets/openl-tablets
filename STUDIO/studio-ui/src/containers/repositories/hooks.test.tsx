import type { PropsWithChildren } from 'react'
import { act, renderHook, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { notification } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { MockedFunction } from 'vitest'
import { useRepositoryConfiguration } from './hooks'
import { RepositoryDataType } from './constants'
import * as services from '../../services'

vi.mock('../../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        ...actual,
        notification: { success: vi.fn(), error: vi.fn() },
    })
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const wrapper = ({ children }: PropsWithChildren) => <MemoryRouter>{children}</MemoryRouter>

const renderConfiguration = async () => {
    const { result } = renderHook(() => useRepositoryConfiguration(RepositoryDataType.DESIGN), { wrapper })
    await waitFor(() => expect(result.current.configuration).toEqual([]))
    return result
}

describe('useRepositoryConfiguration', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockApiCall.mockResolvedValue([])
    })

    it('reports a refused Apply as an error with the reason, not with the class that carries it', async () => {
        const result = await renderConfiguration()
        mockApiCall.mockRejectedValueOnce(Object.assign(new Error('Cannot be empty.'), { name: 'ApiHttpError' }))

        await act(async () => {
            await expect(result.current.updateRepositoryConfiguration([])).rejects.toThrow('Cannot be empty.')
        })

        expect(notification.error).toHaveBeenCalledWith({ title: 'common:error: Cannot be empty.' })
    })

    it('reports a refused removal as an error with the reason, not with the class that carries it', async () => {
        const result = await renderConfiguration()
        mockApiCall.mockRejectedValueOnce(Object.assign(new Error('Repository is in use.'), { name: 'ApiHttpError' }))

        await act(async () => {
            await result.current.deleteRepositoryConfiguration('design')
        })

        expect(notification.error).toHaveBeenCalledWith({ title: 'common:error: Repository is in use.' })
    })
})
