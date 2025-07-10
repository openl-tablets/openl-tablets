import { createContext }  from 'react'
import { UserDetails, UserProfile } from '../types/user'

export const UserContext = createContext({
    userProfile: {} as UserProfile,
    userDetails: {} as UserDetails,
    fetchUserProfile: () => {}
})
