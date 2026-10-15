import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Modal, Select, Typography } from 'antd'
import type { TableThemeOption } from 'types/tables'
import { FieldRow } from '../../components/FieldRow'
import { applyProjectTableTheme, getTableThemes } from '../../services/tables'
import { errorHandler } from '../../utils/errorHandling'

interface ApplyProjectThemeModalProps {
    open: boolean
    projectId: string
    /**
     * Whether a write left the project to be verified, so it is compiled as its workbooks stood before. A theme is laid
     * out by the tables as they were compiled, so none is applied until then.
     */
    verifyNeeded?: boolean | undefined
    onClose: () => void
    /** Told once the theme is written, so the project is read back. */
    onApplied: () => void
}

/**
 * The table themes OpenL Studio offers, in the order it offers them: the primary one first.
 *
 * Undefined until the list arrives. Empty when it cannot be read: the dialog then offers no theme to choose rather
 * than failing, and the reason is kept for support.
 */
const useTableThemes = (): TableThemeOption[] | undefined => {
    const [themes, setThemes] = useState<TableThemeOption[] | undefined>(undefined)
    useEffect(() => {
        let current = true
        getTableThemes()
            .then(found => {
                if (current) {
                    setThemes(found)
                }
            })
            .catch((error: unknown) => {
                if (current) {
                    setThemes([])
                }
                errorHandler.logError(error instanceof Error ? error : new Error(String(error)),
                    { message: 'The table themes cannot be read' })
            })
        return () => {
            current = false
        }
    }, [])
    return themes
}

/**
 * Writes a table theme into every table of the project that the themes style, the theme chosen here.
 *
 * <p>The theme Studio offers first, its primary theme, is chosen to begin with.
 *
 * <p>While the project waits to be verified, the dialog says so and applies nothing.
 */
export const ApplyProjectThemeModal = ({
    open,
    projectId,
    verifyNeeded = false,
    onClose,
    onApplied,
}: ApplyProjectThemeModalProps) => {
    const { t } = useTranslation('repository')
    const themes = useTableThemes()
    // What the reader picked. Until they pick, the dialog stands on the theme offered first, and closing it forgets
    // the pick.
    const [picked, setPicked] = useState<string | undefined>(undefined)
    const theme = picked ?? themes?.[0]?.id
    const [applying, setApplying] = useState(false)

    const close = () => {
        setPicked(undefined)
        onClose()
    }

    const apply = async () => {
        if (theme === undefined) {
            return
        }
        setApplying(true)
        try {
            if (await applyProjectTableTheme(projectId, theme) !== null) {
                onApplied()
                close()
            }
        } finally {
            setApplying(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={applying}
            okButtonProps={{ 'data-testid': 'apply-project-theme-ok', disabled: theme === undefined || verifyNeeded }}
            okText={t('browser.module.apply_theme')}
            onCancel={close}
            onOk={() => { void apply() }}
            open={open}
            title={t('browser.module.apply_theme_project_confirm')}
        >
            <Typography.Paragraph>{t('browser.module.apply_theme_project_body')}</Typography.Paragraph>
            <FieldRow required label={t('browser.module.apply_theme_project_theme')}>
                <Select
                    data-testid="apply-project-theme-select"
                    onChange={setPicked}
                    options={(themes ?? []).map(option => ({ value: option.id, label: option.name }))}
                    value={theme}
                />
            </FieldRow>
            {verifyNeeded && (
                <Alert
                    showIcon
                    data-testid="apply-project-theme-verify"
                    title={t('browser.module.theme_verify_first')}
                    type="warning"
                />
            )}
        </Modal>
    )
}

export default ApplyProjectThemeModal
