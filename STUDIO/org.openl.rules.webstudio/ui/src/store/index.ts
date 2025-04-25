import store, { RootState } from './store'
import { useAppDispatch, useAppSelector } from './hooks'

export default store

export {
    useAppDispatch,
    useAppSelector
}

export type {
    RootState
}