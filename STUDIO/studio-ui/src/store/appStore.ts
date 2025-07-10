import { create } from 'zustand'

interface AppStore {
    showLogin: boolean
    setShowLogin: (show: boolean) => void
    showForbidden: boolean
    setShowForbidden: (show: boolean) => void
    showNotFound: boolean
    setShowNotFound: (show: boolean) => void
    showServerError: boolean
    setShowServerError: (show: boolean) => void
}

export const useAppStore = create<AppStore>((set) => ({
    showLogin: false,
    setShowLogin: (show) => set({ showLogin: show }),
    showForbidden: false,
    setShowForbidden: (show) => set({ showForbidden: show }),
    showNotFound: false,
    setShowNotFound: (show) => set({ showNotFound: show }),
    showServerError: false,
    setShowServerError: (show) => set({ showServerError: show }),
}))