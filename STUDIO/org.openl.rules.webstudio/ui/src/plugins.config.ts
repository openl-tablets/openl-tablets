export interface PluginConfiguration {
    name: string
    library: string
}

if (typeof window.pluginsConfiguration === 'undefined') {
    // define default plugins configuration
    // window.pluginsConfiguration = [
    //     {
    //         name: 'plugin',
    //         library: 'http://localhost:3101/plugin.js',
    //     },
    // ]
}
