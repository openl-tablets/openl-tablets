import { loadRemote } from '@module-federation/runtime'

const isDevMode = process.env.NODE_ENV === 'development'

loadRemote('claimEditorPlugin/i18n').catch((e) => {
    if (isDevMode) {
        console.error('Failed to load claimEditorPlugin/i18n', e)
    }
})

export const claimsRoutes = await loadRemote('claimEditorPlugin/routes').then((a: any) => {
    return a.default
}).catch((e) => {
    return []
})

export const claimMenu = await loadRemote('claimEditorPlugin/menu').then((a: any) => {
    return a.default || []
}).catch((e) => {
    if (isDevMode) {
        console.error('Failed to load claimEditorPlugin/menu', e)
    }
    return []
})