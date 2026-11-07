import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useBlocker, useNavigate, useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Alert, App, Button, Empty, Modal, Skeleton, Space } from 'antd'
import { GlobalOutlined, PlusOutlined, SaveOutlined, UndoOutlined } from '@ant-design/icons'
import type { Project } from '../../types/projects'
import type { MessageFile } from '../../types/localization'
import { getProject } from '../../services/repositories'
import { listModules, type ModuleInfo } from '../../services/modules'
import { LOCAL_LOAD_API_OPTIONS } from '../../services/apiCall'
import {
    BUNDLE_FOLDER,
    readDependencyMessages,
    readMessageFiles,
    saveMessageFiles,
    type DependencyMessages,
} from '../../services/localization'
import { localizationRoute } from '../../services/projectId'
import { isUnresolvedProjectLink } from '../../services/projectLink'
import { errorMessage } from '../../utils/errorMessage'
import { ProjectStatus } from '../../constants/project'
import { useLiveProjectChanges } from '../../hooks/useLiveProjectChanges'
import { useOverwriteConfirm } from '../modules/useOverwriteConfirm'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { WorkspaceHeader } from '../../components/WorkspaceHeader'
import { CompileProblemsPanel } from '../projects/CompileProblemsPanel'
import { CrumbSwitcher } from '../projects/CrumbSwitcher'
import { useSharedStyles } from '../projects/sharedStyles'
import { UnresolvedProjectLink } from '../projects/UnresolvedProjectLink'
import { ValueText } from '../projects/ValueText'
import { AddKeyModal } from './AddKeyModal'
import { AddLanguageModal } from './AddLanguageModal'
import { buildBundle, fileOfTag, languageName, sharedTextOf } from './bundle'
import { KeyPanel } from './KeyPanel'
import { KeyRail } from './KeyRail'
import { useStyles } from './LocalizationWorkspace.styles'
import { filterKeys, NO_FILTER, type MessageFilter } from './messageFilter'
import { indexProblems } from './messageProblems'
import { MessagesTable } from './MessagesTable'
import { LOCALIZATION_ITEM, switchedRoute, switcherItems } from './moduleSwitcher'
import { useElementSize } from './useElementSize'
import { useMessageDrafts } from './useMessageDrafts'

/** Whether a change of the project names the localization files, or does not say which files it touched. */
const touchesMessages = (files: string[]): boolean =>
    files.length === 0 || files.some(file => file === BUNDLE_FOLDER || file.startsWith(`${BUNDLE_FOLDER}/`))

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
 *
 * <p>The reader writes a text in its cell, in the table or on the key, adds a key, or takes one out of every
 * language. What is written is kept while the reader moves from key to key, and Save writes every changed language
 * at once, so the project is compiled once. Only the lines of the changed keys are written: comments, blank lines
 * and the order of the keys stay as they were. A change of the files made elsewhere is read at once, and what the
 * reader has written stays on it.
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
    // A link to the keys a language has not translated opens the messages filtered to them.
    const untranslated = search.get('untranslated')
    const uiLocale = i18n.resolvedLanguage ?? i18n.language

    const [project, setProject] = useState<Project | null>(null)
    const [statusReadAt, setStatusReadAt] = useState(0)
    const [linkProblem, setLinkProblem] = useState(false)
    const [modules, setModules] = useState<ModuleInfo[] | null>(null)
    const [files, setFiles] = useState<MessageFile[] | null>(null)
    // The messages of the projects this one depends on, which give the texts it lacks. Until they are read, and
    // when they cannot be, the project stands alone.
    const [dependencies, setDependencies] = useState<DependencyMessages[]>([])
    const [failure, setFailure] = useState<string | null>(null)
    const [saving, setSaving] = useState(false)
    const [adding, setAdding] = useState(false)
    const [addingLanguage, setAddingLanguage] = useState(false)
    // The files of the languages added and not saved yet. A new language starts as an empty file, so every key
    // of it shows the inherited text until it is written.
    const [created, setCreated] = useState<string[]>([])
    const [filter, setFilter] = useState<MessageFilter>(NO_FILTER)
    useEffect(() => {
        if (untranslated !== null) {
            setFilter({ ...NO_FILTER, notTranslatedInto: untranslated })
        }
    }, [untranslated])
    const { modal, notification } = App.useApp()
    const { ref: canvasRef, size } = useElementSize<HTMLDivElement>()
    const confirmWrite = useOverwriteConfirm(project)

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

    const readFiles = useCallback(() => {
        let current = true
        setFailure(null)
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
    }, [projectId])

    // The files are the branch's: they are read once the project says which branch it stands on.
    const read = project !== null
    const branch = project?.branch ?? null
    useEffect(() => {
        if (!read) {
            return undefined
        }
        listModules(projectId).then(setModules).catch(() => setModules([]))
        return readFiles()
    }, [projectId, read, branch, readFiles])
    useLiveProjectChanges(read ? projectId : undefined, changed => {
        if (touchesMessages(changed)) {
            readFiles()
        }
    }, { holdWhile: saving })

    const defaultName = t('default_language')
    const nameOf = useCallback((tag: string) => languageName(tag, uiLocale, defaultName), [uiLocale, defaultName])
    const pending = useMemo(
        () => created.filter(name => !files?.some(file => file.name === name)),
        [created, files]
    )
    useEffect(() => {
        if (project === null) {
            return undefined
        }
        let current = true
        void readDependencyMessages(project).then(read => {
            if (current) {
                setDependencies(read)
            }
        })
        return () => {
            current = false
        }
    }, [project])
    const bundle = useMemo(() => (files === null
        ? null
        : buildBundle([...files, ...pending.map(name => ({ name, text: '', legacy: false }))], nameOf, dependencies)),
    [files, pending, nameOf, dependencies])
    const drafts = useMessageDrafts(bundle)
    const { remove, write, discard: discardDrafts } = drafts
    const dirty = drafts.dirty || pending.length > 0
    const discard = useCallback(() => {
        discardDrafts()
        setCreated([])
    }, [discardDrafts])
    const keys = bundle === null ? null : drafts.keys
    const problems = useMemo(
        () => indexProblems(bundle ?? { languages: [], keys: [], shared: []}, drafts.keys, drafts.textOf),
        [bundle, drafts.keys, drafts.textOf]
    )
    // The keys the search and the filter leave, in the rail and in the table alike.
    const shownKeys = useMemo(
        () => (bundle === null ? null : filterKeys(bundle, drafts.keys, drafts.textOf, problems, filter)),
        [bundle, drafts.keys, drafts.textOf, problems, filter]
    )
    const shownKey = selectedKey !== null && keys?.includes(selectedKey) === true ? selectedKey : null
    // A key a rule asks for, which the reader followed from the rule, while no file of the project has it.
    const missingKey = selectedKey !== null && keys !== null && !keys.includes(selectedKey) ? selectedKey : null
    // The project a missing key is written in, when one this project depends on holds its default text.
    const keyOwner = missingKey === null || bundle === null ? undefined : sharedTextOf(bundle, '', missingKey)?.project

    const openKey = useCallback((key: string | null) => {
        setSearch(key === null ? {} : { key })
    }, [setSearch])

    // What is written and not saved lives on this screen alone: the reader is asked before leaving it, by the
    // switcher, a link, the Back button or by closing the page. Moving from key to key leaves nothing.
    const leaving = useBlocker(({ currentLocation, nextLocation }) => dirty
        && currentLocation.pathname !== nextLocation.pathname)
    useEffect(() => {
        if (!dirty) {
            return undefined
        }
        const ask = (event: BeforeUnloadEvent) => event.preventDefault()
        window.addEventListener('beforeunload', ask)
        return () => window.removeEventListener('beforeunload', ask)
    }, [dirty])

    const save = async () => {
        setSaving(true)
        try {
            const written = drafts.edited()
            const empty = pending
                .filter(name => !written.some(file => file.name === name))
                .map(name => ({ name, text: '' }))
            await saveMessageFiles(projectId, [...written, ...empty])
            setFiles(await readMessageFiles(projectId))
            discard()
            notification.success({ title: t('saved') })
            // A write takes the project up for editing, which the project read before does not know.
            setProject(await getProject(projectId, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS))
        } catch (error) {
            notification.error({ title: t('save_failed'), description: errorMessage(error) })
        } finally {
            setSaving(false)
        }
    }

    const removeKey = useCallback((key: string) => {
        modal.confirm({
            title: t('remove_key_title', { key }),
            content: t('remove_key_content'),
            okText: t('remove_key'),
            okButtonProps: { danger: true },
            onOk: () => remove(key),
        })
    }, [modal, remove, t])

    const addKey = (key: string, text: string) => {
        const defaultLanguage = bundle?.languages.find(language => language.tag === '')
        if (defaultLanguage !== undefined) {
            write(defaultLanguage, key, text)
            openKey(key)
        }
        setAdding(false)
    }

    const addLanguage = useCallback((tag: string) => {
        setCreated(current => [...current, fileOfTag(tag)])
        setAddingLanguage(false)
    }, [])

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

    // A project nobody opened has no copy in the workspace to write to: its messages are read only.
    const canWrite = project.capabilities?.canWrite === true && project.status !== ProjectStatus.Closed
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
            return (
                <Empty className={styles.centered} description={t('no_files')}>
                    {canWrite && (
                        <Button
                            data-testid="localization-create"
                            icon={<PlusOutlined />}
                            onClick={() => addLanguage('')}
                            type="primary"
                        >
                            {t('create_default')}
                        </Button>
                    )}
                </Empty>
            )
        }
        if (missingKey !== null) {
            return (
                <Empty
                    className={styles.centered}
                    data-testid="localization-key-missing"
                    description={keyOwner === undefined
                        ? t('key_missing', { key: missingKey })
                        : t('key_in_dependency', { key: missingKey, project: keyOwner.name })}
                >
                    <Space>
                        {keyOwner !== undefined && (
                            <Button
                                data-testid="localization-key-owner"
                                onClick={() => void navigate(localizationRoute(keyOwner.id, missingKey))}
                                type="primary"
                            >
                                {t('open_in_project', { project: keyOwner.name })}
                            </Button>
                        )}
                        {canWrite && (
                            <Button
                                icon={<PlusOutlined />}
                                onClick={() => setAdding(true)}
                                type={keyOwner === undefined ? 'primary' : 'default'}
                            >
                                {keyOwner === undefined ? t('add_key') : t('override_key')}
                            </Button>
                        )}
                    </Space>
                </Empty>
            )
        }
        if (shownKey !== null) {
            return (
                <KeyPanel
                    askedLanguage={askedLanguage}
                    bundle={bundle}
                    canWrite={canWrite}
                    drafts={drafts}
                    messageKey={shownKey}
                    nameOf={nameOf}
                    onRemoveKey={removeKey}
                    styles={styles}
                />
            )
        }
        return (
            <MessagesTable
                bundle={bundle}
                canWrite={canWrite}
                drafts={drafts}
                height={size.height}
                keys={shownKeys ?? []}
                nameOf={nameOf}
                onAddLanguage={() => setAddingLanguage(true)}
                onOpenKey={openKey}
                onRemoveKey={removeKey}
                problems={problems}
                styles={styles}
            />
        )
    }

    return (
        <div className={shared.workspacePage} data-testid="localization-workspace">
            <div className={shared.workspaceBody}>
                <KeyRail
                    filter={filter}
                    keys={shownKeys}
                    onFilter={setFilter}
                    onSelectKey={openKey}
                    problems={problems}
                    selectedKey={shownKey}
                    styles={styles}
                    languages={bundle?.languages
                        .filter(language => language.tag !== '')
                        .map(language => ({ value: language.tag, label: nameOf(language.tag) })) ?? []}
                />
                <div className={styles.body}>
                    <WorkspaceHeader
                        crumbs={crumbs}
                        testId="localization-header"
                        actions={canWrite && (
                            <Space>
                                <Button
                                    data-testid="localization-add-key"
                                    disabled={bundle?.languages.every(language => language.tag !== '') !== false}
                                    icon={<PlusOutlined />}
                                    onClick={() => setAdding(true)}
                                >
                                    {t('add_key')}
                                </Button>
                                <Button
                                    data-testid="localization-add-language-header"
                                    disabled={bundle === null || bundle.languages.length === 0}
                                    icon={<GlobalOutlined />}
                                    onClick={() => setAddingLanguage(true)}
                                >
                                    {t('add_language')}
                                </Button>
                                <Button
                                    data-testid="localization-discard"
                                    disabled={!dirty || saving}
                                    icon={<UndoOutlined />}
                                    onClick={() => modal.confirm({
                                        title: t('discard_title'),
                                        content: t('discard_content'),
                                        okText: t('discard'),
                                        onOk: discard,
                                    })}
                                >
                                    {t('discard')}
                                </Button>
                                <Button
                                    data-testid="localization-save"
                                    disabled={!dirty}
                                    icon={<SaveOutlined />}
                                    loading={saving}
                                    onClick={() => confirmWrite(() => void save())}
                                    type="primary"
                                >
                                    {t('save')}
                                </Button>
                            </Space>
                        )}
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
                    <AddKeyModal
                        initialKey={missingKey ?? undefined}
                        keys={drafts.keys}
                        onAdd={addKey}
                        onClose={() => setAdding(false)}
                        open={adding}
                    />
                    <AddLanguageModal
                        onAdd={addLanguage}
                        onClose={() => setAddingLanguage(false)}
                        open={addingLanguage}
                        tags={bundle?.languages.map(language => language.tag) ?? []}
                    />
                    <Modal
                        cancelText={t('common:btn.cancel')}
                        okButtonProps={{ danger: true }}
                        okText={t('discard')}
                        onCancel={() => leaving.reset?.()}
                        open={leaving.state === 'blocked'}
                        title={t('leaving_title')}
                        onOk={() => {
                            discard()
                            leaving.proceed?.()
                        }}
                    >
                        {t('leaving_content')}
                    </Modal>
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
