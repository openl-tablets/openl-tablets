import { createContext } from 'react'

export const PermissionContext = createContext({
    hasAdminPermission: (): boolean => false,
})
