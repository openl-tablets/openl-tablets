import { create } from 'zustand'
import { apiCall } from '../services'
import { UserDetails, UserProfile } from '../types/user'

interface UserStore {
    userProfile?: UserProfile
    userDetails?: UserDetails
    loading: boolean
    error: any | null
    isLoggedIn: boolean
    fetchUserInfo: () => Promise<void>
    fetchUserProfile: () => Promise<void>
}

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