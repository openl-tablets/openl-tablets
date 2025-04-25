import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { apiCall } from '../../services'

interface UserState {
    profile: {
        email?: string
        displayName?: string
        firstName?: string
        lastName?: string
        showHeader?: boolean
        showFormulas?: boolean
        testsPerPage?: number
        testsFailuresOnly?: boolean
        testsFailuresPerTest?: number
        showComplexResult?: boolean
        showRealNumbers?: boolean
        treeView?: string
        username?: string
        externalFlags?: {
            displayNameExternal: boolean
            firstNameExternal: boolean
            lastNameExternal: boolean
            emailExternal: boolean
            emailVerified: boolean
        },
        profiles?: {
            displayName: string
            description: string
            name: string
        }[],
        // Not provided by the API
        displayNameSelect?: string
        // Not provided by the API but mandatory for the API
        changePassword?: {
            currentPassword: string
            newPassword: string
            confirmPassword: string
        }
    }
    details: any
    status: 'idle' | 'loading' | 'succeeded' | 'failed'
    isLoggedIn: boolean
}

const initialState: UserState = {
    profile: {},
    details: {},
    status: 'idle',
    isLoggedIn: false,
}

export const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        logout: state => {
            state.profile = {}
            state.isLoggedIn = false
        }
    },
    extraReducers: builder => {
        builder
            .addCase(fetchUserProfile.pending, (state, action) => {
                state.status = 'loading'
            })
            .addCase(fetchUserProfile.fulfilled, (state, action) => {
                state.status = 'succeeded'
                state.profile = action.payload
                state.isLoggedIn = true
            })
            .addCase(fetchUserProfile.rejected, (state, action) => {
                state.status = 'failed'
                state.isLoggedIn = false
            })
            .addCase(updateUserProfile.pending, (state, action) => {
                state.status = 'loading'
            })
            .addCase(updateUserProfile.fulfilled, (state, action) => {
                state.status = 'succeeded'
                state.profile = { ...state.profile, ...action.payload }
            })
            .addCase(updateUserProfile.rejected, (state, action) => {
                state.status = 'failed'
            })
            .addCase(fetchUserDetails.pending, (state, action) => {
                state.status = 'loading'
            })
            .addCase(fetchUserDetails.fulfilled, (state, action) => {
                state.status = 'succeeded'
                state.details = action.payload
            })
            .addCase(fetchUserDetails.rejected, (state, action) => {
                state.status = 'failed'
            })
    }
})

export const fetchUserProfile = createAsyncThunk('user/fetchUserProfile', async () => {
    return await apiCall('/users/profile') as UserState['profile']
})

export const updateUserProfile = createAsyncThunk('user/updateUserProfile', async (data: UserState['profile']) => {
    await apiCall('/users/profile', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data)
    })

    return data
})

export const fetchUserDetails = createAsyncThunk('user/fetchUserDetails', async (username) => {
    return await apiCall(`/users/${username}`) as UserState['details']
})

export default userSlice.reducer