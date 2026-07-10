import { useEffect, useMemo, useRef, useState, type ComponentType } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Checkbox, Input, Modal, Select, Upload, type UploadFile } from 'antd'
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
    getProjects,
    getProjectTemplates,
    type ProjectInclude,
    type ProjectTemplateGroup,
} from '../../services/repositories'
import type { Repository } from '../../types/repositories'
import type { Project } from '../../types/projects'
import { ProjectStatus } from '../../constants/project'
import { MOCKUP } from './projectsTheme'
import { supportsMappedFolders } from '../../utils/repositoryFeatures'
import { useCommitInfoGuard } from '../../hooks'

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
        grid-template-columns: repeat(4, 1fr);
        gap: 8px;

        @media (max-width: 640px) {
            grid-template-columns: repeat(2, 1fr);
        }
    `,
    tile: css`
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        gap: 6px;
        padding: 12px;
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadius}px;
        background: ${token.colorBgContainer};
        text-align: left;
        cursor: pointer;
        transition: border-color 0.15s ease, background 0.15s ease;

        &:hover {
            border-color: ${token.colorPrimaryBorder};
            background: ${token.colorFillQuaternary};
        }

        .anticon {
            font-size: 18px;
            color: ${token.colorTextTertiary};
        }
    `,
    tileActive: css`
        border-color: ${token.colorPrimary};
        background: ${token.colorPrimaryBg};

        .anticon {
            color: ${token.colorPrimary};
        }
    `,
    tileLabel: css`
        font-size: 13px;
        font-weight: 600;
        line-height: 1.25;
    `,
    tileDesc: css`
        color: ${token.colorTextTertiary};
        font-size: 11px;
        line-height: 1.3;
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
    nameRepoRow: css`
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
        margin-bottom: 12px;

        @media (max-width: 480px) {
            grid-template-columns: 1fr;
        }
    `,
    label: css`
        display: block;
        margin-bottom: 4px;
        font-size: 13px;
        font-weight: 500;
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
    templateCard: css`
        display: flex;
        align-items: flex-start;
        gap: 8px;
        padding: 10px 12px;
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadius}px;
        text-align: left;
        cursor: pointer;

        &:hover {
            border-color: ${token.colorPrimaryBorder};
        }

        .anticon {
            color: ${token.colorPrimary};
        }
    `,
    templateCardActive: css`
        border-color: ${token.colorPrimary};
        background: ${token.colorPrimaryBg};
    `,
    templateName: css`
        font-size: 13px;
        font-weight: 600;
    `,
    cardScroll: css`
        height: 260px;
        overflow-y: auto;
        margin-bottom: 12px;
        padding-right: 4px;
    `,
    groupCard: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px;
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadius}px;
        text-align: left;
        cursor: pointer;

        &:hover {
            border-color: ${token.colorPrimaryBorder};
            background: ${token.colorFillQuaternary};
        }

        .anticon {
            color: ${token.colorTextTertiary};
        }
    `,
    groupName: css`
        font-size: 13px;
        font-weight: 600;
    `,
    groupCount: css`
        margin-left: auto;
        color: ${token.colorTextTertiary};
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
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
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadius}px;
        cursor: pointer;

        &:hover {
            border-color: ${token.colorPrimaryBorder};
        }
    `,
    workspaceEmpty: css`
        padding: 24px;
        border: 1px dashed ${token.colorBorder};
        border-radius: ${token.borderRadius}px;
        text-align: center;
        color: ${token.colorTextTertiary};
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
    onCreated: () => void
}

/**
 * Two-step "create project" wizard. Step one picks the creation method; step two collects the method's
 * inputs together with the target design repository and, where relevant, the project name, path and
 * commit comment. Every method routes through the existing create services, and the backend enforces the
 * CREATE grant, so a forbidden attempt surfaces as an inline error.
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
    const { styles, cx } = useStyles()
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const openApiDefaults = useMemo(() => ({
        modelsModuleName: t('browser.create.openapi_defaults.data_module_name'),
        modelsPath: t('browser.create.openapi_defaults.data_module_path'),
        algorithmsModuleName: t('browser.create.openapi_defaults.rules_module_name'),
        algorithmsPath: t('browser.create.openapi_defaults.rules_module_path'),
    }), [t])
    const [step, setStep] = useState<'method' | 'config'>('method')
    const [mode, setMode] = useState<CreateMode>('template')
    const [repoId, setRepoId] = useState('')
    const [copySource, setCopySource] = useState<string | null>(null)
    const [name, setName] = useState('')
    const [comment, setComment] = useState('')
    const [path, setPath] = useState('')
    const [archive, setArchive] = useState<File | null>(null)
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
    const templatesLoaded = useRef(false)

    const creatableRepos = useMemo(
        () => repositories.filter(repo => repo.capabilities?.canCreateProject),
        [repositories]
    )
    const repoOptions = creatableRepos
    const repository = useMemo(() => creatableRepos.find(repo => repo.id === repoId) ?? null, [creatableRepos, repoId])
    const repositorySupportsFolders = supportsMappedFolders(repository)
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

    const close = () => {
        setStep('method')
        setMode('template')
        setName('')
        setComment('')
        setPath('')
        setArchive(null)
        setExcelFiles([])
        setTemplateGroup(null)
        setTemplate(null)
        setOpenApiFile(null)
        setOpenApi(openApiDefaults)
        setWorkspaceProjects([])
        setCopyProjects(null)
        setLocalProjects(null)
        setCopySource(null)
        templatesLoaded.current = false
        setError(null)
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
        if (mode === 'copy' && !copyableProjectSources.some(candidate => candidate.id === copySource)) {
            setError(t('browser.create.copy_source_required'))
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
                        repositorySupportsFolders ? path.trim() || undefined : undefined
                    )
                } else if (mode === 'workspace') {
                    await createProjectsFromWorkspace(repository.id, {
                        names: workspaceProjects,
                        path: repositorySupportsFolders ? path.trim() || undefined : undefined,
                        comment: comment.trim() || undefined,
                    })
                } else if (mode === 'template') {
                    const [type, category, name_] = JSON.parse(template!) as [string, string, string]
                    await createProject(repository.id, trimmedName, {
                        template: { type, category, name: name_ },
                        path: repositorySupportsFolders ? path.trim() || undefined : undefined,
                        comment: comment.trim() || undefined,
                    })
                } else {
                    await createProject(repository.id, trimmedName, {
                        files: contentFiles(),
                        ...(mode === 'openapi' ? { openApi } : {}),
                        path: repositorySupportsFolders ? path.trim() || undefined : undefined,
                        comment: comment.trim() || undefined,
                    })
                }
                onCreated()
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

    const repoSelect = (
        <div className={styles.field}>
            <span className={styles.label}>{t('browser.create.repository_label')}</span>
            {repoSelectInput}
        </div>
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
                            className={cx(styles.tile, mode === method.id && styles.tileActive)}
                            data-testid={`new-project-method-${method.id}`}
                            onClick={() => setMode(method.id)}
                            onDoubleClick={() => setStep('config')}
                            type="button"
                        >
                            <Icon />
                            <span className={styles.tileLabel}>{t(method.labelKey)}</span>
                            <span className={styles.tileDesc}>{t(method.descKey)}</span>
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
                                className={styles.groupCard}
                                data-testid={`template-group-${group.category}`}
                                onClick={() => { setTemplateGroup(groupKey(group)); setTemplate(null) }}
                                type="button"
                            >
                                <FolderOutlined />
                                <span className={styles.groupName}>{titleCase(group.category)}</span>
                                {group.type === 'custom' && <span className={styles.customBadge}>{t('browser.create.template_custom')}</span>}
                                <span className={styles.groupCount}>{group.templates.length}</span>
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
                                        className={cx(styles.templateCard, template === key && styles.templateCardActive)}
                                        data-testid={`template-${key}`}
                                        onClick={() => setTemplate(key)}
                                        type="button"
                                    >
                                        {template === key ? <CheckCircleFilled /> : <ProfileOutlined />}
                                        <span className={styles.templateName}>{name_}</span>
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
                        <span className={styles.groupCount}>{workspaceProjects.length}/{workspaceSources.length}</span>
                    </div>
                    {workspaceSources.length === 0 ? (
                        <div className={styles.workspaceEmpty}>{t('browser.create.workspace_empty')}</div>
                    ) : (
                        <div className={styles.cardScroll} data-testid="new-project-workspace">
                            <div className={styles.workspaceList}>
                                {workspaceSources.map(projectName => (
                                    <div
                                        key={projectName}
                                        className={cx(styles.workspaceCard, workspaceProjects.includes(projectName) && styles.templateCardActive)}
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
                                        <span className={styles.templateName}>{projectName}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}
            {mode === 'copy' && (
                <div className={styles.field}>
                    <span className={styles.label}>{t('browser.create.copy_source')}</span>
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
                </div>
            )}
            {mode === 'archive' && (
                <div className={styles.field}>
                    <Upload.Dragger
                        accept=".zip"
                        beforeUpload={file => { setArchive(file); setError(null); return false }}
                        data-testid="new-project-upload"
                        fileList={fileList}
                        maxCount={1}
                        onRemove={() => setArchive(null)}
                    >
                        <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                        <p className="ant-upload-text">{t('browser.create.archive_hint')}</p>
                        <p className="ant-upload-hint">{t('browser.create.archive_subhint')}</p>
                    </Upload.Dragger>
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
                    <Input data-testid="new-project-openapi-data-module" onChange={e => setOpenApi(prev => ({ ...prev, modelsModuleName: e.target.value }))} placeholder={t('browser.create.openapi_data_module')} style={{ marginBottom: 8 }} value={openApi.modelsModuleName} />
                    <Input data-testid="new-project-openapi-data-path" onChange={e => setOpenApi(prev => ({ ...prev, modelsPath: e.target.value }))} placeholder={t('browser.create.openapi_data_path')} style={{ marginBottom: 8 }} value={openApi.modelsPath} />
                    <Input data-testid="new-project-openapi-rules-module" onChange={e => setOpenApi(prev => ({ ...prev, algorithmsModuleName: e.target.value }))} placeholder={t('browser.create.openapi_rules_module')} style={{ marginBottom: 8 }} value={openApi.algorithmsModuleName} />
                    <Input data-testid="new-project-openapi-rules-path" onChange={e => setOpenApi(prev => ({ ...prev, algorithmsPath: e.target.value }))} placeholder={t('browser.create.openapi_rules_path')} style={{ marginBottom: 12 }} value={openApi.algorithmsPath} />
                </>
            )}
            {showName ? (
                <div className={styles.nameRepoRow}>
                    <div>
                        <span className={styles.label}>{t('browser.create.name')}</span>
                        <Input
                            data-testid="new-project-name"
                            onChange={event => setName(event.target.value)}
                            placeholder={t('browser.create.name')}
                            value={name}
                        />
                    </div>
                    <div>
                        <span className={styles.label}>{t('browser.create.repository_label')}</span>
                        {repoSelectInput}
                    </div>
                </div>
            ) : (
                repoSelect
            )}
            {repositorySupportsFolders && (
                <div className={styles.field}>
                    <span className={styles.label}>{t('browser.create.path')}</span>
                    <Input data-testid="new-project-path" onChange={event => setPath(event.target.value)} value={path} />
                </div>
            )}
            <div className={styles.field}>
                <span className={styles.label}>{t('browser.create.comment')}</span>
                <Input.TextArea data-testid="new-project-comment" onChange={event => setComment(event.target.value)} rows={2} value={comment} />
            </div>
            {error && <Alert showIcon data-testid="new-project-error" style={{ marginTop: 4 }} title={error} type="error" />}
        </>
    )

    const footer = step === 'method' ? (
        <div className={styles.footer}>
            <span />
            <div className={styles.footerRight}>
                <Button data-testid="new-project-cancel" onClick={close}>{t('browser.create.cancel')}</Button>
                <Button data-testid="new-project-next" onClick={() => setStep('config')} type="primary">
                    {t('browser.create.next')}
                </Button>
            </div>
        </div>
    ) : (
        <div className={styles.footer}>
            <Button data-testid="new-project-back" icon={<ArrowLeftOutlined />} onClick={() => setStep('method')} type="text">
                {t('browser.create.back')}
            </Button>
            <div className={styles.footerRight}>
                <Button data-testid="new-project-cancel" disabled={submitting} onClick={close}>{t('browser.create.cancel')}</Button>
                <Button data-testid="new-project-submit" loading={submitting} onClick={submit} type="primary">
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
                width={step === 'method' ? 640 : 520}
            >
                {step === 'method' ? methodStep : configStep}
            </Modal>
            {commitInfoModal}
        </>
    )
}
