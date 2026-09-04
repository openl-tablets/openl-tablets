import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDesignRepositoryConfig, getRepositoryConfig } from '../services/repositories'
import { useRepositoryConfig } from './useRepositoryConfig'

vi.mock('../services/repositories', () => ({
    getDesignRepositoryConfig: vi.fn(),
    getRepositoryConfig: vi.fn(),
}))

describe('useRepositoryConfig', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getRepositoryConfig).mockResolvedValue({ comment: { templates: { save: 'by project' } } })
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({ comment: { templates: { create: 'by repository' } } })
    })

    it('asks through the project, so a user granted the project alone still gets the settings', async () => {
        const { result } = renderHook(() => useRepositoryConfig({ projectId: 'p1' }))

        await waitFor(() => expect(result.current?.comment.templates.save).toBe('by project'))
        expect(getRepositoryConfig).toHaveBeenCalledWith('p1')
        expect(getDesignRepositoryConfig).not.toHaveBeenCalled()
    })

    it('asks the repository when there is no project yet', async () => {
        const { result } = renderHook(() => useRepositoryConfig({ repositoryId: 'design' }))

        await waitFor(() => expect(result.current?.comment.templates.create).toBe('by repository'))
        expect(getDesignRepositoryConfig).toHaveBeenCalledWith('design')
    })

    it('asks nothing while the form has no target', () => {
        const { result } = renderHook(() => useRepositoryConfig(null))

        expect(result.current).toBeUndefined()
        expect(getRepositoryConfig).not.toHaveBeenCalled()
        expect(getDesignRepositoryConfig).not.toHaveBeenCalled()
    })

    it('leaves the form unconfigured when the settings cannot be read', async () => {
        vi.mocked(getRepositoryConfig).mockRejectedValue(new Error('forbidden'))

        const { result } = renderHook(() => useRepositoryConfig({ projectId: 'p1' }))

        await waitFor(() => expect(getRepositoryConfig).toHaveBeenCalled())
        expect(result.current).toBeUndefined()
    })
})
