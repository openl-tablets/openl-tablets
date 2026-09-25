import { useCallback } from 'react'
import { App } from 'antd'

/** Runs something, asking the reader first where there is anything to ask them. */
export type ConfirmBefore = (go: () => void) => void

/** What the reader is asked, where they are asked at all. */
export interface Question {
    title: string
    content: string
    okText: string
    cancelText?: string | undefined
}

/**
 * Runs something the reader may want to think about first, asking them where there is a question to put.
 *
 * <p>Every such question is the same shape — what is about to happen, and whether to go on with it — so it is
 * asked in one place. A caller with nothing to ask runs straight through and builds no dialog at all.
 */
export const useConfirmBefore = (asks: boolean, question: Question): ConfirmBefore => {
    const { modal } = App.useApp()
    const { title, content, okText, cancelText } = question
    return useCallback((go: () => void) => {
        if (!asks) {
            go()
            return
        }
        modal.confirm({ title, content, okText, cancelText, onOk: go })
    }, [asks, cancelText, content, modal, okText, title])
}

/** Runs something without asking: what a screen given no question of its own falls back to. */
export const GO_AHEAD: ConfirmBefore = go => go()
