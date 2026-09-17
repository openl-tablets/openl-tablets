import type { PropsWithChildren } from 'react'

/** The pop-up objects a mocked `antd` may carry, whatever shape the test gave them. */
type PopupModules = Partial<Record<'message' | 'Modal' | 'notification', unknown>>

/**
 * Gives a mocked `antd` an `App` whose `useApp()` answers with the pop-up objects the module itself carries.
 *
 * A component takes `notification` and `modal` from `App.useApp()`, which is empty outside a real `<App>`.
 * Wrapping the mock — after its own `notification` or `Modal.confirm` stubs are in place — hands those very
 * stubs to the component, so the test asserts on them as before. The `App` is a pass-through, so a test may
 * still render `<App>` around the component.
 */
export const withStaticApp = <T extends object>(antd: T) => {
    const { message, Modal: modal, notification } = antd as PopupModules
    return {
        ...antd,
        App: Object.assign(
            ({ children }: PropsWithChildren) => children,
            { useApp: () => ({ message, modal, notification }) }
        ),
    }
}
