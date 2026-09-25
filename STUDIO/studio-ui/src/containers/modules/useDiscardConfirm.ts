import { useCallback } from 'react'
import { App } from 'antd'
import { useTranslation } from 'react-i18next'

/** Runs something that reads the table again, asking first where cells of it are not saved. */
export type ConfirmDiscard = (read: () => void) => void

/**
 * Asks before something reads the table again over cells the reader has written and not saved.
 *
 * <p>Switching a branch, opening a revision, restoring the project, refreshing the module — each of them
 * reads the workbook afresh, and what was written on screen and not yet saved is gone with it. The editor
 * asks this very question when the reader leaves the page, and it is the same question here: what is about
 * to be lost, and whether to lose it.
 *
 * <p>A table with nothing pending is read again without a word.
 */
export const useDiscardConfirm = (dirty: boolean): ConfirmDiscard => {
    const { modal } = App.useApp()
    const { t } = useTranslation('repository')
    return useCallback((read: () => void) => {
        if (!dirty) {
            read()
            return
        }
        modal.confirm({
            title: t('browser.module.edit_leaving'),
            content: t('browser.module.edit_reloading_message'),
            okText: t('browser.module.edit_discard'),
            cancelText: t('browser.module.edit_keep_editing'),
            onOk: read,
        })
    }, [dirty, t])
}
