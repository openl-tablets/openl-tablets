import React from 'react'
import store from 'store'
import { Provider } from 'react-redux'
import RootRoutes from './routes'
import { App as AntApp } from 'antd'

function App() {
    return (
        <Provider store={store}>
            <AntApp>
                <RootRoutes />
            </AntApp>
        </Provider>
    )
}

export default App
