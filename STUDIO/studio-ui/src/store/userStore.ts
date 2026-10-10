import { create } from 'zustand'
import { apiCall } from '../services'
import { UserDetails, UserProfile } from '../types/user'

interface UserStore {
    userProfile?: UserProfile | undefined
    userDetails?: UserDetails | undefined
    loading: boolean
    error: unknown
    isLoggedIn: boolean
    fetchUserInfo: () => Promise<void>
    fetchUserProfile: () => Promise<void>
}

/**
 * Whether the reader asks to see the tables in the formatting of their Excel files (**Show Original Excel Formatting**
 * in My Settings), as the profile of the user says.
 */
export const excelFormattingOf = (state: UserStore): boolean => state.userProfile?.showExcelFormatting ?? false

export const useUserStore = create<UserStore>((set) => ({
    userProfile: undefined,
    userDetails: undefined,
    loading: false,
    error: null,
    isLoggedIn: false,
    fetchUserInfo: async () => {
        set({ loading: true, error: null })
        try {
            const userProfile = await apiCall('/users/profile')
            const userDetails = await apiCall(`/users/${userProfile.username}`)
            set({ userProfile, userDetails, isLoggedIn: true, loading: false })
        } catch (error) {
            set({ error, loading: false })
        }
    },
    fetchUserProfile: async () => {
        set({ loading: true, error: null })
        try {
            const userProfile = await apiCall('/users/profile')
            set({ userProfile, isLoggedIn: true, loading: false })
        } catch (error) {
            set({ error, isLoggedIn: false, loading: false })
        }
    }
}))
