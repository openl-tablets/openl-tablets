import apiCall from './apiCall'
import { toUrlSafeId } from './projectId'

/** Which project descriptor a migrate targets. Matches the backend MigrationScope wire codes. */
export type MigrationScope = 'rulesXml' | 'rulesDeploy'

/** The rules.xml migration scope: the root workbooks a migrate would move and whether it applies. */
export interface RulesXmlMigration {
    /** Root-level workbooks a migrate would move into `rules/` — populated only when the project has no rules.xml. */
    movableRootModules: string[]
    /** Whether a migrate would move the root workbooks and write a rules.xml, or rewrite an existing one. */
    migratable: boolean
    /**
     * Workbooks a rewrite would turn into modules that rules.xml does not declare today. When non-empty the
     * rules.xml migrate is refused by the server, because it would change which modules compile.
     */
    newModules: string[]
}

/** The rules-deploy.xml migration scope. */
export interface RulesDeployMigration {
    /** Whether the project has a rules-deploy.xml that a migrate would rewrite to the minimal modern form. */
    migratable: boolean
}

/** What migrating a project to the current conventions would do, split per scope. */
export interface ProjectMigration {
    rulesXml: RulesXmlMigration
    rulesDeploy: RulesDeployMigration
}

/** Nothing to migrate — the state used before the info loads and when it fails. */
export const EMPTY_MIGRATION: ProjectMigration = {
    rulesXml: { movableRootModules: [], migratable: false, newModules: [] },
    rulesDeploy: { migratable: false },
}

/** Reads what a migrate would do for the project, so a screen can offer it only when there is something to do. */
export async function getProjectMigration(projectId: string): Promise<ProjectMigration> {
    // The API omits movableRootModules when it is empty (a project with a rules.xml), so default it here to
    // keep every caller array-safe.
    const data = await apiCall(`/projects/${toUrlSafeId(projectId)}/migration`, undefined, { throwError: true }) as {
        rulesXml?: Partial<RulesXmlMigration>
        rulesDeploy?: Partial<RulesDeployMigration>
    }
    return {
        rulesXml: {
            movableRootModules: data.rulesXml?.movableRootModules ?? [],
            migratable: data.rulesXml?.migratable ?? false,
            newModules: data.rulesXml?.newModules ?? [],
        },
        rulesDeploy: {
            migratable: data.rulesDeploy?.migratable ?? false,
        },
    }
}

/**
 * Migrates the requested scope. `rulesXml` moves the root workbooks under `rules/` and writes a rules.xml
 * (or rewrites an existing one); `rulesDeploy` rewrites rules-deploy.xml to the minimal modern form.
 */
export async function migrateProject(projectId: string, scope: MigrationScope): Promise<void> {
    await apiCall(
        `/projects/${toUrlSafeId(projectId)}/migrate?scope=${scope}`,
        { method: 'POST' },
        { throwError: true }
    )
}
