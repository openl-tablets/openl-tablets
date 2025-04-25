export interface PluginConfiguration {
    name: string
    library: string
}

if (typeof window.pluginsConfiguration === 'undefined') {
    // define default plugins configuration
    // window.pluginsConfiguration = [
    //     {
    //         name: 'claimEditorPlugin',
    //         library: 'http://localhost:3101/claim_editor_plugin.js',
    //     },
    // ]
}
