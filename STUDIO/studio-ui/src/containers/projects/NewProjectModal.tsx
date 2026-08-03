import { useCallback, useEffect, useMemo, useRef, useState, type ComponentType } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Checkbox, Input, Modal, Segmented, Select, Typography, Upload, type UploadFile } from 'antd'
import {
    ApiOutlined,
    ArrowLeftOutlined,
    CheckCircleFilled,
    CloudUploadOutlined,
    CopyOutlined,
    FileExcelOutlined,
    FolderOutlined,
    InboxOutlined,
    ProfileOutlined,
    FileZipOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    copyProject,
    createProject,
    createProjectsFromWorkspace,
    getDesignRepositoryBranches,
    getProjects,
    getProjectTemplates,
    type ProjectInclude,
    type ProjectTemplateGroup,
} from '../../services/repositories'
import type { Repository } from '../../types/repositories'
import type { Project } from '../../types/projects'
import { FieldError } from '../../components/FieldError'
import { FieldRow } from '../../components/FieldRow'
import { BranchSelect } from './BranchSelect'
import { branchMarksFromConfig } from './configBranchMarks'
import { RepoFolderInput } from './RepoFolderInput'
import { ProjectStatus } from '../../constants/project'
import { useSharedStyles } from './sharedStyles'
import { supportsBranches, supportsMappedFolders } from '../../utils/repositoryFeatures'
import { inspectOpenLArchive, zipProjectFolder, type OpenLArchiveInfo } from '../../utils/openlArchive'
import { useCommitInfoGuard, useRepositoryConfig } from '../../hooks'
import { suggestComment, validateBranchName } from '../../utils/repositoryConfig'
import { CommentField, useCommentError } from './CommentField'
import { trimTrailingSlashes } from './projectPaths'

/** Capitalise a template category name for display (e.g. "examples" → "Examples"). */
const titleCase = (value: string): string => (value ? value.charAt(0).toUpperCase() + value.slice(1) : value)

type CreateMode = 'template' | 'archive' | 'excel' | 'openapi' | 'workspace' | 'copy'

interface MethodMeta {
    id: CreateMode
    icon: ComponentType
    labelKey: string
    descKey: string
}

const METHODS: MethodMeta[] = [
    { id: 'template', icon: ProfileOutlined, labelKey: 'browser.create.mode_template', descKey: 'browser.create.mode_template_desc' },
    { id: 'archive', icon: FileZipOutlined, labelKey: 'browser.create.mode_archive', descKey: 'browser.create.mode_archive_desc' },
    { id: 'excel', icon: FileExcelOutlined, labelKey: 'browser.create.mode_excel', descKey: 'browser.create.mode_excel_desc' },
    { id: 'openapi', icon: ApiOutlined, labelKey: 'browser.create.mode_openapi', descKey: 'browser.create.mode_openapi_desc' },
    { id: 'workspace', icon: CloudUploadOutlined, labelKey: 'browser.create.mode_workspace', descKey: 'browser.create.mode_workspace_desc' },
    { id: 'copy', icon: CopyOutlined, labelKey: 'browser.create.mode_copy', descKey: 'browser.create.mode_copy_desc' },
]

const loadProjectSources = async (query: { includes?: ProjectInclude[], statuses?: ProjectStatus[] } = {}): Promise<Project[]> => {
    const projects: Project[] = []
    for (let page = 0; ; page++) {
        const response = await getProjects({
            ...query,
            page,
            size: PROJECT_SOURCE_PAGE_SIZE,
            sort: 'name',
        })
        projects.push(...response.content)
        const total = response.total
        if (response.content.length < PROJECT_SOURCE_PAGE_SIZE || total !== undefined && projects.length >= total) {
            return projects
        }
    }
}

const PROJECT_SOURCE_PAGE_SIZE = 1000

const useStyles = createStyles(({ css, token }) => ({
    grid: css`
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 8px;

        @media (max-width: 640px) {
            grid-template-columns: repeat(2, 1fr);
        }
    `,
    card: css`
        display: flex;
        flex-direction: column;
        gap: 4px;
        padding: 12px;
        background: ${token.colorBgContainer};
        text-align: left;

        .anticon {
            font-size: 18px;
            color: ${token.colorTextTertiary};
        }
    `,
    cardActive: css`
        .anticon {
            color: ${token.colorPrimary};
        }
    `,
    cardHead: css`
        display: flex;
        align-items: center;
        gap: 8px;
    `,
    cardLabel: css`
        font-size: 13px;
        font-weight: 600;
        line-height: 1.25;
    `,
    cardDesc: css`
        color: ${token.colorTextTertiary};
        font-size: 11px;
        line-height: 1.3;
    `,
    cardCount: css`
        margin-left: auto;
        color: ${token.colorTextTertiary};
        font-size: 11px;
    `,
    configHead: css`
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        color: ${token.colorPrimary};

        .anticon {
            font-size: 16px;
        }
    `,
    configTitle: css`
        color: ${token.colorText};
        font-size: 15px;
        font-weight: 600;
    `,
    configDesc: css`
        margin: -8px 0 12px;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    field: css`
        margin-bottom: 12px;
    `,
    templateGrid: css`
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 8px;
        margin-bottom: 12px;

        @media (max-width: 560px) {
            grid-template-columns: 1fr;
        }
    `,
    cardScroll: css`
        max-height: 400px;
        overflow-y: auto;
        margin-bottom: 12px;
        padding-right: 4px;
    `,
    customBadge: css`
        padding: 0 6px;
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorFillTertiary};
        color: ${token.colorTextTertiary};
        font-size: 10px;
        line-height: 18px;
    `,
    backLink: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 8px;
        padding: 0;
        border: none;
        background: none;
        color: ${token.colorTextSecondary};
        font-size: 13px;
        cursor: pointer;

        &:hover {
            color: ${token.colorPrimary};
        }
    `,
    selectAllRow: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;
    `,
    workspaceList: css`
        display: flex;
        flex-direction: column;
        gap: 6px;
    `,
    workspaceCard: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
    `,
    workspaceEmpty: css`
        font-size: 13px;
    `,
    footer: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
    `,
    footerRight: css`
        display: flex;
        gap: 8px;
    `,
}))

interface NewProjectModalProps {
    open: boolean
    repositories: Repository[]
    /** Names of the current user's local-only projects, offered for the "from workspace" mode. */
    localProjects?: string[]
    /** All visible projects, offered as sources for the "copy project" mode. */
    projects?: Project[]
    onClose: () => void
    /**
     * A project was created. Carries the created project's repository and name so the caller can open its
     * page; omitted when the workspace publish created several projects at once.
     */
    onCreated: (created?: { repositoryId: string, name: string }) => void
}

/**
 * Two-step "create project" wizard. Step one picks the creation method; a single click on a method opens
 * step two, which collects the method's inputs together with the target design repository and, where
 * relevant, the project name, path and commit comment. Every method routes through the existing create
 * services, and the backend enforces the CREATE grant, so a forbidden attempt surfaces as an inline error.
 */
export const NewProjectModal = ({
    open,
    repositories,
    localProjects: initialLocalProjects = [],
    projects: initialProjects = [],
    onClose,
    onCreated,
}: NewProjectModalProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { runWithCommitInfo, commitInfoModal, busy: committing } = useCommitInfoGuard()
    const openApiDefaults = useMemo(() => ({
        modelsModuleName: t('browser.create.openapi_defaults.data_module_name'),
        modelsPath: t('browser.create.openapi_defaults.data_module_path'),
        algorithmsModuleName: t('browser.create.openapi_defaults.rules_module_name'),
        algorithmsPath: t('browser.create.openapi_defaults.rules_module_path'),
    }), [t])
    const [step, setStep] = useState<'method' | 'config'>('method')
    const [mode, setMode] = useState<CreateMode>('template')
    const [repoId, setRepoId] = useState('')
    const [branch, setBranch] = useState('')
    const [branchOptions, setBranchOptions] = useState<string[]>([])
    const [branchTouched, setBranchTouched] = useState(false)
    const [copySource, setCopySource] = useState<string | null>(null)
    const [name, setName] = useState('')
    const [comment, setComment] = useState('')
    const [path, setPath] = useState('')
    const [archive, setArchive] = useState<File | null>(null)
    // The archive can be supplied as a ready .zip or as a folder the browser zips into one.
    const [archiveSource, setArchiveSource] = useState<'zip' | 'folder'>('zip')
    const [folderFiles, setFolderFiles] = useState<File[]>([])
    const [zipping, setZipping] = useState(false)
    const [excelFiles, setExcelFiles] = useState<File[]>([])
    const [templates, setTemplates] = useState<ProjectTemplateGroup[]>([])
    const [templateGroup, setTemplateGroup] = useState<string | null>(null)
    const [template, setTemplate] = useState<string | null>(null)
    const [openApiFile, setOpenApiFile] = useState<File | null>(null)
    const [openApi, setOpenApi] = useState(openApiDefaults)
    const [workspaceProjects, setWorkspaceProjects] = useState<string[]>([])
    const [copyProjects, setCopyProjects] = useState<Project[] | null>(null)
    const [localProjects, setLocalProjects] = useState<string[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [submitting, setSubmitting] = useState(false)
    // The name input auto-fills from the selection until the user edits it (which locks in their value).
    const [nameTouched, setNameTouched] = useState(false)
    const [commentTouched, setCommentTouched] = useState(false)
    // Suggested name and validity from inspecting the uploaded archive in the browser.
    const [archiveName, setArchiveName] = useState('')
    const [archiveError, setArchiveError] = useState<string | null>(null)
    const inspectSeq = useRef(0)
    const templatesLoaded = useRef(false)

    const creatableRepos = useMemo(
        () => repositories.filter(repo => repo.capabilities?.canCreateProject),
        [repositories]
    )
    const repoOptions = creatableRepos
    const repository = useMemo(() => creatableRepos.find(repo => repo.id === repoId) ?? null, [creatableRepos, repoId])
    const repositorySupportsFolders = supportsMappedFolders(repository)
    const repositorySupportsBranches = supportsBranches(repository)
    const projectSources = copyProjects ?? initialProjects
    const copyableProjectSources = useMemo(
        () => projectSources.filter(project => project.capabilities?.canCopy),
        [projectSources]
    )
    const workspaceSources = localProjects ?? initialLocalProjects

    useEffect(() => {
        if (open) {
            setRepoId(creatableRepos[0]?.id ?? '')
        }
    }, [creatableRepos, open])

    useEffect(() => {
        if (open && mode === 'template' && !templatesLoaded.current) {
            templatesLoaded.current = true
            getProjectTemplates().then(setTemplates).catch(() => setTemplates([]))
        }
    }, [open, mode])

    useEffect(() => {
        if (open && mode === 'copy' && copyProjects === null) {
            loadProjectSources({ includes: ['deleted']}).then(setCopyProjects).catch(() => setCopyProjects(initialProjects))
        }
    }, [copyProjects, initialProjects, mode, open])

    useEffect(() => {
        if (open && mode === 'workspace' && localProjects === null) {
            loadProjectSources({ statuses: [ProjectStatus.Local]})
                .then(projects => setLocalProjects(projects.map(project => project.name)))
                .catch(() => setLocalProjects(initialLocalProjects))
        }
    }, [initialLocalProjects, localProjects, mode, open])

    // Name suggested by the current selection: the template name, the source name with a "(Copy)" suffix,
    // or the name derived from the uploaded archive. Empty when nothing is selected yet.
    const suggestedName = useMemo(() => {
        if (mode === 'template' && template) {
            try {
                return (JSON.parse(template) as [string, string, string])[2]
            } catch {
                return ''
            }
        }
        if (mode === 'copy' && copySource) {
            const source = copyableProjectSources.find(candidate => candidate.id === copySource)
            return source ? `${source.name} (Copy)` : ''
        }
        if (mode === 'archive') {
            return archiveName
        }
        return ''
    }, [mode, template, copySource, copyableProjectSources, archiveName])

    useEffect(() => {
        if (suggestedName && !nameTouched) {
            setName(suggestedName)
        }
    }, [suggestedName, nameTouched])

    // The target repository suggests the comment of the commit the wizard is about to make, following the
    // project the comment is about: the copied one, or the one being created.
    const config = useRepositoryConfig(open && repoId ? { repositoryId: repoId } : null)
    const availableBranches = config?.branch && !branchOptions.includes(config.branch)
        ? [config.branch, ...branchOptions]
        : branchOptions
    const branchKnown = availableBranches.includes(branch.trim())
    const branchError = !repositorySupportsBranches || branchKnown
        ? null
        : validateBranchName(branch, config, t('browser.create.branch_required'),
            t('browser.create.branch_invalid'))
    const commentError = useCommentError(comment, config)
    const commentSubject = mode === 'copy'
        ? copyableProjectSources.find(candidate => candidate.id === copySource)?.name
        : name.trim()

    useEffect(() => {
        if (open && !commentTouched) {
            setComment(suggestComment(config, mode === 'copy' ? 'copy' : 'create', commentSubject))
        }
    }, [commentSubject, commentTouched, config, mode, open])

    useEffect(() => {
        let active = true
        setBranch('')
        setBranchOptions([])
        setBranchTouched(false)
        if (!open || !repositorySupportsBranches || !repoId) {
            return
        }
        getDesignRepositoryBranches(repoId)
            .then(options => {
                if (active) {
                    setBranchOptions(options)
                }
            })
            .catch(() => {
                if (active) {
                    setBranchOptions([])
                }
            })
        return () => {
            active = false
        }
    }, [open, repoId, repositorySupportsBranches])

    useEffect(() => {
        if (open && repositorySupportsBranches && !branchTouched && config?.branch) {
            setBranch(config.branch)
        }
    }, [branchTouched, config, open, repositorySupportsBranches])

    // Applies an inspection result: fill in the suggested name and flag non-OpenL content.
    const applyInspection = useCallback((info: OpenLArchiveInfo) => {
        setArchiveName(info.name)
        setArchiveError(info.readable && !info.isOpenLProject ? t('browser.create.archive_invalid') : null)
    }, [t])

    // Inspect a chosen archive in the browser: suggest its project name and flag non-OpenL content early.
    const inspectArchive = useCallback(async (file: File) => {
        const seq = ++inspectSeq.current
        setArchiveName('')
        setArchiveError(null)
        const info = await inspectOpenLArchive(file)
        if (seq === inspectSeq.current) {
            applyInspection(info)
        }
    }, [applyInspection])

    // A picked folder is zipped in the browser and then inspected as an archive, so it is validated exactly
    // like an uploaded .zip. Debounced so the directory picker's file-by-file delivery zips only once.
    useEffect(() => {
        if (archiveSource !== 'folder' || folderFiles.length === 0) {
            return
        }
        const seq = ++inspectSeq.current
        setZipping(true)
        setArchive(null)
        setArchiveName('')
        setArchiveError(null)
        const timer = window.setTimeout(async () => {
            try {
                const zipped = await zipProjectFolder(folderFiles)
                if (seq !== inspectSeq.current) {
                    return
                }
                setArchive(zipped)
                const info = await inspectOpenLArchive(zipped)
                if (seq !== inspectSeq.current) {
                    return
                }
                applyInspection(info)
            } catch {
                if (seq === inspectSeq.current) {
                    setArchiveError(t('browser.create.archive_invalid'))
                }
            } finally {
                if (seq === inspectSeq.current) {
                    setZipping(false)
                }
            }
        }, 200)
        return () => window.clearTimeout(timer)
    }, [applyInspection, archiveSource, folderFiles, t])

    const changeArchiveSource = (next: 'zip' | 'folder') => {
        setArchiveSource(next)
        setArchive(null)
        setArchiveName('')
        setArchiveError(null)
        setFolderFiles([])
        // A pending folder zip is abandoned; clear the flag or the Create button stays disabled forever.
        setZipping(false)
        inspectSeq.current++
    }

    // Clears the per-method inputs so switching create methods (or reopening) starts from a clean form.
    const resetFields = () => {
        setName('')
        setNameTouched(false)
        setArchiveName('')
        setArchiveError(null)
        setComment('')
        setCommentTouched(false)
        setPath('')
        setArchive(null)
        setArchiveSource('zip')
        setFolderFiles([])
        setZipping(false)
        setExcelFiles([])
        setTemplateGroup(null)
        setTemplate(null)
        setOpenApiFile(null)
        setOpenApi(openApiDefaults)
        setWorkspaceProjects([])
        setCopySource(null)
        setError(null)
        inspectSeq.current++
    }

    const chooseMethod = (next: CreateMode) => {
        resetFields()
        setMode(next)
        setStep('config')
    }

    const close = () => {
        setStep('method')
        setMode('template')
        resetFields()
        setCopyProjects(null)
        setLocalProjects(null)
        templatesLoaded.current = false
        onClose()
    }

    const groupKey = (group: ProjectTemplateGroup) => `${group.type}:${group.category}`
    const activeGroup = templateGroup ? templates.find(group => groupKey(group) === templateGroup) ?? null : null

    const toggleWorkspace = (projectName: string) => {
        setWorkspaceProjects(prev => prev.includes(projectName)
            ? prev.filter(item => item !== projectName)
            : [...prev, projectName])
    }

    const toggleAllWorkspace = () => {
        setWorkspaceProjects(prev => (prev.length === workspaceSources.length ? [] : [...workspaceSources]))
    }

    const contentFiles = (): File[] => {
        if (mode === 'excel') {
            return excelFiles
        }
        if (mode === 'openapi') {
            return [openApiFile!]
        }
        return [archive!]
    }

    /** The folder inside the repository, as every create mode but the archive one expects it. */
    const repositoryPath = () => (repositorySupportsFolders ? path.trim() || undefined : undefined)

    /**
     * The archive upload is mapped by its full internal path, so the project name is appended to the
     * folder here — the server takes the path as given, which is what lets an archive land in a folder
     * named differently from the project.
     */
    const archivePath = () => {
        const folder = trimTrailingSlashes(repositoryPath() ?? '')
        return folder ? `${folder}/${name.trim()}` : undefined
    }

    const submit = async () => {
        const trimmedName = name.trim()
        if (mode !== 'workspace' && !trimmedName) {
            setError(t('browser.create.name_required'))
            return
        }
        if (mode === 'workspace' && workspaceProjects.length === 0) {
            setError(t('browser.create.workspace_required'))
            return
        }
        if (mode === 'archive' && !archive) {
            setError(t('browser.create.file_required'))
            return
        }
        if (mode === 'archive' && archiveError) {
            setError(archiveError)
            return
        }
        if (mode === 'excel' && excelFiles.length === 0) {
            setError(t('browser.create.excel_required'))
            return
        }
        if (mode === 'template' && !template) {
            setError(t('browser.create.template_required'))
            return
        }
        if (mode === 'openapi' && !openApiFile) {
            setError(t('browser.create.openapi_required'))
            return
        }
        if (mode === 'openapi' && [openApi.modelsModuleName, openApi.modelsPath, openApi.algorithmsModuleName, openApi.algorithmsPath].some(field => !field.trim())) {
            setError(t('browser.create.openapi_modules_required'))
            return
        }
        if (mode === 'copy' && !copyableProjectSources.some(candidate => candidate.id === copySource)) {
            setError(t('browser.create.copy_source_required'))
            return
        }
        if (commentError) {
            setError(commentError)
            return
        }
        if (branchError) {
            setError(branchError)
            return
        }
        if (!repository) {
            return
        }
        await runWithCommitInfo(async () => {
            setSubmitting(true)
            setError(null)
            try {
                if (mode === 'copy') {
                    const source = copyableProjectSources.find(candidate => candidate.id === copySource)!
                    await copyProject(
                        source.repository,
                        source.name,
                        repository.id,
                        trimmedName,
                        comment.trim() || undefined,
                        repositoryPath(),
                        undefined,
                        repositorySupportsBranches ? branch.trim() : undefined
                    )
                } else if (mode === 'workspace') {
                    await createProjectsFromWorkspace(repository.id, {
                        names: workspaceProjects,
                        path: repositoryPath(),
                        comment: comment.trim() || undefined,
                        ...(repositorySupportsBranches ? { branch: branch.trim() } : {}),
                    })
                } else if (mode === 'template') {
                    const [type, category, name_] = JSON.parse(template!) as [string, string, string]
                    await createProject(repository.id, trimmedName, {
                        template: { type, category, name: name_ },
                        path: repositoryPath(),
                        comment: comment.trim() || undefined,
                        status: 'OPENED',
                        ...(repositorySupportsBranches ? { branch: branch.trim() } : {}),
                    })
                } else {
                    await createProject(repository.id, trimmedName, {
                        files: contentFiles(),
                        ...(mode === 'openapi' ? { openApi } : {}),
                        path: mode === 'archive' ? archivePath() : repositoryPath(),
                        comment: comment.trim() || undefined,
                        // Every source opens the new project, so an uploaded archive is no longer the odd
                        // one out that lands closed.
                        status: 'OPENED',
                        ...(repositorySupportsBranches ? { branch: branch.trim() } : {}),
                    })
                }
                // Publishing the workspace creates several projects at once, so there is no single one
                // to open; every other source creates exactly one and lands on its page.
                onCreated(mode === 'workspace' ? undefined : { repositoryId: repository.id, name: trimmedName })
                close()
            } catch (e) {
                setError(errorMessage(e))
            } finally {
                setSubmitting(false)
            }
        })
    }

    const fileList: UploadFile[] = archive ? [{ uid: '1', name: archive.name }] : []
    const activeMethod = METHODS.find(method => method.id === mode)!
    const showName = mode !== 'workspace'

    const repoSelectInput = (
        <Select
            data-testid="new-project-repo"
            onChange={value => setRepoId(value ?? '')}
            options={repoOptions.map(repo => ({ value: repo.id, label: repo.name }))}
            style={{ width: '100%' }}
            value={repoId || undefined}
        />
    )

    const nameField = (
        <FieldRow required label={t('browser.create.name')}>
            <Input
                data-testid="new-project-name"
                onChange={event => { setName(event.target.value); setNameTouched(true) }}
                placeholder={t('browser.create.name')}
                value={name}
            />
        </FieldRow>
    )


    const methodStep = (
        <>
            <p className={styles.configDesc}>{t('browser.create.wizard_desc')}</p>
            <div className={styles.grid}>
                {METHODS.map(method => {
                    const Icon = method.icon
                    return (
                        <button
                            key={method.id}
                            className={cx(shared.selectableCard, styles.card)}
                            data-testid={`new-project-method-${method.id}`}
                            onClick={() => chooseMethod(method.id)}
                            type="button"
                        >
                            <span className={styles.cardHead}>
                                <Icon />
                                <span className={styles.cardLabel}>{t(method.labelKey)}</span>
                            </span>
                            <span className={styles.cardDesc}>{t(method.descKey)}</span>
                        </button>
                    )
                })}
            </div>
        </>
    )

    const configStep = (
        <>
            <div className={styles.configHead}>
                <activeMethod.icon />
                <span className={styles.configTitle}>{t(activeMethod.labelKey)}</span>
            </div>
            <p className={styles.configDesc}>{t(activeMethod.descKey)}</p>
            {mode === 'template' && (activeGroup === null ? (
                <div className={styles.cardScroll}>
                    <div className={styles.templateGrid} data-testid="new-project-template-groups">
                        {templates.map(group => (
                            <button
                                key={groupKey(group)}
                                className={cx(shared.selectableCard, styles.card)}
                                data-testid={`template-group-${group.category}`}
                                onClick={() => { setTemplateGroup(groupKey(group)); setTemplate(null) }}
                                type="button"
                            >
                                <span className={styles.cardHead}>
                                    <FolderOutlined />
                                    <span className={styles.cardLabel}>{titleCase(group.category)}</span>
                                    {group.type === 'custom' && <span className={styles.customBadge}>{t('browser.create.template_custom')}</span>}
                                    <span className={styles.cardCount}>{group.templates.length}</span>
                                </span>
                            </button>
                        ))}
                    </div>
                </div>
            ) : (
                <>
                    <button
                        className={styles.backLink}
                        data-testid="new-project-template-back"
                        onClick={() => { setTemplateGroup(null); setTemplate(null) }}
                        type="button"
                    >
                        <ArrowLeftOutlined /> {titleCase(activeGroup.category)}
                    </button>
                    <div className={styles.cardScroll}>
                        <div className={styles.templateGrid} data-testid="new-project-template">
                            {activeGroup.templates.map(name_ => {
                                const key = JSON.stringify([activeGroup.type, activeGroup.category, name_])
                                return (
                                    <button
                                        key={key}
                                        className={cx(shared.selectableCard, styles.card, template === key && cx(shared.selectedCard, styles.cardActive))}
                                        data-testid={`template-${key}`}
                                        onClick={() => setTemplate(key)}
                                        type="button"
                                    >
                                        <span className={styles.cardHead}>
                                            {template === key ? <CheckCircleFilled /> : <ProfileOutlined />}
                                            <span className={styles.cardLabel}>{name_}</span>
                                        </span>
                                    </button>
                                )
                            })}
                        </div>
                    </div>
                </>
            ))}
            {mode === 'workspace' && (
                <div className={styles.field}>
                    <div className={styles.selectAllRow}>
                        <Checkbox
                            checked={workspaceSources.length > 0 && workspaceProjects.length === workspaceSources.length}
                            data-testid="new-project-workspace-all"
                            disabled={workspaceSources.length === 0}
                            indeterminate={workspaceProjects.length > 0 && workspaceProjects.length < workspaceSources.length}
                            onChange={toggleAllWorkspace}
                        >
                            {t('browser.create.workspace_select_all')}
                        </Checkbox>
                        <span className={styles.cardCount}>{workspaceProjects.length}/{workspaceSources.length}</span>
                    </div>
                    {workspaceSources.length === 0 ? (
                        <div className={cx(shared.dashedEmpty, styles.workspaceEmpty)}>{t('browser.create.workspace_empty')}</div>
                    ) : (
                        <div className={styles.cardScroll} data-testid="new-project-workspace">
                            <div className={styles.workspaceList}>
                                {workspaceSources.map(projectName => (
                                    <div
                                        key={projectName}
                                        className={cx(shared.selectableCard, styles.workspaceCard, workspaceProjects.includes(projectName) && styles.cardActive)}
                                        data-testid={`workspace-${projectName}`}
                                        onClick={() => toggleWorkspace(projectName)}
                                        role="button"
                                        tabIndex={0}
                                        onKeyDown={event => {
                                            if (event.key === 'Enter' || event.key === ' ') {
                                                event.preventDefault()
                                                toggleWorkspace(projectName)
                                            }
                                        }}
                                    >
                                        <Checkbox checked={workspaceProjects.includes(projectName)} />
                                        <span className={styles.cardLabel}>{projectName}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}
            {mode === 'copy' && (
                <FieldRow required label={t('browser.create.copy_source_label')}>
                    <Select
                        showSearch
                        data-testid="new-project-copy-source"
                        filterOption={(input, option) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
                        onChange={value => setCopySource(value ?? null)}
                        options={copyableProjectSources.map(candidate => ({ label: candidate.name, value: candidate.id }))}
                        placeholder={t('browser.create.copy_source')}
                        style={{ width: '100%' }}
                        value={copySource ?? undefined}
                    />
                </FieldRow>
            )}
            {mode === 'archive' && (
                <div className={styles.field}>
                    <Segmented<'zip' | 'folder'>
                        block
                        data-testid="new-project-archive-source"
                        onChange={changeArchiveSource}
                        style={{ marginBottom: 8 }}
                        value={archiveSource}
                        options={[
                            { label: t('browser.create.source_zip'), value: 'zip' },
                            { label: t('browser.create.source_folder'), value: 'folder' },
                        ]}
                    />
                    {archiveSource === 'zip' ? (
                        <Upload.Dragger
                            accept=".zip"
                            beforeUpload={file => { setArchive(file); setError(null); void inspectArchive(file); return false }}
                            data-testid="new-project-upload"
                            fileList={fileList}
                            maxCount={1}
                            onRemove={() => { setArchive(null); setArchiveName(''); setArchiveError(null); inspectSeq.current++ }}
                        >
                            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                            <p className="ant-upload-text">{t('browser.create.archive_hint')}</p>
                            <p className="ant-upload-hint">{t('browser.create.archive_subhint')}</p>
                        </Upload.Dragger>
                    ) : (
                        <Upload.Dragger
                            multiple
                            data-testid="new-project-folder-upload"
                            directory={true}
                            showUploadList={false}
                            // Take the current selection from beforeUpload's batch and ignore the file for
                            // Upload's own list: otherwise a second folder is appended to the first, and the
                            // combined tree has two roots and fails validation. batch is the same array for
                            // every file of one pick, so this simply replaces the previous folder.
                            beforeUpload={(_file, batch) => {
                                setError(null)
                                setFolderFiles(batch as File[])
                                return Upload.LIST_IGNORE
                            }}
                        >
                            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                            <p className="ant-upload-text">{t('browser.create.folder_hint')}</p>
                            <p className="ant-upload-hint">{t('browser.create.folder_subhint')}</p>
                        </Upload.Dragger>
                    )}
                    {archiveSource === 'folder' && folderFiles.length > 0 && (
                        <Typography.Text data-testid="new-project-folder-summary" type="secondary">
                            {zipping
                                ? t('browser.create.folder_zipping')
                                : t('browser.create.folder_selected', { count: folderFiles.length })}
                        </Typography.Text>
                    )}
                    {archiveError && (
                        <Alert
                            showIcon
                            data-testid="new-project-archive-error"
                            style={{ marginTop: 8 }}
                            title={archiveError}
                            type="error"
                        />
                    )}
                </div>
            )}
            {mode === 'excel' && (
                <div className={styles.field}>
                    <Upload.Dragger
                        multiple
                        accept=".xlsx,.xls"
                        beforeUpload={file => { setExcelFiles(prev => [...prev, file]); setError(null); return false }}
                        data-testid="new-project-excel-upload"
                        fileList={excelFiles.map((file, index) => ({ uid: String(index), name: file.name }))}
                        onRemove={file => setExcelFiles(prev => prev.filter((_, index) => String(index) !== file.uid))}
                    >
                        <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                        <p className="ant-upload-text">{t('browser.create.excel_hint')}</p>
                    </Upload.Dragger>
                </div>
            )}
            {mode === 'openapi' && (
                <>
                    <div className={styles.field}>
                        <Upload.Dragger
                            accept=".json,.yaml,.yml"
                            beforeUpload={file => { setOpenApiFile(file); setError(null); return false }}
                            data-testid="new-project-openapi-upload"
                            fileList={openApiFile ? [{ uid: '1', name: openApiFile.name }] : []}
                            maxCount={1}
                            onRemove={() => setOpenApiFile(null)}
                        >
                            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                            <p className="ant-upload-text">{t('browser.create.openapi_hint')}</p>
                        </Upload.Dragger>
                    </div>
                    {nameField}
                    <FieldRow alignTop required label={t('browser.create.openapi_data_module')}>
                        <Input data-testid="new-project-openapi-data-module" onChange={e => setOpenApi(prev => ({ ...prev, modelsModuleName: e.target.value }))} value={openApi.modelsModuleName} />
                    </FieldRow>
                    <FieldRow alignTop required label={t('browser.create.openapi_data_path')}>
                        <Input data-testid="new-project-openapi-data-path" onChange={e => setOpenApi(prev => ({ ...prev, modelsPath: e.target.value }))} value={openApi.modelsPath} />
                    </FieldRow>
                    <FieldRow alignTop required label={t('browser.create.openapi_rules_module')}>
                        <Input data-testid="new-project-openapi-rules-module" onChange={e => setOpenApi(prev => ({ ...prev, algorithmsModuleName: e.target.value }))} value={openApi.algorithmsModuleName} />
                    </FieldRow>
                    <FieldRow alignTop required label={t('browser.create.openapi_rules_path')}>
                        <Input data-testid="new-project-openapi-rules-path" onChange={e => setOpenApi(prev => ({ ...prev, algorithmsPath: e.target.value }))} value={openApi.algorithmsPath} />
                    </FieldRow>
                </>
            )}
            {showName && mode !== 'openapi' && nameField}
            <FieldRow required label={t('browser.create.repository_label')}>
                {repoSelectInput}
            </FieldRow>
            {repositorySupportsBranches && (
                <FieldRow required label={t('browser.create.branch')}>
                    <BranchSelect
                        allowNew
                        branchNames={availableBranches}
                        data-testid="new-project-branch"
                        marksOf={branchMarksFromConfig(config)}
                        placeholder={t('browser.create.branch')}
                        value={branch}
                        onChange={value => {
                            setBranchTouched(true)
                            setBranch(value)
                        }}
                    />
                    <FieldError
                        message={branchTouched ? branchError : null}
                        testId="new-project-branch-error"
                    />
                </FieldRow>
            )}
            {repositorySupportsFolders && repository && (
                <FieldRow label={t('browser.create.path')}>
                    <RepoFolderInput
                        data-testid="new-project-path"
                        onChange={setPath}
                        repositoryId={repository.id}
                        value={path}
                    />
                </FieldRow>
            )}
            <CommentField
                config={config}
                testId="new-project-comment"
                value={comment}
                onChange={value => {
                    setCommentTouched(true)
                    setComment(value)
                }}
            />
            {error && <Alert showIcon data-testid="new-project-error" style={{ marginTop: 4 }} title={error} type="error" />}
        </>
    )

    // A picked folder is not ready to create until it has been zipped into a validated archive.
    const folderPending = zipping
        || (mode === 'archive' && archiveSource === 'folder' && folderFiles.length > 0 && !archive)

    const footer = step === 'method' ? (
        <div className={styles.footer}>
            <span />
            <div className={styles.footerRight}>
                <Button data-testid="new-project-cancel" onClick={close}>{t('browser.create.cancel')}</Button>
            </div>
        </div>
    ) : (
        <div className={styles.footer}>
            <Button data-testid="new-project-back" icon={<ArrowLeftOutlined />} onClick={() => setStep('method')} type="text">
                {t('browser.create.back')}
            </Button>
            <div className={styles.footerRight}>
                <Button data-testid="new-project-cancel" disabled={submitting || committing} onClick={close}>{t('browser.create.cancel')}</Button>
                <Button data-testid="new-project-submit" disabled={folderPending} loading={submitting || committing} onClick={submit} type="primary">
                    {t('browser.create.submit')}
                </Button>
            </div>
        </div>
    )

    return (
        <>
            <Modal
                destroyOnHidden
                footer={footer}
                onCancel={close}
                open={open}
                title={t('browser.create.wizard_title')}
                width={640}
            >
                {step === 'method' ? methodStep : configStep}
            </Modal>
            {commitInfoModal}
        </>
    )
}
