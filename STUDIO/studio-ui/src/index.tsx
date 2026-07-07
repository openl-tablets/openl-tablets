import React from 'react'

import { restoreNativeObjectValues } from './utils/prototypeJsCompat'

import './i18n'
import './locales'

// Undo Prototype.js global pollution before anything renders — see prototypeJsCompat.ts (EPBDS-16212).
restoreNativeObjectValues()

import { createRoot } from 'react-dom/client'

import App from './App'

const container = document.getElementById('appRoot') as HTMLElement
const root = createRoot(container)

root.render(
    <App />
)
