import React from 'react'

// define list of default plugins
import './plugins.config'
// runtime import of remote plugins
import './initRemotePlugins'

import { createRoot } from 'react-dom/client'

import store from '../src/store'
import { Provider } from 'react-redux'

import reportWebVitals from './reportWebVitals'
import App from './App'

import './i18n_init'
import './i18n'

import './index.scss'

const container = document.getElementById('appRoot') as HTMLElement
const root = createRoot(container)

root.render(
    <React.StrictMode>
        <Provider store={store}>
            <App />
        </Provider>
    </React.StrictMode>
)

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()
