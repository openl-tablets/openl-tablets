/// <reference types="react-scripts" />

import { PluginConfiguration } from './plugins.config'

declare global {
    interface Window { pluginsConfiguration: PluginConfiguration[] }
}

window.pluginsConfiguration = window.pluginsConfiguration || []

export {}