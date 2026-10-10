import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Modal, Typography } from 'antd'
import { applyProjectTableTheme } from '../../services/tables'

interface ApplyProjectThemeModalProps {
    open: boolean
    projectId: string
    /**
     * Whether a write left the project to be verified, so it is compiled as its workbooks stood before. The theme is
     * laid out by the tables as they were compiled, so it is not applied until then.
     */
    verifyNeeded?: boolean | undefined
    onClose: () => void
    /** Told once the theme is written, so the project is read back. */
    onApplied: () => void
}

/**
 * Writes the table theme into every table of the project that it styles, once the reader confirms it.
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
    const [applying, setApplying] = useState(false)

    const apply = async () => {
        setApplying(true)
        try {
            if (await applyProjectTableTheme(projectId) !== null) {
                onApplied()
                onClose()
            }
        } finally {
            setApplying(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={applying}
            okButtonProps={{ 'data-testid': 'apply-project-theme-ok', disabled: verifyNeeded }}
            okText={t('browser.module.apply_theme')}
            onCancel={onClose}
            onOk={() => { void apply() }}
            open={open}
            title={t('browser.module.apply_theme_project_confirm')}
        >
            <Typography.Paragraph>{t('browser.module.apply_theme_project_body')}</Typography.Paragraph>
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
