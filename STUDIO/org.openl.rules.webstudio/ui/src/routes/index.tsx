import React from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { Notification } from 'containers/notification'
import { System } from 'containers/System'
import { Email } from 'containers/Email'
import { Tags } from 'containers/Tags'
import { GroupsAndUsers } from '../containers/GroupsAndUsers'
import { Repositories } from '../containers/Repositories'
import { UserProfile } from 'containers/UserProfile'
import { UserSettings } from 'containers/UserSettings'
import { DefaultLayout } from '../layouts/DefaultLayout'
import { AdministrationLayout } from '../layouts/AdministrationLayout'
import { RedirectToDefaultPage } from './RedirectToDefaultPage'
import { HeaderLayout } from '../layouts/HeaderLayout'
import { claimsRoutes } from '../plugins'
import { RedirectRoute } from './RedirectRoute'

const basePath = process.env.BASE_PATH || ''

const router = createBrowserRouter([
    {
        path: '*',
        element: <HeaderLayout />,
    },
    {
        path: 'web',
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
                        path: 'repositories/:repositoryTab',
                        element: <Repositories />,
                    },
                    {
                        path: 'user',
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
        path: '/logout',
        element: <RedirectRoute to="logout" />,
    },
    {
        path: '/web/administration',
        element: <RedirectToDefaultPage />,
    },
], {
    basename: basePath,
})

export {
    router
}