import i18next from 'i18next'

i18next.addResourceBundle('en', 'localization', {
    title: 'Localization',
    all_messages: 'All Messages',
    default_language: 'Default',
    key: 'Key',
    load_failed: 'Failed to read the localization files',
    no_files: 'The project has no localization files yet',
    inherited_from: 'Inherited from {{language}}',
    missing: 'Not found: the rule returns the key itself',
    empty: 'Empty text',
    legacy_encoding: 'Not written in UTF-8: {{files}}. Such a file is read in Windows-1252 and saved in UTF-8.',
})
