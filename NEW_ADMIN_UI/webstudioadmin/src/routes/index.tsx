import React from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { Notification } from 'containers/notification'
import { System } from 'containers/System'
import { Email } from 'containers/Email'
import { Tags } from 'containers/Tags'
import { RepoDesignPage } from 'pages/RepoDesignPage'
import { RepoDeploymentPage } from 'pages/RepoDeploymentPage'
import { RepoDeployConfPage } from 'pages/RepoDeployConfPage'
import { GroupsAndUsers } from '../containers/GroupsAndUsers'
import { UserPage } from 'pages/UserPage'
import { UserProfile } from 'containers/UserProfile'
import { UserSettings } from 'containers/UserSettings'
import { DefaultLayout } from '../layouts/DefaultLayout'
import { AdministrationLayout } from '../layouts/AdministrationLayout'
import { RedirectToDefaultPage } from './RedirectToDefaultPage'
import { HeaderLayout } from '../layouts/HeaderLayout'
import { claimsRoutes } from '../plugins'

const basePath = process.env.BASE_PATH || ''

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
                        path: 'admin/management/groups',
                        element: <GroupsAndUsers />,
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