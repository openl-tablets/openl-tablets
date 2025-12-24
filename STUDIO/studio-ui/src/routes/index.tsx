import React, { Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { Notification } from 'containers/Notification'
import { System } from 'containers/System'
import { Email } from 'containers/Email'
import { Tags } from 'containers/Tags'
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
import { EmailVerification } from '../containers/EmailVerification'
import { CONFIG } from '../services'
import Forbidden from '../pages/403'
import NotFound from '../pages/404'
import ServerError from '../pages/500'
import LoginPage from '../pages/LoginPage'
import Tesseract from '../components/Tesseract'
import { RouteErrorFallback } from '../components/RouteErrorFallback'

const PersonalAccessTokens = React.lazy(() => import('containers/PersonalAccessTokens').then(m => ({ default: m.PersonalAccessTokens })))

const router = createBrowserRouter([
    {
        path: '/',
        element: <DefaultLayout />,
        errorElement: <RouteErrorFallback />,
        children: [
            {
                path: 'faces/*', // To integrate with JSF Repository and Editor tabs
            },
            {
                path: 'help',
                element: <Help />,
            },
            {
                path: 'forbidden',
                element: <Forbidden />,
            },
            {
                path: 'not-found',
                element: <NotFound />,
            },
            {
                path: 'server-error',
                element: <ServerError />,
            },
            {
                path: 'administration',
                element: <AdministrationLayout />,
                errorElement: <RouteErrorFallback />,
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
                            {
                                path: 'tokens',
                                element: <Suspense fallback={null}><PersonalAccessTokens /></Suspense>,
                            },
                        ],
                    },
                ],
            },
            {
                path: 'email',
                element: <EmailVerification />,
            },
            {
                path: '*',
                element: <Tesseract size={400} />
            }
        ],
    },
    {
        path: '/login',
        element: <LoginPage />,
        errorElement: <RouteErrorFallback />,
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
