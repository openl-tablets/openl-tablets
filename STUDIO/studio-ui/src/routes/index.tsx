import React from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { Notification } from 'containers/notification'
import { System } from 'containers/System'
import { Email } from 'containers/Email'
import { Tags } from 'containers/Tags'
import Tesseract from '../components/Tesseract'
import { Repositories } from '../containers/Repositories'
import { UserProfile } from 'containers/UserProfile'
import { UserSettings } from 'containers/UserSettings'
import { DefaultLayout } from '../layouts/DefaultLayout'
import { AdministrationLayout } from '../layouts/AdministrationLayout'
import { RedirectRoute } from './RedirectRoute'
import { Groups } from '../containers/Groups'
import { Users } from '../containers/Users'
import { Security } from '../containers/Security'
import { Help } from '../containers/Help'
import { CONFIG } from '../services'

const router = createBrowserRouter([
    {
        path: '/',
        element: <DefaultLayout />,
        children: [
            {
                path: 'faces/*', // To integrate with JSF Repository and Editor tabs
            },
            {
                path: 'administration',
                element: <AdministrationLayout />,
                children: [
                    {
                        index: true,
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
            {
                path: '/*',
                element: <Tesseract size={350} />
            }
        ],
    },
    {
        path: '/logout',
        element: <RedirectRoute to="logout" />,
    },
], {
    basename: CONFIG.CONTEXT,
})

export {
    router
}
