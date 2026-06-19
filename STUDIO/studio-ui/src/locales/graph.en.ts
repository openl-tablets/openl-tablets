import i18next from 'i18next'

i18next.addResourceBundle('en', 'graph', {
    title: 'Table Dependencies',
    empty: 'No table dependencies to display.',
    load_failed: 'Failed to load the table dependency graph.',
    search_placeholder: 'Find a table by name',
    fit: 'Fit to screen',
    zoom_in: 'Zoom in',
    zoom_out: 'Zoom out',
    stats: '{{nodes}} tables · {{edges}} dependencies · {{cyclic}} in cycles · {{isolated}} isolated',
    panel: {
        open: 'Open in editor',
        dependencies: 'Dependencies',
        dependents: 'Used by',
        explore: 'Show only',
        explore_uses: 'what it uses',
        explore_used_by: 'who uses it',
        explore_both: 'both',
        back: 'Show full project',
        highlight_path: "Show one version's path",
        dispatcher_hint: 'Generated table that selects one version at runtime',
    },
})
