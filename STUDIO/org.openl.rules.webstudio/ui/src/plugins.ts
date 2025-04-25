import { loadRemote } from '@module-federation/runtime'

loadRemote('claimEditorPlugin/i18n').catch((e) => console.error('Failed to load claimEditorPlugin/i18n', e))

export const claimsRoutes = await loadRemote('claimEditorPlugin/routes').then((a: any) => {
    return a.default
}).catch((e) => {
    return []
})

export const claimMenu = await loadRemote('claimEditorPlugin/menu').then((a: any) => {
    return a.default || []
}).catch((e) => {
    console.error('Failed to load claimEditorPlugin/menu', e)
    return []
})