import React from 'react'
import store from 'store'
import { Provider } from 'react-redux'
import RootRoutes from './routes'


function App() {
    return (
        <Provider store={store}>
            <RootRoutes />
        </Provider>
    )
}

export default App
