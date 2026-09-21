import { useEffect } from 'react'
import { App, Modal, notification as staticNotification } from 'antd'

type AppApi = ReturnType<typeof App.useApp>

/** The pop-up instances of the mounted application, or nothing until it has mounted. */
let live: AppApi | undefined

/**
 * A stand-in for one pop-up API that always reaches the instance in force.
 *
 * Every property is looked up at the moment it is used, so a call made after the application mounted lands on
 * its instance, and a call made before that — or from a test that never mounts it — falls back to Ant Design's
 * static one. The fallback is resolved late too, so a test can still replace it.
 */
const delegate = <T extends object>(pick: (api: AppApi) => T, fallback: () => T): T =>
    new Proxy({} as T, {
        get: (_target, property) => Reflect.get(live ? pick(live) : fallback(), property),
    })

/** The dialogs both the static `Modal` and the application's instance offer. */
type ModalApi = Pick<typeof Modal, 'confirm' | 'info' | 'success' | 'error' | 'warning'>

/**
 * Notifications that follow the theme, for a module that cannot call a hook — a service or a store.
 *
 * Ant Design's static `notification` renders outside React and stays light on a dark page. This one delegates
 * to the instance of the application's Ant Design `App`, which paints the notice in the theme and appearance
 * the user picked. A component or a hook takes `notification` from `App.useApp()` instead.
 */
export const notification = delegate(api => api.notification, () => staticNotification)

/**
 * Confirmation and information dialogs that follow the theme, for a module that cannot call a hook.
 *
 * The static `Modal.confirm` renders outside React and ignores the theme; this one delegates to the dialogs
 * of the application's Ant Design `App`. A component or a hook takes `modal` from `App.useApp()` instead.
 */
export const modal = delegate<ModalApi>(api => api.modal, () => Modal)

/**
 * Hands the pop-up instances of the enclosing Ant Design `App` to {@link notification} and {@link modal}.
 *
 * Mounted once, directly inside the application's `App` provider. Renders nothing.
 */
export const PopupsBridge = () => {
    const api = App.useApp()
    useEffect(() => {
        live = api
        return () => {
            live = undefined
        }
    }, [api])
    return null
}
