import React, { useCallback } from 'react'
import { Modal, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'

/**
 * Detail passed from a legacy JSF page via the {@code openConfirmModal} event.
 *
 * A generic yes/no confirmation. The message and the optional title are already-localized
 * strings supplied by the caller — the JSF page resolves {@code #{msg[...]}} server-side.
 * The {@code onOK} callback runs the legacy action once the user confirms.
 */
export interface ConfirmModalDetail {
    title?: string
    message: string
    okButton?: string
    onOK?: () => void
}

/**
 * Generic confirmation dialog shared by the legacy JSF pages. It replaces the RichFaces
 * {@code confirmPopup} and the native {@code window.confirm} calls with one Ant Design modal
 * mounted once in {@link DefaultLayout}.
 *
 * A JSF page opens it by dispatching an {@code openConfirmModal} event whose detail carries the
 * text and the {@code onOK} callback. The dialog closes by re-dispatching the same event with a
 * {@code null} detail; {@code onOK} runs on confirm.
 */
export const ConfirmModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<ConfirmModalDetail>('openConfirmModal')

    const close = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openConfirmModal', { detail: null }))
    }, [])

    const handleOk = useCallback(() => {
        close()
        detail?.onOK?.()
    }, [detail, close])

    return (
        <Modal
            destroyOnHidden
            cancelText={t('common:btn.cancel')}
            okText={detail?.okButton ?? t('common:btn.ok')}
            onCancel={close}
            onOk={handleOk}
            open={!!detail?.message}
            title={detail?.title ?? t('common:confirm.title')}
        >
            <Typography.Paragraph>{detail?.message}</Typography.Paragraph>
        </Modal>
    )
}
