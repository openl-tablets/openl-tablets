import { useEffect, useRef, useState } from 'react'
import { App } from 'antd'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../utils/errorMessage'
import { getProjects } from '../../services/repositories'
import type { Project } from '../../types/projects'
import { ValueText } from './ValueText'
import { CrumbSwitcher } from './CrumbSwitcher'

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
    testId?: string
}

/**
 * Opens another project from the breadcrumb, the way the branch beside it is switched.
 *
 * <p>The projects are listed when the list first opens, not before: a workspace holding hundreds of them takes
 * seconds to answer, which is a wait worth nothing to a reader who never opens the list. While it is being
 * read the list says so, and once it is read the search narrows it without asking the server again.
 */
export const ProjectSwitcher = ({
    currentProjectId,
    currentName,
    onSelect,
    disabled = false,
    testId = 'project-switcher',
}: ProjectSwitcherProps) => {
    const { notification } = App.useApp()
    const { t } = useTranslation('repository')
    const [projects, setProjects] = useState<Project[] | null>(null)
    const [loading, setLoading] = useState(false)
    const [switching, setSwitching] = useState(false)
    const alive = useRef(true)

    useEffect(() => () => { alive.current = false }, [])

    const load = async () => {
        if (projects !== null || loading) {
            return
        }
        setLoading(true)
        try {
            const page = await getProjects({ unpaged: true, sort: 'name', fields: LIST_FIELDS })
            if (alive.current) {
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
        setSwitching(true)
        try {
            await onSelect(projectId)
        } finally {
            if (alive.current) {
                setSwitching(false)
            }
        }
    }

    const items = projects === null ? null : projects.map(project => ({
        key: project.id,
        label: <ValueText ellipsis>{project.name}</ValueText>,
        search: project.name,
    }))

    return (
        <CrumbSwitcher
            busy={switching}
            current={<ValueText ellipsis data-testid={testId}>{currentName}</ValueText>}
            disabled={disabled}
            emptyText={t('browser.module.project_no_match')}
            items={items}
            loading={loading}
            onFollow={() => void switchTo(currentProjectId)}
            onOpen={() => void load()}
            onSelect={projectId => void switchTo(projectId)}
            searchPlaceholder={t('browser.module.project_filter')}
            selectedKey={currentProjectId}
            testId={testId}
        />
    )
}

export default ProjectSwitcher
