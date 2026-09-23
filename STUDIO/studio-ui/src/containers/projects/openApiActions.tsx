import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { App, Button, Space, Tooltip } from 'antd'
import { ApiOutlined, TableOutlined } from '@ant-design/icons'
import { errorMessage } from '../../utils/errorMessage'
import {
    generateOpenApiTables,
    getOpenApiGenerationPlan,
    type OpenApiGenerationPlan,
    type OpenApiTargets,
    writeOpenApiSchema,
} from '../../services/openapi'
import type { DescriptorOpenApi } from '../../services/rulesDescriptor'
import { OpenApiGenerationModal } from './OpenApiGenerationModal'

/**
 * The two things a project and its specification do to each other, which the legacy Editor offered from one
 * dialog: write the specification from the rules the project compiled, or generate the tables from a
 * specification somebody wrote first.
 *
 * <p>Both write into the project, so both are offered only to a reader who may write to it, and what they
 * wrote is read back by the caller rather than guessed at here.
 */
export const useOpenApiActions = (projectId: string, onWritten: () => void) => {
    const { t } = useTranslation('repository')
    const { notification } = App.useApp()
    const [running, setRunning] = useState(false)
    const [asked, setAsked] = useState<Asked | undefined>(undefined)

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
     * Asks what the generation would write before it writes it: where each module goes is the reader's to
     * settle, and a module the project already reads has its workbook replaced — which they are owed in
     * words before they say yes.
     */
    const generateTables = async (openapi: DescriptorOpenApi) => {
        setRunning(true)
        try {
            setAsked({
                openapi,
                plan: await getOpenApiGenerationPlan(
                    projectId, openapi.algorithmModuleName, openapi.modelModuleName),
            })
        } catch (e) {
            notification.error({ title: t('browser.overview.openapi_generate_failed'), description: errorMessage(e) })
        } finally {
            setRunning(false)
        }
    }

    const run = async ({ openapi, plan }: Asked, targets: OpenApiTargets) => {
        setRunning(true)
        try {
            await generateOpenApiTables(projectId, {
                path: openapi.path ?? '',
                algorithmModuleName: plan.algorithm.name,
                modelModuleName: plan.model.name,
                ...targets,
            })
            setAsked(undefined)
            notification.success({ title: t('browser.overview.openapi_generated') })
            onWritten()
        } catch (e) {
            notification.error({ title: t('browser.overview.openapi_generate_failed'), description: errorMessage(e) })
        } finally {
            setRunning(false)
        }
    }

    const generationDialog = asked && (
        <OpenApiGenerationModal
            busy={running}
            onCancel={() => setAsked(undefined)}
            onGenerate={targets => void run(asked, targets)}
            plan={asked.plan}
        />
    )

    return { running, writeSchema, generateTables, generationDialog }
}

/** A generation the reader was asked about: what it is generated from, and what it would write. */
interface Asked {
    openapi: DescriptorOpenApi
    plan: OpenApiGenerationPlan
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
