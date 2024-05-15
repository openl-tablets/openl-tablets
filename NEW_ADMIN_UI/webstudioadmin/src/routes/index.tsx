import React from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { Notification } from 'containers/notification'
import { System } from 'containers/System'
import { Email } from 'containers/Email'
import { Tags } from 'containers/Tags'
import { RepoDesignPage } from 'pages/RepoDesignPage'
import { RepoDeploymentPage } from 'pages/RepoDeploymentPage'
import { Users } from 'containers/Users'
import { RepoDeployConfPage } from 'pages/RepoDeployConfPage'
import { Groups } from 'containers/Groups'
import { UserPage } from 'pages/UserPage'
import { UserProfile } from 'containers/UserProfile'
import { UserSettings } from 'containers/UserSettings'
import { DefaultLayout } from '../layouts/DefaultLayout'
import { AdministrationLayout } from '../layouts/AdministrationLayout'
import { RedirectToDefaultPage } from './RedirectToDefaultPage'
import { HeaderLayout } from '../layouts/HeaderLayout'
import { PluginLoader } from '../services/PluginLoader'
import { loadRemote } from '@module-federation/runtime'

const basePath = process.env.BASE_PATH || ''

const claimsRoutes = await loadRemote('claimEditorPlugin/routes').then((a: any) => {
    return a.default
}).catch((e) => {
    return []
})

const additionalRoutes = window.pluginsConfiguration?.map((plugin: any) => {
    return {
        path: plugin.routeRoot,
        element: <PluginLoader request="claimEditorPlugin/App" />,
    }
})


const router = createBrowserRouter([
    {
        path: '*',
        element: <HeaderLayout />,
    },
    {
        path: '/',
        element: <DefaultLayout />,
        children: [
            {
                path: 'administration',
                element: <AdministrationLayout />,
                children: [
                    {
                        index: true,
                        element: <System />,
                    },
                    {
                        path: 'index.xhtml',
                        element: <System />,
                    },
                    {
                        path: 'system',
                        element: <System />,
                    },
                    {
                        path: 'users',
                        element: <Users />,
                    },
                    {
                        path: 'admin/management/groups',
                        element: <Groups />,
                    },
                    {
                        path: 'notification',
                        element: <Notification />,
                    },
                    {
                        path: 'tags',
                        element: <Tags />,
                    },
                    {
                        path: 'mail',
                        element: <Email />,
                    },
                    {
                        path: 'repository/design',
                        element: <RepoDesignPage />,
                    },
                    {
                        path: 'repository/config',
                        element: <RepoDeployConfPage />,
                    },
                    {
                        path: 'repository/deployment',
                        element: <RepoDeploymentPage />,
                    },
                    {
                        path: 'user',
                        element: <UserPage />,
                        children: [
                            {
                                path: 'profile',
                                element: <UserProfile />,
                            },
                            {
                                path: 'settings',
                                element: <UserSettings />,
                            },
                        ],
                    },
                ],
            },
            ...claimsRoutes
        ],
    },
    {
        path: '/faces/pages/admin.xhtml',
        element: <RedirectToDefaultPage />,
    },
], {
    basename: basePath,
})

export {
    router
}