import React from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { NotificationPage } from 'pages/NotificationPage'
import { SystemPage } from 'pages/SystemPage'
import { EmailPage } from '../pages/EmailPage'
import { TagsPage } from 'pages/TagsPage'
import { RepoDesignPage } from 'pages/RepoDesignPage'
import { RepoDeploymentPage } from 'pages/RepoDeploymentPage'
import { UsersPage } from '../pages/UsersPage'
import { RepoDeployConfPage } from 'pages/RepoDeployConfPage'
import { GroupsPage } from '../pages/GroupsPage'

const basePath = process.env.REACT_APP_BASE_PATH || ''

const RootRoutes: React.FC = () => {
    return (
        <BrowserRouter basename={basePath}>
            <Routes>
                <Route element={<SystemPage />} path="/index.xhtml" />
                <Route element={<SystemPage />} path="/" />
                <Route element={<SystemPage />} path="/system" />
                <Route element={<UsersPage />} path="/users" />
                <Route element={<GroupsPage />} path="/admin/management/groups" />
                <Route element={<NotificationPage />} path="/notification" />
                <Route element={<TagsPage />} path="/tags" />
                <Route element={<EmailPage />} path="/mail" />
                <Route element={<RepoDesignPage />} path="/repository/design" />
                <Route element={<RepoDeployConfPage />} path="/repository/config" />
                <Route element={<RepoDeploymentPage />} path="/repository/deployment" />
            </Routes>
        </BrowserRouter>
    )
}

export default RootRoutes