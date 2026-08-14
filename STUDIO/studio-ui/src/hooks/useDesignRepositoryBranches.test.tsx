import { render, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDesignRepositoryBranches } from '../services/repositories'
import { useDesignRepositoryBranches, type DesignRepositoryBranches } from './useDesignRepositoryBranches'

vi.mock('../services/repositories', () => ({ getDesignRepositoryBranches: vi.fn() }))

describe('useDesignRepositoryBranches', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getDesignRepositoryBranches).mockResolvedValue(['main', 'release'])
    })

    it('reports the reading, then the branches it read', async () => {
        const { result } = renderHook(() => useDesignRepositoryBranches('design'))

        // The empty list means "not known yet" only for as long as the reading says so.
        expect(result.current.loading).toBe(true)
        expect(result.current.branches).toEqual([])

        await waitFor(() => expect(result.current.branches).toEqual(['main', 'release']))
        expect(result.current.loading).toBe(false)
        expect(getDesignRepositoryBranches).toHaveBeenCalledWith('design')
    })

    it('asks nothing, and reads as settled, while the form has no repository', () => {
        const { result } = renderHook(() => useDesignRepositoryBranches(null))

        expect(result.current).toEqual({ branches: [], loading: false })
        expect(getDesignRepositoryBranches).not.toHaveBeenCalled()
    })

    it('settles on an empty list when the branches cannot be read, so the form still accepts a typed name', async () => {
        vi.mocked(getDesignRepositoryBranches).mockRejectedValue(new Error('unavailable'))

        const { result } = renderHook(() => useDesignRepositoryBranches('design'))

        await waitFor(() => expect(result.current.loading).toBe(false))
        expect(result.current.branches).toEqual([])
    })

    it('reads again for another repository and never shows the previous branches', async () => {
        const { result, rerender } = renderHook(({ id }) => useDesignRepositoryBranches(id), {
            initialProps: { id: 'design' as string | null },
        })
        await waitFor(() => expect(result.current.branches).toEqual(['main', 'release']))

        vi.mocked(getDesignRepositoryBranches).mockResolvedValue(['prod-only'])
        rerender({ id: 'prod' })

        expect(result.current.branches).toEqual([])
        await waitFor(() => expect(result.current.branches).toEqual(['prod-only']))
    })

    it('offers no branch of the repository asked about before, in any render along the way', async () => {
        // `renderHook` flushes the effects before it hands the value over, so it cannot see the render in
        // between — the one where the new repository is already asked about while only the previous
        // repository has answered. Every render is therefore recorded as it happens.
        const rendered: { id: string | null, state: DesignRepositoryBranches }[] = []
        const Probe = ({ id }: { id: string | null }) => {
            rendered.push({ id, state: useDesignRepositoryBranches(id) })
            return null
        }
        const { rerender } = render(<Probe id="design" />)
        await waitFor(() => expect(rendered.at(-1)?.state.branches).toEqual(['main', 'release']))

        vi.mocked(getDesignRepositoryBranches).mockResolvedValue(['prod-only'])
        rerender(<Probe id="prod" />)
        await waitFor(() => expect(rendered.at(-1)?.state.branches).toEqual(['prod-only']))

        const askedAboutProd = rendered.filter(entry => entry.id === 'prod')
        expect(askedAboutProd[0]?.state).toEqual({ branches: [], loading: true })
        expect([...new Set(askedAboutProd.flatMap(entry => entry.state.branches))]).toEqual(['prod-only'])
    })

    it('ignores a stale answer that lands after the repository changed', async () => {
        let resolveDesign!: (branches: string[]) => void
        vi.mocked(getDesignRepositoryBranches).mockImplementation(id => id === 'design'
            ? new Promise<string[]>(resolve => {
                resolveDesign = resolve
            })
            : Promise.resolve(['prod-only']))
        const { result, rerender } = renderHook(({ id }) => useDesignRepositoryBranches(id), {
            initialProps: { id: 'design' as string | null },
        })

        rerender({ id: 'prod' })
        await waitFor(() => expect(result.current.branches).toEqual(['prod-only']))
        resolveDesign(['design-only'])

        await waitFor(() => expect(result.current.loading).toBe(false))
        expect(result.current.branches).toEqual(['prod-only'])
    })
})
