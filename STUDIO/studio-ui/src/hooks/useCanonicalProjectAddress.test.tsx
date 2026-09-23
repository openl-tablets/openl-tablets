import { renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useCanonicalProjectAddress } from './useCanonicalProjectAddress'

const { navigateMock } = vi.hoisted(() => ({ navigateMock: vi.fn() }))

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}))

const routeOf = (id: string) => `/projects/${id}?tab=files`

/** The hook on a screen whose address names the project as given; the rerender moves the address on. */
const renderAt = (projectId: string) => renderHook(
    ({ address }) => useCanonicalProjectAddress(address, routeOf),
    { initialProps: { address: projectId } }
)

describe('useCanonicalProjectAddress', () => {
    beforeEach(() => {
        navigateMock.mockReset()
    })

    it('replaces a name in the address with the id of the project it names', () => {
        const { result } = renderAt('Hello')

        expect(result.current('ZGVzaWduOkhlbGxv')).toBe(true)

        expect(navigateMock).toHaveBeenCalledWith('/projects/ZGVzaWduOkhlbGxv?tab=files', { replace: true })
    })

    it('respells an id written in the older alphabet the way the screen builds it', () => {
        const { result } = renderAt('a+b/c=')

        expect(result.current('a+b/c=')).toBe(true)

        expect(navigateMock).toHaveBeenCalledWith('/projects/a-b_c=?tab=files', { replace: true })
    })

    it('leaves an address that already names the project by its id', () => {
        const { result } = renderAt('ZGVzaWduOkhlbGxv')

        expect(result.current('ZGVzaWduOkhlbGxv')).toBe(false)

        expect(navigateMock).not.toHaveBeenCalled()
    })

    it('leaves the address to its first read: a read that follows cannot move it', () => {
        const { result } = renderAt('ZGVzaWduOkhlbGxv')
        result.current('ZGVzaWduOkhlbGxv')

        // The project read again after an action answers with an id of its own.
        expect(result.current('ZGVzaWduOk90aGVy')).toBe(false)

        expect(navigateMock).not.toHaveBeenCalled()
    })

    it('lets the first read of every new address replace it', () => {
        const { result, rerender } = renderAt('ZGVzaWduOkhlbGxv')
        result.current('ZGVzaWduOkhlbGxv')

        rerender({ address: 'Other' })

        expect(result.current('ZGVzaWduOk90aGVy')).toBe(true)
        expect(navigateMock).toHaveBeenCalledWith('/projects/ZGVzaWduOk90aGVy?tab=files', { replace: true })
    })
})
