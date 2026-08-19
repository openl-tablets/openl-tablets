import { lazy, Suspense, useEffect, useRef, useState, type ReactNode } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Input, notification, Select, Skeleton, Space, Switch, Tag } from 'antd'
import { createStyles } from 'antd-style'
import { EditToolbar } from './EditToolbar'
import { FieldRow } from '../../components/FieldRow'
import { getFileContent, rootFileExists, writeRootFile } from '../../services/files'
import { MigrateButton, useDescriptorMigration } from './projectMigration'
import {
    DeployConfigParseError,
    EMPTY_DEPLOY_CONFIG,
    PUBLISHER_TYPES,
    parseDeployConfig,
    serializeDeployConfig,
    type DeployConfig,
} from '../../services/rulesDeploy'

const FILE_PATH = 'rules-deploy.xml'

// The configuration block is raw XML; reuse the syntax-highlighted editor (lazy — it pulls in CodeMirror).
const CodeEditor = lazy(() => import('./CodeEditor').then(module => ({ default: module.CodeEditor })))

const useStyles = createStyles(({ css, token }) => ({
    /** The whole descriptor block: its own header above the framed body, like the overview tab. */
    root: css`
        min-width: 0;
    `,
    /** The header row: the descriptor's name on the left, its edit controls on the right. */
    header: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        margin: 0 0 12px;
    `,
    /** The descriptor's name, with the file it is kept in as a muted hint. */
    title: css`
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0;
        font-size: 14px;
        font-weight: 600;
    `,
    titleHint: css`
        color: ${token.colorTextTertiary};
        font-size: 11px;
        font-weight: 400;
    `,
    /** The framed body the fields sit in. */
    box: css`
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        overflow: hidden;
    `,
    panel: css`
        padding: 16px;
    `,
    /**
     * A value the descriptor carries, read the way the fields that set it are laid out. It stands as
     * tall as the input that replaces it on edit, so turning to the editing view moves nothing.
     */
    value: css`
        display: flex;
        align-items: center;
        min-height: ${token.controlHeight}px;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    empty: css`
        color: ${token.colorTextQuaternary};
    `,
    tags: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 4px;
        min-height: ${token.controlHeight}px;
    `,
    /** The raw XML always reads as the file it is: an input, but never edited from here. */
    xml: css`
        width: 100%;
        height: 180px;
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadius}px;
        overflow: hidden;
    `,
}))

interface DeployConfigPanelProps {
    projectId: string
    canWrite: boolean
    /** Called after the descriptor is saved so project status and the files tree can refresh. */
    onSaved?: () => void
    /** Bumped when the project reloads (save/close); forces the descriptor to refetch so it never goes stale. */
    reloadToken?: number
}

/**
 * A project's `rules-deploy.xml` deployment descriptor.
 *
 * <p>The descriptor reads as plain values by default, whether or not the project is open for editing.
 * A project that may be written offers an Edit button that turns the values into the fields that set
 * them; the raw XML block stays a read-only view of the file throughout. The XML is read and written
 * through the Files API, and the form maps the common publishing settings while preserving any other
 * elements. A missing descriptor starts blank and is created on the first save.
 *
 * <p>An edit under way ends only by its own Save or Cancel. A reload of the same project refreshes the
 * values underneath it — what a save writes over, what a cancel returns to — and leaves the draft alone.
 */
export const DeployConfigPanel = ({ projectId, canWrite, onSaved, reloadToken }: DeployConfigPanelProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    // The working copy an edit changes; off the edit it stands for nothing and the read view is shown.
    const [config, setConfig] = useState<DeployConfig>(EMPTY_DEPLOY_CONFIG)
    // The descriptor as it was last read or saved: what is shown off an edit, and what one starts from.
    const [savedConfig, setSavedConfig] = useState<DeployConfig>(EMPTY_DEPLOY_CONFIG)
    const [originalXml, setOriginalXml] = useState('')
    // Whether the project has the file, as the last read that answered found it. A save writes against
    // this rather than against the shown state, which also stands for a read that failed.
    const [fileExists, setFileExists] = useState(false)
    const [state, setState] = useState<'loading' | 'ready' | 'missing' | 'invalid' | 'error'>('loading')
    const [editing, setEditing] = useState(false)
    const [saving, setSaving] = useState(false)
    // The project the descriptor on screen belongs to; another one starts from nothing, the same one is
    // only refreshed.
    const readFor = useRef<string | null>(null)
    // Whether the shown descriptor is being replaced by a fresh read of the same project.
    const [refreshing, setRefreshing] = useState(false)
    // How many saves this descriptor has seen. A read that started before one is answering a question the
    // save has since answered better, so its values are dropped instead of put back over what was written.
    const saves = useRef(0)
    const { migration, migrating, run } = useDescriptorMigration(projectId, canWrite, reloadToken, () => onSaved?.())
    const migratable = migration.rulesDeploy.migratable

    useEffect(() => {
        let cancelled = false
        const sameProject = readFor.current === projectId
        if (!sameProject) {
            readFor.current = projectId
            setState('loading')
            // Another project is another descriptor, so an edit of the previous one has nothing to save here.
            setEditing(false)
        }
        // A re-read of the same project keeps what is shown: a skeleton in its place would take the values
        // away and put them back, and would wipe the form of an edit under way. The Edit button is off
        // offer while it runs, since a draft started here would come from values about to be replaced.
        setRefreshing(sameProject)
        // A save that writes the file while this read is in flight makes this answer the older one, and it
        // is then dropped rather than put back over what the save wrote.
        const savesAtRead = saves.current
        const overtakenBySave = () => saves.current !== savesAtRead
        // Look the descriptor up first: a missing rules-deploy.xml is an empty search result, not a 404.
        rootFileExists(projectId, FILE_PATH)
            .then(async exists => {
                if (cancelled) {
                    return
                }
                const xml = exists ? await getFileContent(projectId, FILE_PATH) : ''
                if (cancelled) {
                    return
                }
                // The read is over either way, so the Edit button is back on offer.
                setRefreshing(false)
                if (overtakenBySave()) {
                    return
                }
                setOriginalXml(xml)
                setFileExists(exists)
                if (!exists) {
                    setSavedConfig({ ...EMPTY_DEPLOY_CONFIG })
                    setState('missing')
                    return
                }
                try {
                    setSavedConfig(parseDeployConfig(xml))
                    setState('ready')
                } catch (e) {
                    if (!(e instanceof DeployConfigParseError)) {
                        throw e
                    }
                    setSavedConfig({ ...EMPTY_DEPLOY_CONFIG })
                    setState('invalid')
                }
            })
            .catch(() => {
                if (cancelled) {
                    return
                }
                setRefreshing(false)
                if (overtakenBySave()) {
                    return
                }
                setOriginalXml('')
                setSavedConfig({ ...EMPTY_DEPLOY_CONFIG })
                setState('error')
            })
        return () => {
            cancelled = true
        }
    }, [projectId, reloadToken])

    const set = <K extends keyof DeployConfig>(key: K, value: DeployConfig[K]) =>
        setConfig(prev => ({ ...prev, [key]: value }))

    /** What the panel shows: the working copy of an edit, or the descriptor as it was last read. */
    const shown = editing ? config : savedConfig

    const startEditing = () => {
        setConfig(savedConfig)
        setEditing(true)
    }

    const save = async () => {
        setSaving(true)
        try {
            const xml = serializeDeployConfig(config, originalXml)
            await writeRootFile(projectId, FILE_PATH, xml, fileExists ? 'overwrite' : 'create')
            // The file now says what this save wrote, so a read that started before it is answering a
            // question this save has answered better: its values are dropped rather than put back.
            saves.current += 1
            setOriginalXml(xml)
            setFileExists(true)
            setSavedConfig(config)
            notification.success({ title: t('browser.deploy_config.saved') })
            setState('ready')
            setEditing(false)
            onSaved?.()
        } catch (e) {
            notification.error({
                title: t('browser.deploy_config.save_failed'),
                description: errorMessage(e),
            })
        } finally {
            setSaving(false)
        }
    }

    // A value reads as text off the edit, and as the field that sets it on. An empty value reads as a
    // muted note rather than a blank, so the descriptor is legible even before it is filled in.
    const textValue = (testId: string, value: string): ReactNode => value
        ? <span className={styles.value} data-testid={testId}>{value}</span>
        : <span className={cx(styles.value, styles.empty)} data-testid={testId}>{t('browser.deploy_config.empty')}</span>

    const inputRow = (testId: string, key: 'serviceName' | 'serviceClass' | 'version' | 'url'
        | 'annotationTemplateClassName' | 'groups', label: string) => (
        <FieldRow label={t(label)} labelWidth={180}>
            {editing
                ? <Input data-testid={testId} onChange={e => set(key, e.target.value)} value={shown[key]} />
                : textValue(testId, shown[key])}
        </FieldRow>
    )

    // The read view of the publishers: their tags, or the muted "not set" note when there are none.
    const publishersValue = shown.publishers.length > 0
        ? (
            <span className={styles.tags} data-testid="deploy-publishers">
                {shown.publishers.map(type => <Tag key={type}>{type}</Tag>)}
            </span>
        )
        : textValue('deploy-publishers', '')

    // The edit controls only make sense once there is a descriptor to edit or create; a load error or a
    // malformed file offers none. They match the overview: small buttons at the end of the header row.
    // An edit under way keeps them whatever a reload found, so it is never left with no way out.
    const canEdit = canWrite && (editing || state === 'ready' || state === 'missing')
    const header = (
        <div className={styles.header}>
            <h3 className={styles.title}>
                {t('browser.deploy_config.title')}
                <span className={styles.titleHint}>{FILE_PATH}</span>
            </h3>
            <Space size={8}>
                {migratable && canWrite && !editing && (
                    <MigrateButton
                        label={t('browser.deploy_config.migrate')}
                        loading={migrating}
                        onClick={() => run('rulesDeploy', t('browser.deploy_config.migrate_failed'))}
                        testId="deploy-migrate"
                        tooltip={t('browser.deploy_config.migrate_hint')}
                    />
                )}
                {canEdit && (
                    <EditToolbar
                        disabled={migrating}
                        disabledEdit={refreshing}
                        editing={editing}
                        labels={{ edit: t('browser.deploy_config.edit'), save: t('browser.deploy_config.save'), cancel: t('browser.deploy_config.cancel') }}
                        onCancel={() => setEditing(false)}
                        onEdit={startEditing}
                        onSave={save}
                        saving={saving}
                        testId="deploy-config"
                    />
                )}
            </Space>
        </div>
    )

    /** The descriptor as rows of values — the form an edit turns into fields. */
    const form = (): ReactNode => (
        <div className={styles.panel} data-testid="deploy-config">
            {state === 'missing' && (
                <Alert showIcon data-testid="deploy-config-missing" style={{ marginBottom: 16 }} title={t('browser.deploy_config.none')} type="info" />
            )}
            <FieldRow label={t('browser.deploy_config.provide_runtime_context')} labelWidth={180}>
                <Switch
                    checked={shown.provideRuntimeContext}
                    data-testid="deploy-runtime-context"
                    disabled={!editing}
                    onChange={checked => set('provideRuntimeContext', checked)}
                />
            </FieldRow>
            <FieldRow label={t('browser.deploy_config.publishers')} labelWidth={180}>
                {editing
                    ? (
                        <Select
                            data-testid="deploy-publishers"
                            mode="tags"
                            onChange={value => set('publishers', value)}
                            options={PUBLISHER_TYPES.map(type => ({ label: type, value: type }))}
                            placeholder={t('browser.deploy_config.publishers_placeholder')}
                            style={{ width: '100%' }}
                            value={shown.publishers}
                        />
                    )
                    : publishersValue}
            </FieldRow>
            {inputRow('deploy-service-name', 'serviceName', 'browser.deploy_config.service_name')}
            {inputRow('deploy-service-class', 'serviceClass', 'browser.deploy_config.service_class')}
            {inputRow('deploy-version', 'version', 'browser.deploy_config.version')}
            {inputRow('deploy-url', 'url', 'browser.deploy_config.url')}
            {inputRow('deploy-annotation-template', 'annotationTemplateClassName', 'browser.deploy_config.annotation_template')}
            {inputRow('deploy-groups', 'groups', 'browser.deploy_config.groups')}
            <FieldRow alignTop label={t('browser.deploy_config.configuration')} labelWidth={180}>
                <div className={styles.xml} data-testid="deploy-configuration">
                    <Suspense fallback={<Skeleton active paragraph={{ rows: 3 }} style={{ padding: 12 }} title={false} />}>
                        {/* The raw XML is shown as the file it is, and never edited from this form. */}
                        <CodeEditor readOnly path="configuration.xml" value={shown.configuration} />
                    </Suspense>
                </div>
            </FieldRow>
        </div>
    )

    const body = (): ReactNode => {
        // An edit under way keeps its form whatever a reload found: it ends by its own Save or Cancel,
        // never by a skeleton or an error taking the fields it is being typed into away.
        if (editing) {
            return form()
        }
        if (state === 'loading') {
            return <Skeleton active paragraph={{ rows: 6 }} style={{ padding: 16 }} />
        }
        if (state === 'error') {
            return <Alert showIcon data-testid="deploy-config-error" style={{ margin: 16 }} title={t('browser.deploy_config.load_failed')} type="error" />
        }
        if (state === 'invalid') {
            return <Alert showIcon data-testid="deploy-config-invalid" style={{ margin: 16 }} title={t('browser.deploy_config.invalid')} type="error" />
        }
        return form()
    }

    return (
        <div className={styles.root} data-testid="deploy-config-panel">
            {header}
            <div className={styles.box}>{body()}</div>
        </div>
    )
}
