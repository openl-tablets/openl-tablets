import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useBlocker, useNavigate, useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Alert, App, Modal } from 'antd'
import type { Project } from '../../types/projects'
import { getProject } from '../../services/repositories'
import { listModules, type ModuleInfo } from '../../services/modules'
import { LOCAL_LOAD_API_OPTIONS } from '../../services/apiCall'
import { applyMessageActions, BUNDLE_FOLDER } from '../../services/localization'
import { localizationRoute } from '../../services/projectId'
import { isUnresolvedProjectLink } from '../../services/projectLink'
import { errorMessage } from '../../utils/errorMessage'
import { ProjectStatus } from '../../constants/project'
import { isCompiled } from '../../services/projectStatus'
import { useLiveProjectChanges } from '../../hooks/useLiveProjectChanges'
import { ModuleActionBar } from '../modules/ModuleActionBar'
import { useConfirmBefore } from '../modules/useConfirmBefore'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { useModuleCompilation } from '../modules/useModuleCompilation'
import { useOverwriteConfirm } from '../modules/useOverwriteConfirm'
import { CompileProblemsPanel } from '../projects/CompileProblemsPanel'
import { ProjectScreenHeader } from '../projects/ProjectScreenHeader'
import { useSharedStyles } from '../projects/sharedStyles'
import { ProjectScreenPending } from '../projects/ProjectScreenPending'
import { AddKeyModal } from './AddKeyModal'
import { AddLanguageModal } from './AddLanguageModal'
import { addedKeyRow, fileOfTag, keyRowOf, languageName, languagesOf, type BundleLanguage, type KeyRow } from './bundle'
import { KeyRail } from './KeyRail'
import { MissingKeyPage } from './MissingKeyPage'
import { useStyles } from './LocalizationWorkspace.styles'
import { narrows, NO_FILTER, type MessageFilter } from './messageFilter'
import { MessagesCanvas } from './MessagesCanvas'
import { useOpenProject } from '../projects/useOpenProject'
import { MessagesToolbar } from './MessagesToolbar'
import { LOCALIZATION_ITEM } from '../projects/moduleSwitcher'
import { useElementSize } from '../../hooks/useElementSize'
import { useMessageEdits, type MessageEdits } from './useMessageEdits'
import { useMessageKey, useMessageKeys, useMessagePages } from './useMessagePages'

/** Whether a change of the project names the localization files, or does not say which files it touched. */
const touchesMessages = (files: string[]): boolean =>
    files.length === 0 || files.some(file => file === BUNDLE_FOLDER || file.startsWith(`${BUNDLE_FOLDER}/`))

/**
 * The messages of a project in all their languages: the `message*.properties` files of its `i18n` folder.
 *
 * <p>The messages belong to the project rather than to one of its modules, so they have a screen of their own,
 * reached from the module switcher. The rail lists the keys. Its root opens the table of every key in every language,
 * a key opens that key alone. The address keeps the key and the language the reader is on (`key`, `lang`), so a link
 * leads straight to them.
 *
 * <p>The server reads the files of an open project once and keeps them: the rail asks it for every key, the table for
 * a page of them at a time, and the page of a key for that key alone. The rail searches the keys it holds by name. The
 * columns of the table narrow its keys down — the keys by a piece of a key or of a text, or those with warnings, a
 * language by the keys it does not translate — and order them by name, the server answering the pages so; without an
 * order the keys come as the files write them. A closed project has no copy in the workspace to read: the screen asks
 * for it to be opened.
 *
 * <p>The default language comes first, then the others by name, a regional language next to its parent. A key a
 * language lacks shows, faint, the text the language falls back to and where that text comes from.
 *
 * <p>Opening the screen compiles nothing. The server checks the files without compiling the project, and each key
 * comes with what the check warned about it. The header follows the compilation of the project the way the screen of
 * a module does, and Refresh beside the title compiles the project again and reads the files afresh.
 *
 * <p>The screen wears the header of the screen of a module: the trail picks another project and another branch, the
 * title another module, and the actions of the project come at its end. The band above the messages offers what is
 * done to the files. The reader writes a text in its cell, adds a key or a language, or takes a key out of every
 * language. The row under the keys of the table adds keys one after another from the keyboard; a key added comes
 * after the keys of the project, as it is written at the end of the files. Each is a step of its own, kept on any
 * page until the reader saves: Undo takes the last step back and Redo does it again. Save sends the least of the
 * changes that leave the files as the steps leave them, and the server writes only what they change. Saving the
 * project with changes of the messages not saved asks to save them first. A change of the files made elsewhere is
 * read at once, and what the reader did stays on it.
 */

/**
 * The key on its own page: shown when a language of the project, or one the reader added, has it; missing when the
 * reader followed a key from a rule that no file of the project has. Neither while the key is read.
 */
const keyPageOf = (selectedKey: string | null, keyRow: KeyRow | null | undefined, textsOf: MessageEdits['textsOf']) => {
    if (keyRow === undefined) {
        return { shownKey: null, missingKey: null }
    }
    const texts = keyRow === null ? null : textsOf(keyRow.key, keyRow.base)
    const shown = keyRow !== null && texts !== null && Object.keys(texts).length > 0
    return { shownKey: shown ? keyRow : null, missingKey: shown ? null : selectedKey }
}

/** The key on screen, unless the reader added it and the server has none to read. */
const savedKeyOf = (key: string | null, added: ReadonlySet<string>): string | null =>
    (key !== null && !added.has(key) ? key : null)

/**
 * What the reader may do to the messages: write them in a project they may change, add keys once the default file is
 * there, as a key is added with its default text, and edit the texts while nothing is being saved, as a text written
 * meanwhile would be cleared with the saved ones.
 */
const rightsOf = (project: Project, closed: boolean, languages: readonly BundleLanguage[] | null, saving: boolean) => {
    const canWrite = project.capabilities?.canWrite === true && !closed
    return {
        canWrite,
        canAddKeys: canWrite && languages?.some(language => language.tag === '') === true,
        canEdit: canWrite && !saving,
    }
}

export const LocalizationWorkspace = () => {
    const { projectId = '' } = useParams()
    // Another project starts the screen afresh: nothing read, compiled or written for the previous one stays.
    return <LocalizationScreen key={projectId} />
}

/** The screen of the messages of the project the address names. */
const LocalizationScreen = () => {
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
    const [failure, setFailure] = useState<string | null>(null)
    const [modules, setModules] = useState<ModuleInfo[] | null>(null)
    const [saving, setSaving] = useState(false)
    const [adding, setAdding] = useState(false)
    const [addingLanguage, setAddingLanguage] = useState(false)
    const [filter, setFilter] = useState<MessageFilter>(NO_FILTER)
    // Bumped to read the messages afresh: after a save, a change made elsewhere, an action of the header.
    const [readToken, setReadToken] = useState(0)
    // The key and the language added on the page of all the keys, which the table brings into sight and lights.
    const [addedKey, setAddedKey] = useState<string | null>(null)
    const [addedLanguage, setAddedLanguage] = useState<string | null>(null)
    // The link is followed once: the filter is the reader's from then on, shown in its column, and the address leaves it,
    // so the plain address of the screen does not keep narrowing the keys down.
    useEffect(() => {
        if (untranslated !== null) {
            setFilter({ ...NO_FILTER, notTranslatedInto: [untranslated]})
            setSearch(Object.fromEntries([...search].filter(([name]) => name !== 'untranslated')), { replace: true })
        }
    }, [untranslated, search, setSearch])
    const { modal, notification } = App.useApp()
    const { ref: canvasRef, size } = useElementSize<HTMLDivElement>()
    const confirmWrite = useOverwriteConfirm(project)
    const edits = useMessageEdits()

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

    // A project nobody opened has no copy in the workspace: its messages are not read, nor is it compiled.
    const closed = project?.status === ProjectStatus.Closed
    const readable = project !== null && !closed
    const branch = project?.branch ?? null
    const read = project !== null
    useEffect(() => {
        if (read) {
            listModules(projectId).then(setModules).catch(() => setModules([]))
        }
    }, [projectId, read, branch])
    const reread = useCallback(() => setReadToken(token => token + 1), [])
    useLiveProjectChanges(readable ? projectId : undefined, changed => {
        if (touchesMessages(changed)) {
            reread()
        }
    }, { holdWhile: saving })

    // What the reader did meets what the server answers here: a key added comes last, as it is written at the end of
    // the files, and a key taken out is left out. The rail holds every key, which a new key is told apart from.
    const { addedKeys, textsOf } = edits
    const addedKeySet = useMemo(() => new Set(addedKeys), [addedKeys])
    const kept = useCallback((key: string) => textsOf(key, {}) !== null, [textsOf])
    const serverKeys = useMessageKeys(projectId, readToken, readable)
    const railKeys = useMemo(() => {
        // A closed project has no keys to list.
        if (closed) {
            return []
        }
        return serverKeys === null
            ? null
            : [...serverKeys.filter(key => !addedKeySet.has(key) && kept(key)), ...addedKeys]
    }, [closed, serverKeys, addedKeys, addedKeySet, kept])
    const onTable = selectedKey === null
    const pages = useMessagePages(projectId, filter, readToken, readable && onTable)
    // A key the reader added is not read from the server: it is not written yet.
    const keyPage = useMessageKey(projectId, savedKeyOf(selectedKey, addedKeySet), readToken, readable)
    const tableRows = useMemo<KeyRow[]>(() => [
        ...pages.rows.filter(row => !addedKeySet.has(row.key) && kept(row.key)).map(keyRowOf),
        ...addedKeys.map(addedKeyRow),
    ], [addedKeys, pages.rows, addedKeySet, kept])

    const defaultName = t('default_language')
    const nameOf = useCallback((tag: string) => languageName(tag, uiLocale, defaultName), [uiLocale, defaultName])
    const serverLanguages = (onTable ? pages.languages : keyPage.languages) ?? pages.languages ?? keyPage.languages
    const languages = useMemo(() => {
        if (serverLanguages === null) {
            return null
        }
        const added = edits.addedLanguages
            .filter(tag => !serverLanguages.some(language => language.tag === tag))
            .map(tag => ({ tag }))
        return languagesOf([...serverLanguages, ...added], nameOf)
    }, [serverLanguages, edits.addedLanguages, nameOf])

    // The key on screen: one the reader added, one the project writes, or one only a project it depends on gives.
    const keyRow = useMemo<KeyRow | null | undefined>(() => {
        if (selectedKey === null) {
            return null
        }
        if (addedKeySet.has(selectedKey)) {
            return addedKeyRow(selectedKey)
        }
        return keyPage.row === undefined || keyPage.row === null ? keyPage.row : keyRowOf(keyPage.row)
    }, [selectedKey, addedKeySet, keyPage.row])
    const { shownKey, missingKey } = keyPageOf(selectedKey, keyRow, textsOf)
    // The project a missing key is written in, when one this project depends on holds its default text.
    const keyOwner = keyRow?.shared['']

    const dirty = edits.dirty
    // Opening a revision, syncing or restoring the project reads the files afresh, over what is not saved.
    const confirmDiscard = useConfirmBefore(dirty, {
        title: t('discard_title'),
        content: t('discard_content'),
        okText: t('discard'),
    })
    const firstModule = modules?.[0]?.name
    // Bumped by Refresh, so the project is compiled again.
    const [reloadToken, setReloadToken] = useState(0)
    const compilation = useModuleCompilation({
        projectId: project?.id ?? '',
        branch,
        moduleName: firstModule ?? '',
        initial: project?.compileStatus ?? null,
        initialReadAt: statusReadAt,
        reloadToken,
        enabled: readable && firstModule !== undefined,
        // Opening the messages compiles nothing: the screen follows a compilation the reader asked for elsewhere,
        // and Refresh asks for one.
        compileOnOpen: false,
    })
    // Whether every module is compiled, which only then lets the tests of the whole project run. Held once it is
    // true: the status says "compiling" again while each next module is built.
    const [projectCompiled, setProjectCompiled] = useState(false)
    useEffect(() => {
        if (isCompiled(compilation.state)) {
            setProjectCompiled(true)
        }
    }, [compilation.state])

    const openKey = useCallback((key: string | null) => {
        setAddedKey(null)
        setAddedLanguage(null)
        setSearch(key === null ? {} : { key })
    }, [setSearch])

    // What is written and not saved lives on this screen alone: the reader is asked before leaving it, by the
    // switcher, a link, the Back button or by closing the page. Moving from key to key keeps it.
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

    // An action of the header that changed the project, such as a sync or an opened revision, is read back.
    const { clear } = edits
    const readAfresh = useCallback(() => {
        clear()
        reread()
        // The messages are read again whether or not the project is: a failed read of the project is told.
        void getProject(projectId, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS)
            .then(setProject)
            .catch((error: unknown) => notification.error({ title: t('load_failed'), description: errorMessage(error) }))
    }, [clear, projectId, reread, notification, t])

    // A closed project is opened from here, as from its module, and its messages are read once it is open.
    const failedToOpen = useCallback((error: unknown) => setFailure(errorMessage(error)), [])
    const { opening, open: openProject } = useOpenProject(project, readAfresh, failedToOpen)

    // Refresh compiles the project again and reads the files afresh, over what is not saved.
    const refresh = useCallback(() => {
        setProjectCompiled(false)
        setReloadToken(token => token + 1)
        readAfresh()
    }, [readAfresh])

    /** Sends the changes, and answers whether they were written. */
    const save = async (): Promise<boolean> => {
        setSaving(true)
        try {
            if (edits.actions.length > 0) {
                await applyMessageActions(projectId, edits.actions)
            }
            clear()
            reread()
            notification.success({ title: t('saved') })
            // A write takes the project up for editing, which the project read before does not know. The messages are
            // saved whether or not the project is read again.
            void getProject(projectId, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS).then(setProject).catch(() => undefined)
            return true
        } catch (error) {
            notification.error({ title: t('save_failed'), description: errorMessage(error) })
            return false
        } finally {
            setSaving(false)
        }
    }

    const { removeKey: takeKeyOut } = edits
    const removeKey = useCallback((row: KeyRow) => {
        modal.confirm({
            title: t('remove_key_title', { key: row.key }),
            content: t('remove_key_content'),
            okText: t('remove_key'),
            okButtonProps: { danger: true },
            onOk: () => {
                takeKeyOut(row.key, row.base)
                if (selectedKey === row.key) {
                    openKey(null)
                }
            },
        })
    }, [modal, t, takeKeyOut, selectedKey, openKey])

    // Added on the page of all the keys, the key stays among them, brought into sight; added on the page of a key,
    // it is opened on its own.
    const { addKey: takeKey } = edits
    const addKey = useCallback((key: string, texts: Readonly<Record<string, string>>) => {
        setAdding(false)
        takeKey(key, texts)
        if (onTable) {
            setAddedKey(key)
        } else {
            openKey(key)
        }
    }, [takeKey, onTable, openKey])
    // The same function every time, so the columns of the table, which offer to add a language, are not made anew.
    const askLanguage = useCallback(() => setAddingLanguage(true), [])

    const addLanguage = (tag: string) => {
        setAddingLanguage(false)
        edits.addLanguage(tag)
        setAddedLanguage(tag)
    }

    // A key may not be added twice: one the reader added, or one the project writes and the reader did not take out.
    // Nothing is asked of the server: the rail holds every key, whatever the table is narrowed down to.
    const keyExists = useCallback((key: string) => Promise.resolve(addedKeySet.has(key)
        || (serverKeys !== null && serverKeys.includes(key) && kept(key))), [addedKeySet, serverKeys, kept])

    // The project is saved from its files, so the messages written and not saved are saved first, when the reader
    // agrees: left behind, they would be forgotten.
    const beforeProjectSave = (saveProject: () => void) => {
        if (!dirty) {
            saveProject()
            return
        }
        modal.confirm({
            title: t('save_messages_first_title'),
            content: t('save_messages_first_content'),
            okText: t('save_messages_first'),
            cancelText: t('common:btn.cancel'),
            onOk: () => confirmWrite(() => void save().then(saved => {
                if (saved) {
                    saveProject()
                }
            })),
        })
    }

    // A file read in a legacy encoding is written in UTF-8 when it is saved, which the reader is told up front.
    const legacy = languages?.filter(language => language.legacy).map(language => fileOfTag(language.tag)) ?? []

    // What the canvas shows: on the table of every key, or on the page of one.
    const shownFailure = onTable ? pages.failure : keyPage.failure
    const shownLanguages = modules === null ? null : languages
    const keyOnPage = onTable ? undefined : shownKey
    const emptyText = narrows(filter) ? t('no_match') : t('no_keys')
    const removeShownKey = shownKey === null ? undefined : () => removeKey(shownKey)

    if (linkProblem || project === null) {
        return <ProjectScreenPending failure={failure} linkProblem={linkProblem} testId="localization-loading" />
    }

    const { canWrite, canAddKeys, canEdit } = rightsOf(project, closed, languages, saving)
    return (
        <div className={shared.workspacePage} data-testid="localization-workspace">
            <div className={shared.workspaceBody}>
                <KeyRail
                    keys={railKeys}
                    onSelectKey={openKey}
                    selectedKey={shownKey?.key ?? null}
                />
                <div className={shared.workspaceMain}>
                    <ProjectScreenHeader
                        branch={branch}
                        compilation={compilation}
                        compilationDisabled={closed || firstModule === undefined}
                        confirmDiscard={confirmDiscard}
                        modules={modules}
                        onBranchSwitched={readAfresh}
                        onRefresh={() => confirmDiscard(refresh)}
                        opening={opening}
                        project={project}
                        selectedKey={LOCALIZATION_ITEM}
                        testId="localization-header"
                        title={t('title')}
                        actions={(
                            <ModuleActionBar
                                beforeSave={beforeProjectSave}
                                confirmDiscard={confirmDiscard}
                                disabled={closed}
                                moduleName={firstModule ?? ''}
                                ofModule={false}
                                onProjectChanged={readAfresh}
                                onRevisionOpened={readAfresh}
                                project={project}
                                projectCompiled={projectCompiled}
                                testCount={compilation.tests}
                                verifyNeeded={compilation.verifyNeeded}
                            />
                        )}
                    />
                    {canWrite && languages !== null && languages.length > 0 && (
                        <MessagesToolbar
                            canAddLanguage
                            canAddKey={canAddKeys}
                            canRedo={edits.canRedo}
                            canUndo={edits.canUndo}
                            dirty={dirty}
                            onAddKey={() => setAdding(true)}
                            onAddLanguage={askLanguage}
                            onRedo={edits.redo}
                            onRemoveKey={removeShownKey}
                            onSave={() => confirmWrite(() => void save())}
                            onUndo={edits.undo}
                            saving={saving}
                        />
                    )}
                    <div className={styles.canvas}>
                        {legacy.length > 0 && (
                            <Alert showIcon title={t('legacy_encoding', { files: legacy.join(', ') })} type="warning" />
                        )}
                        <div ref={canvasRef} className={styles.measured}>
                            <MessagesCanvas
                                canWrite={canWrite}
                                closed={closed}
                                failure={shownFailure}
                                keyPanel={{ askedLanguage, canWrite: canEdit, edits, nameOf }}
                                languages={shownLanguages}
                                onCreateDefault={() => addLanguage('')}
                                onOpenProject={openProject}
                                opening={opening}
                                shownKey={keyOnPage}
                                missing={missingKey !== null && (
                                    <MissingKeyPage
                                        canAdd={canAddKeys}
                                        messageKey={missingKey}
                                        onAdd={() => setAdding(true)}
                                        onOpenOwner={owner => void navigate(localizationRoute(owner, missingKey))}
                                        owner={keyOwner}
                                    />
                                )}
                                table={{
                                    addedKey,
                                    addedLanguage,
                                    canWrite: canEdit,
                                    edits,
                                    emptyText,
                                    filter,
                                    height: size.height,
                                    width: size.width,
                                    keyExists,
                                    nameOf,
                                    onAddKey: addKey,
                                    onAddLanguage: askLanguage,
                                    onFilter: setFilter,
                                    onMore: pages.loadMore,
                                    onOpenKey: openKey,
                                    onRemoveKey: removeKey,
                                    rows: tableRows,
                                }}
                            />
                        </div>
                    </div>
                    <AddKeyModal
                        exists={keyExists}
                        initialKey={missingKey ?? undefined}
                        onAdd={(key, text) => addKey(key, { '': text })}
                        onClose={() => setAdding(false)}
                        open={adding}
                    />
                    <AddLanguageModal
                        onAdd={addLanguage}
                        onClose={() => setAddingLanguage(false)}
                        open={addingLanguage}
                        tags={languages?.map(language => language.tag) ?? []}
                    />
                    <Modal
                        cancelText={t('common:btn.cancel')}
                        okButtonProps={{ danger: true }}
                        okText={t('discard')}
                        onCancel={() => leaving.reset?.()}
                        open={leaving.state === 'blocked'}
                        title={t('leaving_title')}
                        onOk={() => {
                            clear()
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
