import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall, { LOCAL_LOAD_API_OPTIONS } from './apiCall'
import { getLocalHistory, restoreLocalHistory } from './localHistory'

vi.mock('./apiCall', async (importOriginal) => {
    const original = await importOriginal<typeof import('./apiCall')>()
    return { ...original, default: vi.fn() }
})

describe('localHistory', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('loads history for the requested project and module', async () => {
        const items = [{ id: '100_current', modifiedOn: 'now', current: true }]
        vi.mocked(apiCall).mockResolvedValue(items)

        await expect(getLocalHistory('design:My/Project', 'Pricing & Rules')).resolves.toEqual(items)
        expect(apiCall).toHaveBeenCalledWith(
            '/projects/design:My_Project/local-history?module=Pricing%20%26%20Rules',
            undefined,
            LOCAL_LOAD_API_OPTIONS
        )
    })

    it('omits the module query for the default module', async () => {
        vi.mocked(apiCall).mockResolvedValue([])

        await getLocalHistory('project', '')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/local-history',
            undefined,
            LOCAL_LOAD_API_OPTIONS
        )
    })

    it('restores the selected version as JSON', async () => {
        vi.mocked(apiCall).mockResolvedValue(true)

        await restoreLocalHistory('design:My/Project', 'Pricing & Rules', '100_current')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/design:My_Project/local-history/restore?module=Pricing%20%26%20Rules',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ version: '100_current' }),
            },
            { throwError: true, suppressErrorPages: true }
        )
    })

    it('restores the default module without a module query', async () => {
        vi.mocked(apiCall).mockResolvedValue(true)

        await restoreLocalHistory('project', undefined, '100_current')

        expect(apiCall).toHaveBeenCalledWith('/projects/project/local-history/restore', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ version: '100_current' }),
        }, { throwError: true, suppressErrorPages: true })
    })
})
