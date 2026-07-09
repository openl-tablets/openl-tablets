import { create } from 'zustand'
import { errorHandler } from '../utils/errorHandling'

interface AppStore {
    showLogin: boolean
    setShowLogin: (show: boolean) => void
    showForbidden: boolean
    setShowForbidden: (show: boolean) => void
    showNotFound: boolean
    setShowNotFound: (show: boolean) => void
    showServerError: boolean
    setShowServerError: (show: boolean) => void
    /** Number of unfinished operations holding the full-screen loading overlay open. */
    loaderCount: number
    showLoader: () => void
    hideLoader: () => void
}

export const useAppStore = create<AppStore>((set, get) => ({
    showLogin: false,
    setShowLogin: (show) => set({ showLogin: show }),
    showForbidden: false,
    setShowForbidden: (show) => set({ showForbidden: show }),
    showNotFound: false,
    setShowNotFound: (show) => set({ showNotFound: show }),
    showServerError: false,
    setShowServerError: (show) => set({ showServerError: show }),
    loaderCount: 0,
    showLoader: () => set((state) => ({ loaderCount: state.loaderCount + 1 })),
    hideLoader: () => {
        if (get().loaderCount === 0) {
            errorHandler.logError(new Error('hideLoader() without showLoader() is called.'))
            return
        }
        set((state) => ({ loaderCount: state.loaderCount - 1 }))
    },
}))
