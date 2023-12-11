import React from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { NotificationPage } from 'pages/NotificationPage'
import { SystemPage } from 'pages/SystemPage'
import { MailPage } from 'pages/MailPage'
import { TagsPage } from 'pages/TagsPage'
import { RepoDesignPage } from 'pages/RepoDesignPage'
import { RepoDeploymentPage } from 'pages/RepoDeploymentPage'
import { UserPage } from 'pages/UserPage'
import { RepoDeployConfPage } from 'pages/RepoDeployConfPage'
import { GroupPage } from 'pages/GroupPage'

const RootRoutes: React.FC = () => {
    return (
        <BrowserRouter>
            <Routes>
                <Route element={<SystemPage />} path="/" />
                <Route element={<SystemPage />} path="/system" />
                <Route element={<UserPage />} path="/users" />
                <Route element={<GroupPage />} path="/admin/management/groups" />
                <Route element={<NotificationPage />} path="/notification" />
                <Route element={<TagsPage />} path="/tags" />
                <Route element={<MailPage />} path="/mail" />
                <Route element={<RepoDesignPage />} path="/repository/design" />
                <Route element={<RepoDeployConfPage />} path="/repository/config" />
                <Route element={<RepoDeploymentPage />} path="/repository/deployment" />
            </Routes>
        </BrowserRouter>
    )
}

export default RootRoutes