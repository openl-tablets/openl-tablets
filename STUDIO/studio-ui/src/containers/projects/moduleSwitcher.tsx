import React from 'react'
import { TranslationOutlined } from '@ant-design/icons'
import type { ModuleInfo } from '../../services/modules'
import { localizationRoute, moduleRoute } from '../../services/projectId'

/** The key of the messages among the modules of the switcher: no module name holds a control character. */
export const LOCALIZATION_ITEM = '\u0000localization'

/**
 * What the module switcher of a project offers: its modules, and after them the messages of the project, which
 * every module shares.
 */
export const switcherItems = (modules: ModuleInfo[] | null, localizationLabel: string) => (modules === null
    ? null
    : [
        ...modules.map(declared => ({ key: declared.name, label: declared.name, search: declared.name })),
        {
            key: LOCALIZATION_ITEM,
            label: <span><TranslationOutlined /> {localizationLabel}</span>,
            search: localizationLabel,
            divided: true,
        },
    ])

/** Where the switcher leads for the item picked in it. */
export const switchedRoute = (projectId: string, picked: string): string =>
    (picked === LOCALIZATION_ITEM ? localizationRoute(projectId) : moduleRoute(projectId, picked))
