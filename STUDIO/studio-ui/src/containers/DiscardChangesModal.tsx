import { Alert, Modal } from 'antd'
import { useTranslation } from 'react-i18next'

interface DiscardChangesModalProps {
    cancelButtonTestId: string
    confirmButtonTestId: string
    confirmText: string
    onCancel: () => void
    onConfirm: () => void
    open: boolean
    warning: string
}

export const DiscardChangesModal = ({
    cancelButtonTestId,
    confirmButtonTestId,
    confirmText,
    onCancel,
    onConfirm,
    open,
    warning,
}: DiscardChangesModalProps) => {
    const { t } = useTranslation('repository')

    return (
        <Modal
            destroyOnHidden
            cancelButtonProps={{ 'data-testid': cancelButtonTestId }}
            okButtonProps={{ danger: true, 'data-testid': confirmButtonTestId }}
            okText={confirmText}
            onCancel={onCancel}
            onOk={onConfirm}
            open={open}
            title={t('browser.close_discard_title')}
        >
            <Alert showIcon title={warning} type="warning" />
        </Modal>
    )
}
