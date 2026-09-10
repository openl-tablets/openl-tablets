import { afterEach, describe, expect, it, vi } from 'vitest'
import { openCompareWindow } from './compare'

vi.mock('../../services/config', () => ({ default: { CONTEXT: '/studio' } }))

describe('openCompareWindow', () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('opens the comparison window for the project, which picks what to compare itself', () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(null)

        openCompareWindow({ id: 'design:My Project' })

        expect(open).toHaveBeenCalledWith(
            '/studio/compare?projectId=design%3AMy%20Project',
            'compare_win',
            'width=1240,height=800,resizable=yes,scrollbars=yes'
        )
    })
})
