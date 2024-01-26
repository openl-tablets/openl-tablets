import { AnyAction, configureStore, createAsyncThunk, ThunkDispatch } from '@reduxjs/toolkit'
import { notificationReducer } from 'containers/notification'
import { userReducer } from 'containers/user'

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

export const createAppAsyncThunk = createAsyncThunk.withTypes<{
    state: RootState
    dispatch: AppDispatch
    rejectValue: string
    extra: { s: string; n: number }
}>()

// Quick start guide for Redux v5.0
// @link https://redux.js.org/tutorials/quick-start

const store = configureStore({
    reducer: {
        notification: notificationReducer,
        user: userReducer
    }
})

export default store
