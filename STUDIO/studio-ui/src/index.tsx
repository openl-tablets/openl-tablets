import React from 'react'

import './i18n'
import './locales'

import { createRoot } from 'react-dom/client'

import App from './App'

import './index.scss'

const container = document.getElementById('appRoot') as HTMLElement
const root = createRoot(container)

root.render(
    <App />
)
