import React from 'react'
import ReactDOM from 'react-dom'
import i18next from 'i18next'
import * as reacti18next from 'react-i18next'
import { init } from '@module-federation/runtime'

/*
 * This is the entry point for the host application.
 * @see https://www.npmjs.com/package/@module-federation/runtime
 */

const remotes = window.pluginsConfiguration?.map((plugin: any) => {
    return {
        name: plugin.name,
        alias: plugin.name,
        entry: plugin.library,
    }
}) || []

const plugins = window.pluginsConfiguration?.map((plugin: any) => {
    return {
        name: plugin.name,
    }
}) || []

init({
    name: '',
    remotes,
    plugins,
    shared: {
        react: {
            version: '18.2.0',
            scope: 'default',
            lib: () => React,
            shareConfig: {
                singleton: true,
                requiredVersion: '^18.2.0',
            },
        },
        'react-dom': {
            version: '18.2.0',
            scope: 'default',
            lib: () => ReactDOM,
            shareConfig: {
                singleton: true,
                requiredVersion: '^18.2.0',
            },
        },
        i18next: {
            version: '23.7.10',
            scope: 'default',
            lib: () => i18next,
            shareConfig: {
                singleton: true,
                requiredVersion: '^23.7.10',
            },
        },
        'react-i18next': {
            version: '13.5.0',
            scope: 'default',
            lib: () => reacti18next,
            shareConfig: {
                singleton: true,
                requiredVersion: '^13.5.0',
            },
        }
    }
})

