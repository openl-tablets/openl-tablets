import React from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { Notification } from 'containers/notification'
import { System } from 'containers/System'
import { Email } from 'containers/Email'
import { Tags } from 'containers/Tags'
import { Repositories } from '../containers/Repositories'
import { UserProfile } from 'containers/UserProfile'
import { UserSettings } from 'containers/UserSettings'
import { DefaultLayout } from '../layouts/DefaultLayout'
import { AdministrationLayout } from '../layouts/AdministrationLayout'
import { RedirectToDefaultPage } from './RedirectToDefaultPage'
import { HeaderLayout } from '../layouts/HeaderLayout'
import { RedirectRoute } from './RedirectRoute'
import { Groups } from '../containers/Groups'
import { Users } from '../containers/Users'
import { Security } from '../containers/Security'
import { Help } from '../containers/Help'
import { claimsRoutes } from '../plugins'
import CONFIG from '../services/config'

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
                        path: 'admin/management/users',
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
                        path: 'repositories/:repositoryTab',
                        element: <Repositories />,
                    },
                    {
                        path: 'security',
                        element: <Security />
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
            {
                path: 'help',
                element: <Help />,
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
    basename: CONFIG.CONTEXT,
})

export {
    router
}