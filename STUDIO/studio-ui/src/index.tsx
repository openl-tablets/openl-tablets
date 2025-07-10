import './setPublicPath'
import React from 'react'

import './i18n'
import './locales'

import { createRoot } from 'react-dom/client'

import reportWebVitals from './reportWebVitals'

import App from './App'

import './index.scss'

const container = document.getElementById('appRoot') as HTMLElement
const root = createRoot(container)

root.render(
    <App />
)

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()
