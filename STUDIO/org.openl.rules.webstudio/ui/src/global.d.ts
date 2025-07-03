/// <reference types="react-scripts" />

import { PluginConfiguration } from './plugins.config'

declare global {
    interface Window {
        pluginsConfiguration: PluginConfiguration[]
        // Global runtime variables
        __APP_SOURCE_PATH__: string
        __APP_PUBLIC_PATH__: string
    }
}

window.pluginsConfiguration = window.pluginsConfiguration || []

export {}