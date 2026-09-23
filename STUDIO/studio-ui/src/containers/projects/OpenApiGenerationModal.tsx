import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { UndoOutlined } from '@ant-design/icons'
import { Button, Input, Modal, Space, Tooltip, Typography } from 'antd'
import { FieldRow } from '../../components/FieldRow'
import type { OpenApiGenerationPlan, OpenApiModule } from '../../services/openapi'
import { useSharedStyles } from './sharedStyles'

const LABEL_WIDTH = 150

/** What a generated module may be written to: a file the engine reads as a workbook. */
const WORKBOOK = /\.(xlsx|xls|xlsm)$/i

/** Where the generation writes its two modules, once the reader has settled it. */
export interface OpenApiTargets {
    algorithmModulePath: string
    modelModulePath: string
}

/** What is wrong with a path the reader may write, said in their language, or nothing when it will do. */
const faultOf = (t: (key: string) => string, path: string): string | undefined => {
    const value = path.trim()
    if (!value) {
        return t('browser.overview.openapi_path_required')
    }
    return WORKBOOK.test(value) ? undefined : t('browser.overview.openapi_path_not_excel')
}

/**
 * The workbook a generated module is written to, typed freely, with the path the plan proposed a click away.
 *
 * <p>The twin of {@link RepoFolderInput}: the whole path stands in one field, so what the field shows is
 * exactly where the module lands.
 */
const WorkbookPathInput = ({ value, onChange, onReset, id, name, 'data-testid': testId }: {
    value: string
    onChange: (value: string) => void
    onReset: () => void
    /** Set by {@link FieldRow} so its label points at the input. */
    id?: string
    /** Set by {@link FieldRow} from the field label. */
    name?: string
    'data-testid'?: string
}) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    return (
        <Space.Compact block className={shared.compactField}>
            <Input
                data-testid={testId}
                {...(id ? { id } : {})}
                {...(name ? { name } : {})}
                onChange={event => onChange(event.target.value)}
                value={value}
            />
            <Tooltip title={t('browser.overview.openapi_plan_reset')}>
                <Button
                    aria-label={t('browser.overview.openapi_plan_reset')}
                    data-testid={testId && `${testId}-reset`}
                    icon={<UndoOutlined />}
                    onClick={onReset}
                />
            </Tooltip>
        </Space.Compact>
    )
}

/** One of the two modules: what it is called, what becomes of it, and the workbook it is written to. */
const ModulePlan = ({ module, label, path, onChange, testId }: {
    module: OpenApiModule
    label: string
    path: string
    onChange: (path: string) => void
    testId: string
}) => {
    const { t } = useTranslation('repository')
    // A module the project already reads is written where it reads, whatever path is asked for it, so its
    // workbook is stated rather than offered — as the Editor's Modules Settings stated it.
    const fault = module.declared ? undefined : faultOf(t, path)
    return (
        <div>
            <Typography.Paragraph type={module.declared ? 'warning' : 'secondary'}>
                {t(module.declared ? 'browser.overview.openapi_plan_replaces' : 'browser.overview.openapi_plan_adds')}
            </Typography.Paragraph>
            <FieldRow label={label} labelWidth={LABEL_WIDTH}>
                <span data-testid={`${testId}-name`}>{module.name}</span>
            </FieldRow>
            <FieldRow
                alignTop
                label={t('browser.overview.openapi_plan_path')}
                labelWidth={LABEL_WIDTH}
                required={!module.declared}
            >
                {module.declared
                    ? <span data-testid={`${testId}-path`}>{module.path}</span>
                    : (
                        <WorkbookPathInput
                            data-testid={`${testId}-path`}
                            onChange={onChange}
                            onReset={() => onChange(module.path)}
                            value={path}
                        />
                    )}
                {fault !== undefined && <Typography.Text type="danger">{fault}</Typography.Text>}
            </FieldRow>
        </div>
    )
}

/**
 * What the generation will write, and where, asked before it writes anything.
 *
 * <p>A module the project does not read yet is the reader's to place: the workbook the plan proposed can be
 * written over, and put back with the reset beside it. A module the project already reads keeps its own
 * workbook and is written over wherever it stands, so the reader is told that in words — and the button
 * says so too — before anything of the project is replaced.
 */
export const OpenApiGenerationModal = ({ open, plan, busy, onCancel, onGenerate }: {
    open: boolean
    /** What the generation would write, as the server planned it; absent until it has answered. */
    plan: OpenApiGenerationPlan | undefined
    busy: boolean
    onCancel: () => void
    onGenerate: (targets: OpenApiTargets) => void
}) => {
    const { t } = useTranslation('repository')
    const [algorithm, setAlgorithm] = useState('')
    const [model, setModel] = useState('')

    // The plan proposes where each module goes, and that is what the dialog opens on.
    useEffect(() => {
        if (open && plan) {
            setAlgorithm(plan.algorithm.path)
            setModel(plan.model.path)
        }
    }, [open, plan])

    if (!plan) {
        return null
    }

    const overwrites = plan.algorithm.declared || plan.model.declared
    // What the dialog weighs is what it sends. A typed path is read without the spaces around it, so the
    // workbook the reader is shown as settled is the one the generation is asked for.
    const targets = { algorithmModulePath: algorithm.trim(), modelModulePath: model.trim() }
    const sharesOneWorkbook = targets.algorithmModulePath.toLowerCase() === targets.modelModulePath.toLowerCase()
    // Only a path the reader may write is theirs to answer for: a declared module is written where it
    // reads, so whatever its workbook is called, there is nothing here to put right.
    const settled = !sharesOneWorkbook
            && (plan.algorithm.declared || faultOf(t, algorithm) === undefined)
            && (plan.model.declared || faultOf(t, model) === undefined)

    return (
        <Modal
            destroyOnHidden
            confirmLoading={busy}
            okButtonProps={{ 'data-testid': 'openapi-generate-submit', danger: overwrites, disabled: !settled }}
            okText={t(overwrites ? 'browser.overview.openapi_generate_overwrite' : 'browser.overview.openapi_generate')}
            onCancel={onCancel}
            onOk={() => onGenerate(targets)}
            open={open}
            title={t('browser.overview.openapi_generate')}
        >
            <Typography.Paragraph>{t('browser.overview.openapi_generate_confirm')}</Typography.Paragraph>
            <ModulePlan
                label={t('browser.overview.openapi_algorithm')}
                module={plan.algorithm}
                onChange={setAlgorithm}
                path={algorithm}
                testId="openapi-plan-algorithm"
            />
            <ModulePlan
                label={t('browser.overview.openapi_model')}
                module={plan.model}
                onChange={setModel}
                path={model}
                testId="openapi-plan-model"
            />
            {sharesOneWorkbook && (
                <Typography.Text data-testid="openapi-plan-same-path" type="danger">
                    {t('browser.overview.openapi_path_same')}
                </Typography.Text>
            )}
        </Modal>
    )
}
