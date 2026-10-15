import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Modal, Typography } from 'antd'
import { formatProject } from '../../services/tables'

interface ReformatExcelFilesModalProps {
    open: boolean
    projectId: string
    onClose: () => void
    /** Told once the Excel files are formatted, so the project is read back. */
    onFormatted: () => void
}

/**
 * Asks before every table of every Excel file of the project is formatted with the OpenL table style.
 *
 * The project is compiled first when it needs it, so nothing holds the formatting back. A formatting that fails keeps
 * the dialog open, its reason already told, so the reader may try again.
 */
export const ReformatExcelFilesModal = ({ open, projectId, onClose, onFormatted }: ReformatExcelFilesModalProps) => {
    const { t } = useTranslation('repository')
    const [formatting, setFormatting] = useState(false)

    const reformat = async () => {
        setFormatting(true)
        try {
            if (await formatProject(projectId) !== null) {
                onFormatted()
                onClose()
            }
        } finally {
            setFormatting(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={formatting}
            okButtonProps={{ 'data-testid': 'reformat-ok' }}
            okText={t('browser.module.reformat_ok')}
            onCancel={onClose}
            onOk={() => { void reformat() }}
            open={open}
            title={t('browser.module.reformat_confirm')}
        >
            <Typography.Paragraph>{t('browser.module.reformat_body')}</Typography.Paragraph>
        </Modal>
    )
}

export default ReformatExcelFilesModal
