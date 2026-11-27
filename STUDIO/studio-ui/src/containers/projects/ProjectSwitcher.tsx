import { useEffect, useMemo, useRef, useState } from 'react'
import { App } from 'antd'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../utils/errorMessage'
import { getProjects } from '../../services/repositories'
import type { Project } from '../../types/projects'
import { recentProjects } from '../../utils/recentChoices'
import { useRecentChoices } from '../../hooks/useRecentChoices'
import { ValueText } from './ValueText'
import { useSharedStyles } from './sharedStyles'
import { CrumbSwitcher } from '../../components/CrumbSwitcher'

/** What the list asks the server for: the names to choose from, and nothing that costs time to work out. */
const LIST_FIELDS = 'id,name,status'

interface ProjectSwitcherProps {
    currentProjectId: string
    currentName: string
    /**
     * A project was chosen — another one, or the one the breadcrumb already names, which is how a reader
     * goes from a module back to the project holding it. A promise is awaited, so the switch counts as
     * running until the screen shows the project that was picked.
     */
    onSelect: (projectId: string) => void | Promise<unknown>
    /** Blocks the switch while what it would leave is busy with something of its own. */
    disabled?: boolean | undefined
    /**
     * Whether the name leads to the project itself, the list opened from the caret beside it — as on the screen of a
     * module. Without the link, on the screen of the project, the name opens the list too, and picking the project
     * already shown does nothing.
     */
    withLink?: boolean | undefined
    testId?: string
}

/**
 * Opens another project from the breadcrumb, the way the branch beside it is switched.
 *
 * <p>The projects are listed when the list first opens, not before: a workspace holding hundreds of them takes
 * seconds to answer, which is a wait worth nothing to a reader who never opens the list. While it is being
 * read the list says so, and once it is read the search narrows it without asking the server again.
 *
 * <p>The list leads with the projects opened most recently in this browser, then lists the others by name. A project
 * the list no longer holds, one removed, is forgotten among the recent ones.
 */
export const ProjectSwitcher = ({
    currentProjectId,
    currentName,
    onSelect,
    disabled = false,
    withLink = true,
    testId = 'project-switcher',
}: ProjectSwitcherProps) => {
    const { notification } = App.useApp()
    const { styles: shared } = useSharedStyles()
    const { t } = useTranslation('repository')
    const [projects, setProjects] = useState<Project[] | null>(null)
    const [loading, setLoading] = useState(false)
    const [switching, setSwitching] = useState(false)
    const alive = useRef(true)
    const recent = useRecentChoices(recentProjects)

    useEffect(() => () => { alive.current = false }, [])

    const load = async () => {
        if (projects !== null || loading) {
            return
        }
        setLoading(true)
        try {
            const page = await getProjects({ unpaged: true, sort: 'name', fields: LIST_FIELDS })
            if (alive.current) {
                recentProjects.keepOnly(page.content.map(project => project.id))
                setProjects(page.content)
            }
        } catch (e) {
            if (alive.current) {
                notification.error({
                    title: t('browser.module.project_list_failed'),
                    description: errorMessage(e),
                })
            }
        } finally {
            if (alive.current) {
                setLoading(false)
            }
        }
    }

    const switchTo = async (projectId: string) => {
        if (!withLink && projectId === currentProjectId) {
            return
        }
        setSwitching(true)
        try {
            await onSelect(projectId)
        } finally {
            if (alive.current) {
                setSwitching(false)
            }
        }
    }

    const items = useMemo(() => projects?.map(project => ({
        key: project.id,
        label: <ValueText ellipsis>{project.name}</ValueText>,
        search: project.name,
    })) ?? null, [projects])

    return (
        <CrumbSwitcher
            busy={switching}
            current={<span className={shared.ellipsis} data-testid={testId}>{currentName}</span>}
            disabled={disabled}
            emptyText={t('browser.module.project_no_match')}
            items={items}
            loading={loading}
            onFollow={withLink ? () => void switchTo(currentProjectId) : undefined}
            onOpen={() => void load()}
            onSelect={projectId => void switchTo(projectId)}
            recent={recent}
            searchPlaceholder={t('browser.module.project_filter')}
            selectedKey={currentProjectId}
            testId={testId}
        />
    )
}

export default ProjectSwitcher
