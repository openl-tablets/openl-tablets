import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Dropdown, type MenuProps } from 'antd'
import {
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    FolderOpenOutlined,
    MoreOutlined,
    RocketOutlined,
} from '@ant-design/icons'
import type { Project } from '../../types/projects'

/** Actions offered from a project row's overflow menu, resolved from the project's capabilities. */
export interface ProjectListHandlers {
    onOpen: (project: Project) => void
    onCopy: (project: Project) => void
    onDeploy: (project: Project) => void
    onExport: (project: Project) => void
    onDelete: (project: Project) => void
}

interface ProjectRowActionsProps {
    project: Project
    handlers: ProjectListHandlers
}

/**
 * The per-row overflow menu (the "⋯" trigger) on the projects list. Every item is gated by a
 * server-computed capability, so the menu only offers actions the current user may perform.
 */
export const ProjectRowActions = ({ project, handlers }: ProjectRowActionsProps) => {
    const { t } = useTranslation('repository')
    const caps = project.capabilities

    const items = useMemo<MenuProps['items']>(() => {
        const list: MenuProps['items'] = []
        if (caps?.canOpen) {
            list.push({ key: 'open', icon: <FolderOpenOutlined />, label: t('browser.open') })
        }
        if (caps?.canExport) {
            list.push({ key: 'export', icon: <DownloadOutlined />, label: t('browser.export') })
        }
        if (caps?.canCopy) {
            list.push({ key: 'copy', icon: <CopyOutlined />, label: t('browser.copy') })
        }
        if (caps?.canDeploy) {
            list.push({ key: 'deploy', icon: <RocketOutlined />, label: t('browser.deploy') })
        }
        const destructive: MenuProps['items'] = []
        if (caps?.canDelete) {
            destructive.push({ key: 'delete', icon: <DeleteOutlined />, label: t('browser.delete'), danger: true })
        }
        if (list.length && destructive.length) {
            list.push({ type: 'divider' })
        }
        return [...list, ...destructive]
    }, [caps, t])

    if (!items || items.length === 0) {
        return null
    }

    const onClick: MenuProps['onClick'] = ({ key, domEvent }) => {
        domEvent.stopPropagation()
        switch (key) {
            case 'open': return handlers.onOpen(project)
            case 'export': return handlers.onExport(project)
            case 'copy': return handlers.onCopy(project)
            case 'deploy': return handlers.onDeploy(project)
            case 'delete': return handlers.onDelete(project)
        }
    }

    return (
        <Dropdown menu={{ items, onClick }} trigger={['click']}>
            <Button
                aria-label={t('home.row_actions')}
                data-testid={`project-actions-${project.id}`}
                icon={<MoreOutlined />}
                onClick={event => event.stopPropagation()}
                size="small"
                type="text"
            />
        </Dropdown>
    )
}
