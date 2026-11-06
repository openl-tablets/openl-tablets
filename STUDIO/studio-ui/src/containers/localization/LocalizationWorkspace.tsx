import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Alert, Empty, Skeleton } from 'antd'
import type { Project } from '../../types/projects'
import type { MessageFile } from '../../types/localization'
import { getProject } from '../../services/repositories'
import { listModules, type ModuleInfo } from '../../services/modules'
import { LOCAL_LOAD_API_OPTIONS } from '../../services/apiCall'
import { readMessageFiles } from '../../services/localization'
import { isUnresolvedProjectLink } from '../../services/projectLink'
import { errorMessage } from '../../utils/errorMessage'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { WorkspaceHeader } from '../../components/WorkspaceHeader'
import { CompileProblemsPanel } from '../projects/CompileProblemsPanel'
import { CrumbSwitcher } from '../projects/CrumbSwitcher'
import { useSharedStyles } from '../projects/sharedStyles'
import { UnresolvedProjectLink } from '../projects/UnresolvedProjectLink'
import { ValueText } from '../projects/ValueText'
import { buildBundle, languageName } from './bundle'
import { KeyPanel } from './KeyPanel'
import { KeyRail } from './KeyRail'
import { useStyles } from './LocalizationWorkspace.styles'
import { fileTextOf } from './MessageText'
import { MessagesTable } from './MessagesTable'
import { LOCALIZATION_ITEM, switchedRoute, switcherItems } from './moduleSwitcher'
import { useElementSize } from './useElementSize'

/**
 * The messages of a project in all their languages: the `message*.properties` files of its `i18n` folder.
 *
 * <p>The messages belong to the project rather than to one of its modules, so they have a screen of their own,
 * reached from the module switcher. The rail lists the keys grouped by their dot-separated parts. Its root opens
 * the table of every key in every language, a key opens that key alone. The address keeps the key and the language
 * the reader is on (`key`, `lang`), so a link leads straight to them.
 *
 * <p>The default language comes first, then the others by name, a regional language next to its parent. A key a
 * language lacks shows, faint, the text the language falls back to and where that text comes from.
 */
export const LocalizationWorkspace = () => {
    const { t, i18n } = useTranslation('localization')
    const { styles } = useStyles()
    const { styles: shared } = useSharedStyles()
    const navigate = useNavigate()
    const { projectId = '' } = useParams()
    const [search, setSearch] = useSearchParams()
    const selectedKey = search.get('key')
    const askedLanguage = search.get('lang')
    const uiLocale = i18n.resolvedLanguage ?? i18n.language

    const [project, setProject] = useState<Project | null>(null)
    const [statusReadAt, setStatusReadAt] = useState(0)
    const [linkProblem, setLinkProblem] = useState(false)
    const [modules, setModules] = useState<ModuleInfo[] | null>(null)
    const [files, setFiles] = useState<MessageFile[] | null>(null)
    const [failure, setFailure] = useState<string | null>(null)
    const { ref: canvasRef, size } = useElementSize<HTMLDivElement>()

    useEffect(() => {
        let current = true
        setProject(null)
        setLinkProblem(false)
        const startedAt = Date.now()
        getProject(projectId, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS)
            .then(loaded => {
                if (current) {
                    setProject(loaded)
                    setStatusReadAt(startedAt)
                }
            })
            .catch((error: unknown) => {
                if (current) {
                    if (isUnresolvedProjectLink(error)) {
                        setLinkProblem(true)
                    } else {
                        setFailure(errorMessage(error))
                    }
                }
            })
        return () => {
            current = false
        }
    }, [projectId])

    // The files are the branch's: they are read once the project says which branch it stands on.
    useEffect(() => {
        if (project === null) {
            return undefined
        }
        let current = true
        setFailure(null)
        listModules(projectId).then(setModules).catch(() => setModules([]))
        readMessageFiles(projectId)
            .then(read => {
                if (current) {
                    setFiles(read)
                }
            })
            .catch((error: unknown) => {
                if (current) {
                    setFailure(errorMessage(error))
                }
            })
        return () => {
            current = false
        }
    }, [projectId, project])

    const defaultName = t('default_language')
    const nameOf = useCallback((tag: string) => languageName(tag, uiLocale, defaultName), [uiLocale, defaultName])
    const bundle = useMemo(() => (files === null ? null : buildBundle(files, nameOf)), [files, nameOf])
    const keys = bundle?.keys ?? null
    const shownKey = selectedKey !== null && keys?.includes(selectedKey) === true ? selectedKey : null

    const openKey = useCallback((key: string | null) => {
        setSearch(key === null ? {} : { key })
    }, [setSearch])

    // A file read in a legacy encoding is written in UTF-8 when it is saved, which the reader is told up front.
    const legacy = files?.filter(file => file.legacy).map(file => file.name) ?? []

    const title = t('title')
    const moduleItems = useMemo(() => switcherItems(modules, title), [modules, title])

    if (linkProblem) {
        return (
            <div className={shared.workspacePage}>
                <div className={styles.centered}><UnresolvedProjectLink /></div>
            </div>
        )
    }
    if (project === null) {
        return (
            <div className={shared.workspacePage}>
                <div className={styles.centered} data-testid="localization-loading">
                    {failure === null ? <Skeleton active /> : <Alert showIcon title={failure} type="error" />}
                </div>
            </div>
        )
    }

    const branch = project.branch ?? null
    const hasBranches = supportsBranches({ features: project.repositoryInfo?.features }) && branch !== null
    const crumbs = (
        <>
            <Link to="/projects">{t('repository:home.title')}</Link>
            <span aria-hidden>/</span>
            <ValueText className={styles.crumbValue}>{project.repositoryInfo?.name ?? project.repository}</ValueText>
            <span aria-hidden>/</span>
            <Link data-testid="localization-crumb-project" to={`/projects/${project.id}`}>{project.name}</Link>
            {hasBranches && (
                <>
                    <span aria-hidden>/</span>
                    <ValueText className={styles.crumbValue}>{branch}</ValueText>
                </>
            )}
        </>
    )

    const canvas = () => {
        if (failure !== null) {
            return <Alert showIcon description={failure} title={t('load_failed')} type="error" />
        }
        if (bundle === null) {
            return <Skeleton active />
        }
        if (bundle.languages.length === 0) {
            return <Empty className={styles.centered} description={t('no_files')} />
        }
        if (shownKey !== null) {
            return (
                <KeyPanel
                    askedLanguage={askedLanguage}
                    bundle={bundle}
                    messageKey={shownKey}
                    nameOf={nameOf}
                    styles={styles}
                    textOf={fileTextOf}
                />
            )
        }
        return (
            <MessagesTable
                bundle={bundle}
                height={size.height}
                keys={bundle.keys}
                nameOf={nameOf}
                onOpenKey={openKey}
                styles={styles}
                textOf={fileTextOf}
            />
        )
    }

    return (
        <div className={shared.workspacePage} data-testid="localization-workspace">
            <div className={shared.workspaceBody}>
                <KeyRail keys={keys} onSelectKey={openKey} selectedKey={shownKey} styles={styles} />
                <div className={styles.body}>
                    <WorkspaceHeader
                        crumbs={crumbs}
                        testId="localization-header"
                        title={(
                            <CrumbSwitcher
                                current={title}
                                emptyText={t('repository:browser.module.module_no_match')}
                                items={moduleItems}
                                loading={modules === null}
                                onSelect={picked => void navigate(switchedRoute(project.id, picked))}
                                searchPlaceholder={t('repository:browser.module.module_filter')}
                                selectedKey={LOCALIZATION_ITEM}
                                testId="module-switcher"
                            />
                        )}
                    />
                    <div className={styles.canvas}>
                        {legacy.length > 0 && (
                            <Alert showIcon title={t('legacy_encoding', { files: legacy.join(', ') })} type="warning" />
                        )}
                        <div ref={canvasRef} className={styles.measured}>{canvas()}</div>
                    </div>
                    <CompileProblemsPanel
                        project={project}
                        statusReadAt={statusReadAt}
                        supportsBranches={supportsBranches({ features: project.repositoryInfo?.features })}
                    />
                </div>
            </div>
        </div>
    )
}
