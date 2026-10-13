import { afterEach, describe, expect, it, vi } from 'vitest'
import { openCompareWindow, openFileRevisionsCompareWindow } from './compare'

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

describe('openFileRevisionsCompareWindow', () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('names the file, the branch and its two revisions', () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(null)

        openFileRevisionsCompareWindow('design:Pricing', 'rules/Main.xlsx', 'main', 'b1', 'a1')

        expect(open).toHaveBeenCalledWith(
            '/studio/compare?projectId=design%3APricing&file=rules%2FMain.xlsx&branch=main&before=b1&after=a1',
            'compare_win',
            'width=1240,height=800,resizable=yes,scrollbars=yes'
        )
    })
})
