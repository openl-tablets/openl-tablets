import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { App, Button, Space, Tooltip } from 'antd'
import { ApiOutlined, TableOutlined } from '@ant-design/icons'
import { errorMessage } from '../../utils/errorMessage'
import {
    generateOpenApiTables,
    getOpenApiGenerationPlan,
    type OpenApiGenerationPlan,
    type OpenApiModule,
    writeOpenApiSchema,
} from '../../services/openapi'
import type { DescriptorOpenApi } from '../../services/rulesDescriptor'

/**
 * The two things a project and its specification do to each other, which the legacy Editor offered from one
 * dialog: write the specification from the rules the project compiled, or generate the tables from a
 * specification somebody wrote first.
 *
 * <p>Both write into the project, so both are offered only to a reader who may write to it, and what they
 * wrote is read back by the caller rather than guessed at here.
 */
export const useOpenApiActions = (projectId: string, onWritten: () => void) => {
    const { modal } = App.useApp()
    const { t } = useTranslation('repository')
    const { notification } = App.useApp()
    const [running, setRunning] = useState(false)

    const writeSchema = async () => {
        setRunning(true)
        try {
            const written = await writeOpenApiSchema(projectId)
            notification.success({
                title: t(written.created ? 'browser.overview.openapi_written' : 'browser.overview.openapi_rewritten'),
                description: written.path,
            })
            onWritten()
        } catch (e) {
            notification.error({ title: t('browser.overview.openapi_write_failed'), description: errorMessage(e) })
        } finally {
            setRunning(false)
        }
    }

    /**
     * Asks what the generation would write before it writes it: a module the project already declares has
     * its workbook replaced, and a reader is owed that in words before they say yes.
     */
    const generateTables = async (openapi: DescriptorOpenApi) => {
        setRunning(true)
        try {
            const plan = await getOpenApiGenerationPlan(projectId, openapi.algorithmModuleName, openapi.modelModuleName)
            modal.confirm({
                title: t('browser.overview.openapi_generate'),
                content: <GenerationPlan plan={plan} />,
                okText: t('browser.overview.openapi_generate'),
                onOk: () => run(openapi, plan),
            })
        } catch (e) {
            notification.error({ title: t('browser.overview.openapi_generate_failed'), description: errorMessage(e) })
        } finally {
            setRunning(false)
        }
    }

    const run = async (openapi: DescriptorOpenApi, plan: OpenApiGenerationPlan) => {
        setRunning(true)
        try {
            await generateOpenApiTables(projectId, {
                path: openapi.path ?? '',
                algorithmModuleName: plan.algorithm.name,
                algorithmModulePath: plan.algorithm.path,
                modelModuleName: plan.model.name,
                modelModulePath: plan.model.path,
            })
            notification.success({ title: t('browser.overview.openapi_generated') })
            onWritten()
        } catch (e) {
            notification.error({ title: t('browser.overview.openapi_generate_failed'), description: errorMessage(e) })
        } finally {
            setRunning(false)
        }
    }

    return { running, writeSchema, generateTables }
}

/** What the generation will write, one line per module, saying which workbook it replaces. */
const GenerationPlan = ({ plan }: { plan: OpenApiGenerationPlan }) => {
    const { t } = useTranslation('repository')
    const line = (module: OpenApiModule, labelKey: string) => (
        <li>
            {t(labelKey)}
            {': '}
            <b>{module.name}</b>
            {' — '}
            {t(module.declared ? 'browser.overview.openapi_plan_replaces' : 'browser.overview.openapi_plan_adds',
                { path: module.path })}
        </li>
    )
    return (
        <>
            <div>{t('browser.overview.openapi_generate_confirm')}</div>
            <ul data-testid="openapi-generation-plan">
                {line(plan.algorithm, 'browser.overview.openapi_algorithm')}
                {line(plan.model, 'browser.overview.openapi_model')}
            </ul>
        </>
    )
}

/** The two actions, as the section heading offers them. */
export const OpenApiActions = ({ openapi, running, onWrite, onGenerate }: {
    openapi: DescriptorOpenApi | undefined
    running: boolean
    onWrite: () => void
    onGenerate: () => void
}) => {
    const { t } = useTranslation('repository')
    // Generating tables needs a specification to generate them from, and is what the mode asks for.
    const generates = openapi?.mode === 'GENERATION' && !!openapi.path
    return (
        <Space size="small">
            {generates
                ? (
                    <Tooltip title={t('browser.overview.openapi_generate_hint')}>
                        <Button
                            data-testid="openapi-generate"
                            icon={<TableOutlined />}
                            loading={running}
                            onClick={onGenerate}
                            size="small"
                        >
                            {t('browser.overview.openapi_generate')}
                        </Button>
                    </Tooltip>
                )
                : (
                    <Tooltip title={t('browser.overview.openapi_write_hint')}>
                        <Button
                            data-testid="openapi-write"
                            icon={<ApiOutlined />}
                            loading={running}
                            onClick={onWrite}
                            size="small"
                        >
                            {t('browser.overview.openapi_write')}
                        </Button>
                    </Tooltip>
                )}
        </Space>
    )
}
