import * as path from 'path'

export interface PluginConfiguration {
    name: string
    routeRoot: string
    library: string
    rules?: string[]
}

console.log('window.pluginsConfiguration', window.pluginsConfiguration)

if (typeof window.pluginsConfiguration === 'undefined') {
    // define default plugins configuration
    window.pluginsConfiguration = [
        // {
        //     name: 'claimEditorPlugin',
        //     routeRoot: '/faces/pages/ui.xhtml',
        //     library: 'http://localhost:3101/claim_editor_plugin.js',
        // },
        {
            name: 'claimEditorPlugin',
            routeRoot: '/claim',
            library: 'http://localhost:3101/claim_editor_plugin.js',
        },
    ]
}
