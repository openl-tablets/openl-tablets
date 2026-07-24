import { afterEach, describe, expect, it, vi } from 'vitest'
import { openCompareWindow } from './compare'

vi.mock('../../services/config', () => ({ default: { CONTEXT: '/studio' } }))

describe('openCompareWindow', () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('opens the legacy comparison popup addressed by project name and repository', () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(null)

        openCompareWindow({ name: 'My Project', repository: 'design' })

        expect(open).toHaveBeenCalledWith(
            '/studio/faces/pages/modules/repository/compare.xhtml?projectName=My%20Project&repoId=design',
            'compare_win',
            'width=1240,height=800,resizable=yes,scrollbars=yes'
        )
    })
})
