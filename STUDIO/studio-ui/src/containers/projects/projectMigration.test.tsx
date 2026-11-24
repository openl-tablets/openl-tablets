import { act, renderHook, screen, waitFor } from '@testing-library/react'
import { App } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getProjectMigration, migrateProject } from '../../services/migration'
import { useDescriptorMigration } from './projectMigration'

vi.mock('../../services/migration', () => ({
    EMPTY_MIGRATION: {
        rulesXml: { movableRootModules: [], migratable: false, newModules: [] },
        rulesDeploy: { migratable: false },
    },
    getProjectMigration: vi.fn(),
    migrateProject: vi.fn(),
}))

const wrapper = ({ children }: { children: React.ReactNode }) => <App>{children}</App>

describe('useDescriptorMigration', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('does not fetch migration info when the viewer cannot write', () => {
        const { result } = renderHook(() => useDescriptorMigration('p1', false, 0, vi.fn()), { wrapper })

        expect(getProjectMigration).not.toHaveBeenCalled()
        expect(result.current.migration.rulesXml.migratable).toBe(false)
        expect(result.current.migration.rulesDeploy.migratable).toBe(false)
    })

    it('loads the migration info for a writable project', async () => {
        vi.mocked(getProjectMigration).mockResolvedValue({
            rulesXml: { movableRootModules: ['Pricing.xlsx'], migratable: true, newModules: [] },
            rulesDeploy: { migratable: false },
        })

        const { result } = renderHook(() => useDescriptorMigration('p1', true, 0, vi.fn()), { wrapper })

        await waitFor(() => expect(result.current.migration.rulesXml.migratable).toBe(true))
        expect(getProjectMigration).toHaveBeenCalledWith('p1')
        expect(result.current.migration.rulesXml.movableRootModules).toEqual(['Pricing.xlsx'])
    })

    it('updates the migrated scope locally on success and calls onMigrated, without waiting for a refetch', async () => {
        vi.mocked(getProjectMigration).mockResolvedValue({
            rulesXml: { movableRootModules: [], migratable: false, newModules: [] },
            rulesDeploy: { migratable: true },
        })
        vi.mocked(migrateProject).mockResolvedValue(undefined)
        const onMigrated = vi.fn()

        const { result } = renderHook(() => useDescriptorMigration('p1', true, 0, onMigrated), { wrapper })
        await waitFor(() => expect(result.current.migration.rulesDeploy.migratable).toBe(true))

        await act(async () => {
            await result.current.run('rulesDeploy', 'Migration failed')
        })

        expect(migrateProject).toHaveBeenCalledWith('p1', 'rulesDeploy')
        expect(result.current.migration.rulesDeploy.migratable).toBe(false)
        expect(result.current.migrating).toBe(false)
        expect(onMigrated).toHaveBeenCalled()
    })

    it('surfaces a failure notification and resets migrating, without calling onMigrated', async () => {
        vi.mocked(getProjectMigration).mockResolvedValue({
            rulesXml: { movableRootModules: [], migratable: false, newModules: [] },
            rulesDeploy: { migratable: true },
        })
        vi.mocked(migrateProject).mockRejectedValue(new Error('disk full'))
        const onMigrated = vi.fn()

        const { result } = renderHook(() => useDescriptorMigration('p1', true, 0, onMigrated), { wrapper })
        await waitFor(() => expect(result.current.migration.rulesDeploy.migratable).toBe(true))

        await act(async () => {
            await result.current.run('rulesDeploy', 'Migration failed')
        })

        expect(await screen.findByText('Migration failed')).toBeInTheDocument()
        expect(await screen.findByText('disk full')).toBeInTheDocument()
        expect(result.current.migrating).toBe(false)
        expect(onMigrated).not.toHaveBeenCalled()
        // A failed migrate leaves the previously-loaded state untouched.
        expect(result.current.migration.rulesDeploy.migratable).toBe(true)
    })
})
