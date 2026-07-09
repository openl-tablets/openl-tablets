import { lazy, Suspense, useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Form, Input, notification, Select, Skeleton, Space, Switch } from 'antd'
import { getFileContent, rootFileExists, updateFileContent, uploadFiles } from '../../services/files'
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

interface DeployConfigPanelProps {
    projectId: string
    canWrite: boolean
    /** Called after the descriptor is saved so project status and the files tree can refresh. */
    onSaved?: () => void
    /** Bumped when the project reloads (save/close); forces the descriptor to refetch so it never goes stale. */
    reloadToken?: number
}

/**
 * Structured editor for a project's `rules-deploy.xml` deployment descriptor. The XML is read and written
 * through the Files API; the form maps the common publishing settings while preserving any other
 * elements. Missing descriptors start blank and are created on the first save.
 */
export const DeployConfigPanel = ({ projectId, canWrite, onSaved, reloadToken }: DeployConfigPanelProps) => {
    const { t } = useTranslation('repository')
    const [config, setConfig] = useState<DeployConfig>(EMPTY_DEPLOY_CONFIG)
    const [originalXml, setOriginalXml] = useState('')
    const [state, setState] = useState<'loading' | 'ready' | 'missing' | 'invalid' | 'error'>('loading')
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        let cancelled = false
        setState('loading')
        // Look the descriptor up first: a missing rules-deploy.xml is an empty search result, not a 404.
        rootFileExists(projectId, FILE_PATH)
            .then(async exists => {
                if (cancelled) {
                    return
                }
                if (!exists) {
                    setOriginalXml('')
                    setConfig({ ...EMPTY_DEPLOY_CONFIG })
                    setState('missing')
                    return
                }
                const xml = await getFileContent(projectId, FILE_PATH)
                if (!cancelled) {
                    setOriginalXml(xml)
                    try {
                        setConfig(parseDeployConfig(xml))
                        setState('ready')
                    } catch (e) {
                        if (e instanceof DeployConfigParseError) {
                            setConfig({ ...EMPTY_DEPLOY_CONFIG })
                            setState('invalid')
                            return
                        }
                        throw e
                    }
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setOriginalXml('')
                    setConfig({ ...EMPTY_DEPLOY_CONFIG })
                    setState('error')
                }
            })
        return () => {
            cancelled = true
        }
    }, [projectId, reloadToken])

    const set = <K extends keyof DeployConfig>(key: K, value: DeployConfig[K]) =>
        setConfig(prev => ({ ...prev, [key]: value }))

    const save = async () => {
        setSaving(true)
        try {
            const xml = serializeDeployConfig(config, originalXml)
            if (state === 'missing') {
                await uploadFiles(projectId, '', [new File([xml], FILE_PATH, { type: 'application/xml' })])
            } else {
                await updateFileContent(projectId, FILE_PATH, xml)
            }
            setOriginalXml(xml)
            notification.success({ title: t('browser.deploy_config.saved') })
            setState('ready')
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

    if (state === 'loading') {
        return <Skeleton active paragraph={{ rows: 6 }} style={{ padding: 16 }} />
    }
    if (state === 'error') {
        return <Alert showIcon data-testid="deploy-config-error" style={{ margin: 16 }} title={t('browser.deploy_config.load_failed')} type="error" />
    }
    if (state === 'invalid') {
        return <Alert showIcon data-testid="deploy-config-invalid" style={{ margin: 16 }} title={t('browser.deploy_config.invalid')} type="error" />
    }

    return (
        <Form data-testid="deploy-config" layout="vertical" style={{ padding: 16 }}>
            {state === 'missing' && (
                <Alert showIcon data-testid="deploy-config-missing" style={{ marginBottom: 16 }} title={t('browser.deploy_config.none')} type="info" />
            )}
            <Form.Item label={t('browser.deploy_config.provide_runtime_context')}>
                <Switch
                    checked={config.provideRuntimeContext}
                    data-testid="deploy-runtime-context"
                    disabled={!canWrite}
                    onChange={checked => set('provideRuntimeContext', checked)}
                />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.publishers')}>
                <Select
                    data-testid="deploy-publishers"
                    disabled={!canWrite}
                    mode="tags"
                    onChange={value => set('publishers', value)}
                    options={PUBLISHER_TYPES.map(type => ({ label: type, value: type }))}
                    placeholder={t('browser.deploy_config.publishers_placeholder')}
                    value={config.publishers}
                />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.service_name')}>
                <Input data-testid="deploy-service-name" disabled={!canWrite} onChange={e => set('serviceName', e.target.value)} value={config.serviceName} />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.service_class')}>
                <Input data-testid="deploy-service-class" disabled={!canWrite} onChange={e => set('serviceClass', e.target.value)} value={config.serviceClass} />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.version')}>
                <Input data-testid="deploy-version" disabled={!canWrite} onChange={e => set('version', e.target.value)} value={config.version} />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.url')}>
                <Input data-testid="deploy-url" disabled={!canWrite} onChange={e => set('url', e.target.value)} value={config.url} />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.annotation_template')}>
                <Input data-testid="deploy-annotation-template" disabled={!canWrite} onChange={e => set('annotationTemplateClassName', e.target.value)} value={config.annotationTemplateClassName} />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.groups')}>
                <Input data-testid="deploy-groups" disabled={!canWrite} onChange={e => set('groups', e.target.value)} value={config.groups} />
            </Form.Item>
            <Form.Item label={t('browser.deploy_config.configuration')}>
                <div data-testid="deploy-configuration" style={{ height: 180, border: '1px solid #d9d9d9', borderRadius: 6, overflow: 'hidden' }}>
                    <Suspense fallback={<Skeleton active paragraph={{ rows: 3 }} style={{ padding: 12 }} title={false} />}>
                        <CodeEditor
                            onChange={value => set('configuration', value)}
                            path="configuration.xml"
                            readOnly={!canWrite}
                            value={config.configuration}
                        />
                    </Suspense>
                </div>
            </Form.Item>
            {canWrite && (
                <Button data-testid="deploy-config-save" loading={saving} onClick={save} type="primary">
                    {t('browser.deploy_config.save')}
                </Button>
            )}
        </Form>
    )
}
