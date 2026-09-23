import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { UndoOutlined } from '@ant-design/icons'
import { Input, Modal, Typography } from 'antd'
import { FieldError } from '../../components/FieldError'
import { FieldRow } from '../../components/FieldRow'
import { isWorkbookPath } from '../../utils/workbooks'
import type { OpenApiGenerationPlan, OpenApiModule, OpenApiTargets } from '../../services/openapi'
import { CompactField } from './CompactField'
import { normalizeProjectPath } from './projectPaths'

const LABEL_WIDTH = 150

/** What is wrong with a path the reader wrote, said in their language, or nothing when it will do. */
const faultOf = (t: (key: string) => string, path: string): string | undefined => {
    if (!path) {
        return t('browser.overview.openapi_path_required')
    }
    return isWorkbookPath(path) ? undefined : t('browser.overview.openapi_path_not_excel')
}

/**
 * One of the two modules: what it is called, what becomes of it, and the workbook it is written to.
 *
 * <p>Two separate things decide how it is drawn. Where the module goes is settled by the project when it
 * already reads one of that name — the generation writes where it reads, whatever path is asked — so the
 * workbook is stated rather than offered, as the Editor's Modules Settings stated it. What becomes of the
 * file standing there is a question of its own: a module can be declared at a workbook nobody wrote yet.
 */
const ModulePlan = ({ module, label, path, fault, onChange, testId }: {
    module: OpenApiModule
    label: string
    path: string
    /** The message the workbook is refused with, or nothing when it will do. */
    fault: string | undefined
    onChange: (path: string) => void
    testId: string
}) => {
    const { t } = useTranslation('repository')
    return (
        <div>
            <Typography.Paragraph type={module.overwrites ? 'warning' : 'secondary'}>
                {t(module.overwrites ? 'browser.overview.openapi_plan_replaces' : 'browser.overview.openapi_plan_adds')}
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
                        <CompactField
                            action={{
                                icon: <UndoOutlined />,
                                title: t('browser.overview.openapi_plan_reset'),
                                onClick: () => onChange(module.path),
                                'data-testid': `${testId}-path-reset`,
                            }}
                        >
                            <Input
                                data-testid={`${testId}-path`}
                                onChange={event => onChange(event.target.value)}
                                value={path}
                            />
                        </CompactField>
                    )}
                <FieldError message={fault ?? null} testId={`${testId}-path-error`} />
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
 *
 * <p>Mounted on the plan it is opened on and unmounted when it closes, so the workbooks it opens with are
 * always that plan's own.
 */
export const OpenApiGenerationModal = ({ plan, busy, onCancel, onGenerate }: {
    /** What the generation would write, as the server planned it. */
    plan: OpenApiGenerationPlan
    busy: boolean
    onCancel: () => void
    onGenerate: (targets: OpenApiTargets) => void
}) => {
    const { t } = useTranslation('repository')
    const [algorithm, setAlgorithm] = useState(plan.algorithm.path)
    const [model, setModel] = useState(plan.model.path)

    const overwrites = plan.algorithm.overwrites || plan.model.overwrites
    // What the dialog weighs is what it sends: a typed path is read the way every other dialog of this
    // screen reads one, so the workbook the reader is shown as settled is the one the generation is asked
    // for. A stated path is weighed too — a module declared at a file that is no workbook is refused as
    // well, and saying so here beats letting the reader click into that refusal.
    const targets: OpenApiTargets = {
        algorithmModulePath: normalizeProjectPath(algorithm),
        modelModulePath: normalizeProjectPath(model),
    }
    const algorithmFault = faultOf(t, targets.algorithmModulePath)
    const modelFault = faultOf(t, targets.modelModulePath)
    const sharesOneWorkbook = targets.algorithmModulePath.toLowerCase() === targets.modelModulePath.toLowerCase()
    const settled = !sharesOneWorkbook && algorithmFault === undefined && modelFault === undefined

    return (
        <Modal
            open
            confirmLoading={busy}
            okButtonProps={{ 'data-testid': 'openapi-generate-submit', danger: overwrites, disabled: !settled }}
            okText={t(overwrites ? 'browser.overview.openapi_generate_overwrite' : 'browser.overview.openapi_generate')}
            onCancel={onCancel}
            onOk={() => onGenerate(targets)}
            title={t('browser.overview.openapi_generate')}
        >
            <Typography.Paragraph>{t('browser.overview.openapi_generate_confirm')}</Typography.Paragraph>
            <ModulePlan
                fault={algorithmFault}
                label={t('browser.overview.openapi_algorithm')}
                module={plan.algorithm}
                onChange={setAlgorithm}
                path={algorithm}
                testId="openapi-plan-algorithm"
            />
            <ModulePlan
                fault={modelFault}
                label={t('browser.overview.openapi_model')}
                module={plan.model}
                onChange={setModel}
                path={model}
                testId="openapi-plan-model"
            />
            <FieldError
                message={sharesOneWorkbook ? t('browser.overview.openapi_path_same') : null}
                testId="openapi-plan-same-path"
            />
        </Modal>
    )
}
