import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall from './apiCall'
import { getProjectMigration, migrateProject } from './migration'

vi.mock('./apiCall', () => ({
    default: vi.fn(),
}))

describe('migration service', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('reads the nested migration info through apiCall', async () => {
        vi.mocked(apiCall).mockResolvedValue({
            rulesXml: { movableRootModules: ['Pricing.xlsx'], migratable: true },
            rulesDeploy: { migratable: false },
        })

        await expect(getProjectMigration('06YZYX2FFV2RT')).resolves.toEqual({
            rulesXml: { movableRootModules: ['Pricing.xlsx'], migratable: true, newModules: []},
            rulesDeploy: { migratable: false },
        })
        expect(apiCall).toHaveBeenCalledWith('/projects/06YZYX2FFV2RT/migration', undefined, { throwError: true })
    })

    it('defaults the omitted movableRootModules and flags', async () => {
        // The API omits an empty movableRootModules and only sends the flags that apply.
        vi.mocked(apiCall).mockResolvedValue({ rulesXml: { migratable: true }, rulesDeploy: { migratable: true } })

        await expect(getProjectMigration('p')).resolves.toEqual({
            rulesXml: { movableRootModules: [], migratable: true, newModules: []},
            rulesDeploy: { migratable: true },
        })
    })

    it('passes through the workbooks a rewrite would turn into modules', async () => {
        vi.mocked(apiCall).mockResolvedValue({
            rulesXml: { migratable: true, newModules: ['rules/Extra.xlsx']},
            rulesDeploy: { migratable: false },
        })

        await expect(getProjectMigration('p')).resolves.toEqual({
            rulesXml: { movableRootModules: [], migratable: true, newModules: ['rules/Extra.xlsx']},
            rulesDeploy: { migratable: false },
        })
    })

    it('posts the migrate action with the scope', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await migrateProject('06YZYX2FFV2RT', 'rulesDeploy')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/06YZYX2FFV2RT/migrate?scope=rulesDeploy',
            { method: 'POST' },
            { throwError: true }
        )
    })
})
