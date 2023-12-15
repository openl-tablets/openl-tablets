import React from 'react'
import { createRoot } from 'react-dom/client'

import reportWebVitals from './reportWebVitals'
import App from './App'

import './i18n_init'
import './i18n'

import './index.scss'

const container = document.getElementById('admin_ui_root') as HTMLElement
const root = createRoot(container)

root.render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
)

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()
