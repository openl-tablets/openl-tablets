import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { apiCall } from '../../services'

interface NotificationState {
    notification?: string
    status?: 'idle' | 'loading' | 'succeeded' | 'failed'
}

const initialState: NotificationState = {
    notification: '',
    status: 'idle',
}

export const notificationSlice = createSlice({
    name: 'notification',
    initialState,
    reducers: {},
    extraReducers: builder => {
        builder
            .addCase(fetchNotification.pending, (state, action) => {
                state.status = 'loading'
            })
            .addCase(fetchNotification.fulfilled, (state, action) => {
                state.status = 'succeeded'
                state.notification = action.payload
            })
            .addCase(fetchNotification.rejected, (state, action) => {
                state.status = 'failed'
            })
            .addCase(setNotification.pending, (state, action) => {
                state.status = 'loading'
            })
            .addCase(setNotification.fulfilled, (state, action) => {
                state.status = 'succeeded'
                state.notification = action.payload
            })
            .addCase(setNotification.rejected, (state, action) => {
                state.status = 'failed'
            })
    }
})

export const fetchNotification = createAsyncThunk('notification/fetchNotification', async () => {
    return await apiCall('/public/notification.txt')
})

export const setNotification = createAsyncThunk('notification/setNotification', async (notification: string) => {
    await apiCall('/admin/notification.txt', {
        method: 'POST',
        body: notification
    })

    return notification
})

export default notificationSlice.reducer