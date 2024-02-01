import React from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Notification } from 'containers/notification'
import { System } from 'containers/System'
import { Email } from 'containers/Email'
import { Tags } from 'containers/Tags'
import { RepoDesignPage } from 'pages/RepoDesignPage'
import { RepoDeploymentPage } from 'pages/RepoDeploymentPage'
import { Users } from 'containers/Users'
import { RepoDeployConfPage } from 'pages/RepoDeployConfPage'
import { Groups } from 'containers/Groups'
import { RedirectRoute } from './RedirectRoute'
import { NoMatch } from './NoMatch'
import { UserPage } from 'pages/UserPage'
import { UserProfile } from 'containers/UserProfile'
import { UserSettings } from 'containers/UserSettings'
import { DefaultLayout } from '../layouts/DefaultLayout'
// import { RedirectToDefaultPage } from './RedirectToDefaultPage'

const basePath = process.env.REACT_APP_BASE_PATH || ''

const RootRoutes: React.FC = () => {
    return (
        <BrowserRouter basename={basePath}>
            <Routes>
                <Route element={<DefaultLayout />} path="/">
                    <Route index element={<System />} />
                    <Route element={<System />} path="/index.xhtml" />
                    <Route element={<System />} path="/system" />
                    <Route element={<Users />} path="/users" />
                    <Route element={<Groups />} path="/admin/management/groups" />
                    <Route element={<Notification />} path="/notification" />
                    <Route element={<Tags />} path="/tags" />
                    <Route element={<Email />} path="/mail" />
                    <Route element={<RepoDesignPage />} path="/repository/design" />
                    <Route element={<RepoDeployConfPage />} path="/repository/config" />
                    <Route element={<RepoDeploymentPage />} path="/repository/deployment" />
                    <Route element={<UserPage />} path="/user">
                        <Route element={<UserProfile />} path="profile" />
                        <Route element={<UserSettings />} path="settings" />
                    </Route>
                    <Route element={<RedirectRoute />} path="/redirect/:page" />
                    <Route element={<NoMatch />} path="*" />
                </Route>
            </Routes>
        </BrowserRouter>
    )
}

export default RootRoutes