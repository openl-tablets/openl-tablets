import React, { useEffect, useState } from 'react'
import { Alert, App, Badge, Button, Checkbox, Input, Select, Skeleton, Typography } from 'antd'
import { CloseOutlined, PlusOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { fetchProject } from 'services/projects'
import {
    fetchProjectDescriptor,
    fetchProjectStatus,
    generateProjectOpenApiSchema,
    isApiHttpError,
    subscribeProjectStatus,
    updateProjectDescriptor,
} from 'services'
import type { ProjectCompileState } from 'services'
import type { Project } from '../../types/projects'
import type {
    ProjectDescriptorDependency,
    ProjectDescriptorExposedMethod,
    ProjectDescriptorExposedType,
    ProjectDescriptorModule,
    ProjectDescriptorOpenApi,
    ProjectDescriptorOpenApiMode,
    ProjectDescriptorView,
} from '../../types/projectDescriptor'
import { StringListEditor } from 'components/StringListEditor'
import { SortablePatternList } from 'components/SortablePatternList'
import { formatDateTime } from 'utils/dateFormat'
import { useStyles } from './ProjectPage.styles'

interface ProjectPageProps {
    projectId: string
}

type BadgeStatus = 'default' | 'processing' | 'success' | 'warning' | 'error'
type Styles = ReturnType<typeof useStyles>['styles']

const COMPILE_STATE_BADGE: Record<ProjectCompileState, BadgeStatus> = {
    idle: 'default',
    compiling: 'processing',
    ok: 'success',
    warnings: 'warning',
    errors: 'error',
}

/**
 * Read/edit view of a project, rendered as a light document (like a rendered README) rather than a
 * stack of cards. Portaled into the legacy JSF `project.xhtml` placeholder.
 *
 * <p>Read-only by default; a single Edit turns the page editable at once. Every list is edited per
 * line (an input per entry with add/remove), never a free-text area. Save writes the whole
 * `rules.xml` in one `PUT /projects/{id}/descriptor` (validated and recompiled server-side); a 409
 * means the descriptor changed since it was opened, and the user confirms to overwrite.
 */
export const ProjectPage: React.FC<ProjectPageProps> = ({ projectId }) => {
    const { t } = useTranslation()
    const { styles, cx } = useStyles()
    const { modal, notification } = App.useApp()
    const [project, setProject] = useState<Project>()
    const [descriptor, setDescriptor] = useState<ProjectDescriptorView>()
    const [form, setForm] = useState<ProjectDescriptorView | null>(null)
    const [compileState, setCompileState] = useState<ProjectCompileState>()
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [generating, setGenerating] = useState(false)
    const [failed, setFailed] = useState(false)

    const branch = project?.branch
    const editing = form !== null

    useEffect(() => {
        if (!projectId) {
            return
        }
        let active = true
        setLoading(true)
        setFailed(false)
        Promise.all([fetchProject(projectId), fetchProjectDescriptor(projectId)])
            .then(([loadedProject, loadedDescriptor]) => {
                if (active) {
                    setProject(loadedProject)
                    setDescriptor(loadedDescriptor)
                }
            })
            .catch(() => {
                if (active) {
                    setFailed(true)
                }
            })
            .finally(() => {
                if (active) {
                    setLoading(false)
                }
            })
        return () => {
            active = false
        }
    }, [projectId])

    useEffect(() => {
        if (!projectId) {
            return
        }
        let active = true
        fetchProjectStatus(projectId)
            .then((status) => {
                if (active) {
                    setCompileState(status.compileState)
                }
            })
            .catch(() => {
                // Status is best-effort; the subscription below still delivers transitions.
            })
        const subscription = subscribeProjectStatus(projectId, branch ?? null, (status) => {
            setCompileState(status.compileState)
        })
        return () => {
            active = false
            subscription.unsubscribe()
        }
    }, [projectId, branch])

    const update = (patch: Partial<ProjectDescriptorView>) => setForm((current) => (current ? { ...current, ...patch } : current))
    const setOpenApi = (next: ProjectDescriptorOpenApi | undefined) =>
        setForm((current) => {
            if (!current) {
                return current
            }
            const draft = { ...current }
            if (next) {
                draft.openapi = next
            } else {
                delete draft.openapi
            }
            return draft
        })

    const save = (force: boolean) => {
        const body = form
        if (!body) {
            return
        }
        setSaving(true)
        updateProjectDescriptor(projectId, body, force)
            .then((updated) => {
                setDescriptor(updated)
                setForm(null)
                notification.success({ title: t('project:page.saved') })
            })
            .catch((error) => {
                if (isApiHttpError(error) && error.status === 409) {
                    modal.confirm({
                        title: t('project:page.overwrite_title'),
                        content: t('project:page.overwrite_body'),
                        okText: t('project:page.overwrite_ok'),
                        okButtonProps: { danger: true },
                        onOk: () => save(true),
                    })
                } else {
                    notification.error({
                        title: t('project:page.save_failed'),
                        description: error instanceof Error ? error.message : String(error),
                    })
                }
            })
            .finally(() => setSaving(false))
    }

    const generateSchema = () => {
        setGenerating(true)
        generateProjectOpenApiSchema(projectId)
            .then((updated) => {
                setDescriptor(updated)
                notification.success({ title: t('project:page.openapi_generated') })
            })
            .catch((error) => {
                notification.error({
                    title: t('project:page.openapi_generate_failed'),
                    description: error instanceof Error ? error.message : String(error),
                })
            })
            .finally(() => setGenerating(false))
    }

    if (loading) {
        return (
            <div className={styles.page}>
                <Skeleton active paragraph={{ rows: 6 }} />
            </div>
        )
    }

    if (failed || !descriptor) {
        return (
            <div className={styles.page}>
                <Alert showIcon message={t('project:page.load_failed')} type="error" />
            </div>
        )
    }

    const view = form ?? descriptor
    const name = view.name || project?.name
    const comment = view.comment || project?.comment
    const modules = view.modules ?? []
    const dependencies = view.dependencies ?? []
    const classpath = view.classpath ?? []
    const exposedMethods = view.exposedMethods ?? []
    const propertiesPatterns = view.propertiesFileNamePatterns ?? []
    const openapi = view.openapi

    return (
        <div className={styles.page} data-testid="project-page">
            <div className={styles.header}>
                {editing ? (
                    <Input
                        className={styles.titleInput}
                        data-testid="project-name-input"
                        onChange={(event) => update({ name: event.target.value })}
                        placeholder={project?.name}
                        value={view.name ?? ''}
                    />
                ) : (
                    <Typography.Title className={styles.title} level={2}>
                        {name}
                    </Typography.Title>
                )}
                <div className={styles.actions}>
                    {editing ? (
                        <>
                            <Button loading={saving} onClick={() => save(false)} type="primary">
                                {t('project:page.save')}
                            </Button>
                            <Button disabled={saving} onClick={() => setForm(null)}>
                                {t('project:page.cancel')}
                            </Button>
                        </>
                    ) : (
                        descriptor.editable && (
                            <Button data-testid="edit-button" onClick={() => setForm(descriptor)}>
                                {t('project:page.edit')}
                            </Button>
                        )
                    )}
                </div>
            </div>
            {editing ? (
                <Input.TextArea
                    autoSize={{ minRows: 2 }}
                    className={styles.field}
                    onChange={(event) => update({ comment: event.target.value })}
                    placeholder={t('project:page.comment_placeholder')}
                    value={view.comment ?? ''}
                />
            ) : (
                comment && <Typography.Paragraph className={styles.lead}>{comment}</Typography.Paragraph>
            )}
            {project && (
                <dl className={styles.meta}>
                    {project.status && (
                        <>
                            <dt>{t('project:summary.status')}</dt>
                            <dd>{String(project.status)}</dd>
                        </>
                    )}
                    {compileState && (
                        <>
                            <dt>{t('project:summary.compilation')}</dt>
                            <dd>
                                <Badge
                                    status={COMPILE_STATE_BADGE[compileState]}
                                    text={t(`project:summary.compile_state.${compileState}`)}
                                />
                            </dd>
                        </>
                    )}
                    {project.modifiedBy && (
                        <>
                            <dt>{t('project:summary.modified_by')}</dt>
                            <dd>{project.modifiedBy}</dd>
                        </>
                    )}
                    {project.modifiedAt && (
                        <>
                            <dt>{t('project:summary.modified_at')}</dt>
                            <dd>{formatDateTime(project.modifiedAt)}</dd>
                        </>
                    )}
                    {project.repository && (
                        <>
                            <dt>{t('project:summary.repository')}</dt>
                            <dd>{project.repository}</dd>
                        </>
                    )}
                    {project.branch && (
                        <>
                            <dt>{t('project:summary.branch')}</dt>
                            <dd>{project.branch}</dd>
                        </>
                    )}
                    {project.path && (
                        <>
                            <dt>{t('project:summary.path')}</dt>
                            <dd className={styles.code}>{project.path}</dd>
                        </>
                    )}
                </dl>
            )}
            <section className={styles.section}>
                <h2 className={styles.heading}>{t('project:page.modules')}</h2>
                {editing ? (
                    <ModulesEditor cx={cx} modules={modules} onChange={(next) => update({ modules: next })} styles={styles} t={t} />
                ) : modules.length === 0 ? (
                    <p className={styles.empty}>{t('project:page.empty_modules')}</p>
                ) : (
                    modules.map((module, index) => (
                        <ModuleItem key={module.rulesRootPath ?? module.name ?? index} module={module} styles={styles} t={t} />
                    ))
                )}
            </section>
            <section className={styles.section}>
                <h2 className={styles.heading}>{t('project:page.dependencies')}</h2>
                {editing ? (
                    <DependenciesEditor
                        dependencies={dependencies}
                        onChange={(next) => update({ dependencies: next })}
                        styles={styles}
                        t={t}
                    />
                ) : dependencies.length === 0 ? (
                    <p className={styles.empty}>{t('project:page.empty_dependencies')}</p>
                ) : (
                    <ul className={styles.list}>
                        {dependencies.map((dependency, index) => (
                            <li key={dependency.name ?? index}>
                                {dependency.name}
                                {dependency.autoIncluded && (
                                    <span className={styles.subtle}> · {t('project:page.dependency_all_modules')}</span>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </section>
            <section className={styles.section}>
                <h2 className={styles.heading}>{t('project:page.sources')}</h2>
                {editing ? (
                    <>
                        <p className={styles.fieldHint}>{t('project:page.sources_hint')}</p>
                        <StringListEditor
                            mono
                            addLabel={t('project:page.add_source')}
                            onChange={(next) => update({ classpath: next })}
                            placeholder={t('project:page.source_placeholder')}
                            removeLabel={t('project:page.remove')}
                            values={classpath}
                        />
                    </>
                ) : classpath.length === 0 ? (
                    <p className={styles.empty}>{t('project:page.empty_sources')}</p>
                ) : (
                    <ul className={styles.list}>
                        {classpath.map((entry, index) => (
                            <li key={index}>
                                <span className={styles.code}>{entry}</span>
                            </li>
                        ))}
                    </ul>
                )}
            </section>
            {(editing || exposedMethods.length > 0) && (
                <section className={styles.section}>
                    <h2 className={styles.heading}>{t('project:page.exposed_methods')}</h2>
                    {editing ? (
                        <>
                            <p className={styles.fieldHint}>{t('project:page.exposed_hint')}</p>
                            <ExposedMethodsEditor
                                cx={cx}
                                methods={exposedMethods}
                                onChange={(next) => update({ exposedMethods: next })}
                                styles={styles}
                                t={t}
                            />
                        </>
                    ) : (
                        <ul className={styles.list}>
                            {exposedMethods.map((method, index) => (
                                <li key={index}>
                                    <span className={styles.subtle}>{t(`project:page.exposed_type.${method.type}`)}</span>{' '}
                                    <span className={styles.code}>{method.pattern}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            )}
            {(editing || propertiesPatterns.length > 0) && (
                <section className={styles.section}>
                    <h2 className={styles.heading}>{t('project:page.properties_patterns')}</h2>
                    {editing ? (
                        <>
                            <p className={styles.fieldHint}>{t('project:page.properties_patterns_hint')}</p>
                            <SortablePatternList
                                addLabel={t('project:page.add_properties_pattern')}
                                dragLabel={t('project:page.drag_to_reorder')}
                                onChange={(next) => update({ propertiesFileNamePatterns: next })}
                                placeholder={t('project:page.properties_pattern_placeholder')}
                                removeLabel={t('project:page.remove')}
                                values={propertiesPatterns}
                            />
                        </>
                    ) : (
                        <ol className={styles.orderedList}>
                            {propertiesPatterns.map((pattern, index) => (
                                <li key={index}>
                                    <span className={styles.code}>{pattern}</span>
                                </li>
                            ))}
                        </ol>
                    )}
                </section>
            )}
            {(editing || openapi || descriptor.editable) && (
                <section className={styles.section}>
                    <h2 className={styles.heading}>{t('project:page.openapi')}</h2>
                    {editing ? (
                        <OpenApiEditor cx={cx} onChange={setOpenApi} styles={styles} t={t} value={openapi} />
                    ) : (
                        <>
                            {openapi ? (
                                <dl className={styles.meta}>
                                    {openapi.path && (
                                        <>
                                            <dt>{t('project:page.openapi_path')}</dt>
                                            <dd className={styles.code}>{openapi.path}</dd>
                                        </>
                                    )}
                                    {openapi.mode && (
                                        <>
                                            <dt>{t('project:page.openapi_mode')}</dt>
                                            <dd>{t(`project:page.openapi_mode_option.${openapi.mode}`)}</dd>
                                        </>
                                    )}
                                    {openapi.algorithmModuleName && (
                                        <>
                                            <dt>{t('project:page.openapi_rules_module')}</dt>
                                            <dd>{openapi.algorithmModuleName}</dd>
                                        </>
                                    )}
                                    {openapi.modelModuleName && (
                                        <>
                                            <dt>{t('project:page.openapi_data_module')}</dt>
                                            <dd>{openapi.modelModuleName}</dd>
                                        </>
                                    )}
                                </dl>
                            ) : (
                                <p className={styles.empty}>{t('project:page.empty_openapi')}</p>
                            )}
                            {descriptor.editable && (
                                <Button
                                    className={styles.field}
                                    data-testid="generate-openapi-button"
                                    loading={generating}
                                    onClick={generateSchema}
                                >
                                    {t('project:page.generate_schema')}
                                </Button>
                            )}
                        </>
                    )}
                </section>
            )}
        </div>
    )
}

interface ModuleItemProps {
    module: ProjectDescriptorModule
    styles: Styles
    t: TFunction
}

const ModuleItem: React.FC<ModuleItemProps> = ({ module, styles, t }) => (
    <div className={styles.module}>
        <span className={styles.moduleName}>{module.name ?? module.rulesRootPath}</span>
        {module.rulesRootPath && <span className={styles.code}>{module.rulesRootPath}</span>}
        {module.wildcard && <span className={styles.subtle}> ({t('project:page.module_pattern')})</span>}
    </div>
)

interface DependenciesEditorProps {
    dependencies: ProjectDescriptorDependency[]
    onChange: (dependencies: ProjectDescriptorDependency[]) => void
    styles: Styles
    t: TFunction
}

const DependenciesEditor: React.FC<DependenciesEditorProps> = ({ dependencies, onChange, styles, t }) => {
    const setAt = (index: number, patch: Partial<ProjectDescriptorDependency>) =>
        onChange(dependencies.map((dependency, i) => (i === index ? { ...dependency, ...patch } : dependency)))
    const removeAt = (index: number) => onChange(dependencies.filter((_, i) => i !== index))
    const add = () => onChange([...dependencies, { name: '', autoIncluded: false }])
    return (
        <div>
            {dependencies.map((dependency, index) => (
                <div key={index} className={styles.editRow}>
                    <Input
                        className={styles.grow}
                        onChange={(event) => setAt(index, { name: event.target.value })}
                        placeholder={t('project:page.dependency_name')}
                        value={dependency.name ?? ''}
                    />
                    <Checkbox
                        checked={!!dependency.autoIncluded}
                        onChange={(event) => setAt(index, { autoIncluded: event.target.checked })}
                    >
                        {t('project:page.dependency_all_modules')}
                    </Checkbox>
                    <Button
                        aria-label={t('project:page.remove')}
                        className={styles.subtle}
                        icon={<CloseOutlined />}
                        onClick={() => removeAt(index)}
                        size="small"
                        type="text"
                    />
                </div>
            ))}
            <Button className={styles.addBtn} icon={<PlusOutlined />} onClick={add} size="small" type="link">
                {t('project:page.add_dependency')}
            </Button>
        </div>
    )
}

interface ModulesEditorProps {
    modules: ProjectDescriptorModule[]
    onChange: (modules: ProjectDescriptorModule[]) => void
    styles: Styles
    cx: ReturnType<typeof useStyles>['cx']
    t: TFunction
}

const ModulesEditor: React.FC<ModulesEditorProps> = ({ modules, onChange, styles, cx, t }) => {
    const setAt = (index: number, patch: Partial<ProjectDescriptorModule>) =>
        onChange(modules.map((module, i) => (i === index ? { ...module, ...patch } : module)))
    const removeAt = (index: number) => onChange(modules.filter((_, i) => i !== index))
    const add = () => onChange([...modules, {}])
    return (
        <div>
            {modules.map((module, index) => (
                <div key={index} className={styles.editRow}>
                    <Input
                        className={styles.grow}
                        onChange={(event) => setAt(index, { name: event.target.value })}
                        placeholder={t('project:page.module_name_label')}
                        value={module.name ?? ''}
                    />
                    <Input
                        className={cx(styles.grow, styles.monoInput)}
                        onChange={(event) => setAt(index, { rulesRootPath: event.target.value })}
                        placeholder={t('project:page.module_path_label')}
                        value={module.rulesRootPath ?? ''}
                    />
                    <Button
                        aria-label={t('project:page.remove')}
                        className={styles.subtle}
                        icon={<CloseOutlined />}
                        onClick={() => removeAt(index)}
                        size="small"
                        type="text"
                    />
                </div>
            ))}
            <Button className={styles.addBtn} icon={<PlusOutlined />} onClick={add} size="small" type="link">
                {t('project:page.add_module')}
            </Button>
        </div>
    )
}

interface ExposedMethodsEditorProps {
    methods: ProjectDescriptorExposedMethod[]
    onChange: (methods: ProjectDescriptorExposedMethod[]) => void
    styles: Styles
    cx: ReturnType<typeof useStyles>['cx']
    t: TFunction
}

const EXPOSED_TYPES: ProjectDescriptorExposedType[] = ['include', 'exclude']

const ExposedMethodsEditor: React.FC<ExposedMethodsEditorProps> = ({ methods, onChange, styles, cx, t }) => {
    const setAt = (index: number, patch: Partial<ProjectDescriptorExposedMethod>) =>
        onChange(methods.map((method, i) => (i === index ? { ...method, ...patch } : method)))
    const removeAt = (index: number) => onChange(methods.filter((_, i) => i !== index))
    const add = () => onChange([...methods, { pattern: '', type: 'include' }])
    return (
        <div>
            {methods.map((method, index) => (
                <div key={index} className={styles.editRow}>
                    <Input
                        className={cx(styles.grow, styles.monoInput)}
                        onChange={(event) => setAt(index, { pattern: event.target.value })}
                        placeholder={t('project:page.pattern_placeholder')}
                        value={method.pattern ?? ''}
                    />
                    <Select<ProjectDescriptorExposedType>
                        className={styles.typeSelect}
                        onChange={(value) => setAt(index, { type: value })}
                        options={EXPOSED_TYPES.map((type) => ({ value: type, label: t(`project:page.exposed_type.${type}`) }))}
                        value={method.type}
                    />
                    <Button
                        aria-label={t('project:page.remove')}
                        className={styles.subtle}
                        icon={<CloseOutlined />}
                        onClick={() => removeAt(index)}
                        size="small"
                        type="text"
                    />
                </div>
            ))}
            <Button className={styles.addBtn} icon={<PlusOutlined />} onClick={add} size="small" type="link">
                {t('project:page.add_pattern')}
            </Button>
        </div>
    )
}

interface OpenApiEditorProps {
    value?: ProjectDescriptorOpenApi | undefined
    onChange: (value: ProjectDescriptorOpenApi | undefined) => void
    styles: Styles
    cx: ReturnType<typeof useStyles>['cx']
    t: TFunction
}

const OPENAPI_MODES: ProjectDescriptorOpenApiMode[] = ['RECONCILIATION', 'GENERATION']

const OpenApiEditor: React.FC<OpenApiEditorProps> = ({ value, onChange, styles, cx, t }) => {
    // The <openapi> wrapper is dropped when every field is empty, so nothing partial is written.
    const set = (patch: Partial<ProjectDescriptorOpenApi>) => {
        const next = { ...value, ...patch }
        const empty = !next.path && !next.mode && !next.algorithmModuleName && !next.modelModuleName
        onChange(empty ? undefined : next)
    }
    return (
        <dl className={styles.metaEdit}>
            <dt>{t('project:page.openapi_path')}</dt>
            <dd>
                <Input
                    className={cx(styles.monoInput)}
                    onChange={(event) => set({ path: event.target.value })}
                    placeholder={t('project:page.openapi_path_placeholder')}
                    value={value?.path ?? ''}
                />
            </dd>
            <dt>{t('project:page.openapi_mode')}</dt>
            <dd>
                <Select<ProjectDescriptorOpenApiMode>
                    allowClear
                    onChange={(mode) => set({ mode })}
                    options={OPENAPI_MODES.map((mode) => ({ value: mode, label: t(`project:page.openapi_mode_option.${mode}`) }))}
                    placeholder={t('project:page.openapi_mode_placeholder')}
                    {...(value?.mode ? { value: value.mode } : {})}
                />
            </dd>
            <dt>{t('project:page.openapi_rules_module')}</dt>
            <dd>
                <Input
                    onChange={(event) => set({ algorithmModuleName: event.target.value })}
                    placeholder={t('project:page.openapi_module_placeholder')}
                    value={value?.algorithmModuleName ?? ''}
                />
            </dd>
            <dt>{t('project:page.openapi_data_module')}</dt>
            <dd>
                <Input
                    onChange={(event) => set({ modelModuleName: event.target.value })}
                    placeholder={t('project:page.openapi_module_placeholder')}
                    value={value?.modelModuleName ?? ''}
                />
            </dd>
        </dl>
    )
}
