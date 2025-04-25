import React from 'react'

// Uncomment the following line to enable the runtime import of remote plugins
// define list of default plugins
// import './plugins.config'
// runtime import of remote plugins
// import './initRemotePlugins'

import './i18n'
import './locales'

import { createRoot } from 'react-dom/client'

import store from '../src/store'
import { Provider } from 'react-redux'

import reportWebVitals from './reportWebVitals'

import App from './App'

import './index.scss'

const container = document.getElementById('appRoot') as HTMLElement
const root = createRoot(container)

root.render(
    <Provider store={store}>
        <App />
    </Provider>
)

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()
