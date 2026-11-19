import i18next from 'i18next'

i18next.addResourceBundle('en', 'maven', {
    title: 'Maven repository',
    description: 'OpenL Studio downloads from this repository the libraries and the projects that rules.xml names by Maven coordinates, to compile a project and run its tests. Leave the URL empty to download nothing.',
    url: 'URL',
    url_invalid: 'Enter an http, https or file URL',
    username: 'Username',
    password: 'Password',
    saved: 'Maven repository configuration saved',
    load_failed: 'Cannot read the Maven repository configuration',
    save_failed: 'Cannot save the Maven repository configuration',
    confirm_apply: 'Are you sure you want to change the Maven repository?',
    confirm_apply_message: 'All users working with OpenL Studio now will lose all unsaved changes. Are you sure you want to change the Maven repository now?',
})
