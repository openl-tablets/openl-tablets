import "@refinedev/antd/dist/reset.css";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import React from 'react';
import { NotificationPage } from "pages2/NotificationPage";
import { SystemPage } from 'pages2/SystemPage';
import { CommonPage } from 'pages2/CommonPage';
import { MailPage } from 'pages2/MailPage';
import { TagsPage } from 'pages2/TagsPage';
import { RepoDesignPage } from 'pages2/RepoDesignPage';
import { RepoDeploymentPage } from 'pages2/RepoDeploymentPage';
import { UserPage } from 'pages2/UserPage';
import { RepoDeployConfPage } from 'pages2/RepoDeployConfPage';
import { GroupPage } from 'pages2/GroupPage';

function App() {

    return (

        <BrowserRouter>
            <Routes>
                <Route path="/" element={<CommonPage />} />
                <Route path="/common" element={<CommonPage />} />
                <Route path="/system" element={<SystemPage />} />
                <Route path="/users" element={<UserPage />} />
                <Route path="/admin/management/groups" element={<GroupPage />} />
                <Route path="/notification" element={<NotificationPage />} />
                <Route path="/tags" element={<TagsPage />} />
                <Route path="/mail" element={<MailPage />} />
                <Route path='/repository/design' element={<RepoDesignPage />} />
                <Route path='/repository/config' element={<RepoDeployConfPage />} />
                <Route path='/repository/deployment' element={<RepoDeploymentPage />} />
            </Routes>
        </BrowserRouter>
    );
};

export default App;
