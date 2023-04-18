import {
    Refine,
    GitHubBanner,
    WelcomePage,
    Authenticated,
} from '@refinedev/core';
import { RefineKbar, RefineKbarProvider } from "@refinedev/kbar";

import {
    AuthPage, ErrorComponent
    , notificationProvider
    , ThemedLayout
} from '@refinedev/antd';
import "@refinedev/antd/dist/reset.css";

import { BrowserRouter, Route, Routes, Outlet } from "react-router-dom";
import routerBindings, { NavigateToResource, CatchAllNavigate, UnsavedChangesNotifier } from "@refinedev/react-router-v6";
import dataProvider from "@refinedev/simple-rest";
import { ColorModeContextProvider } from "./contexts/color-mode";
import React from 'react';
import { NotificationPage } from './Pages/NotificationPage';
import { SystemPage } from './Pages/SystemPage';
import { CommonPage } from './Pages/CommonPage';
import { MailPage } from './Pages/MailPage';
import { TagsPage } from './Pages/TagsPage';
import { HeaderMenu } from './components/HeaderMenu';
import { RepositoryPage } from './Pages/RepositoryPage';
import { RepoDesignPage } from './Pages/RepoDesignPage';
import { RepoDeploymentPage } from './Pages/RepoDeploymentPage';
import { UserPage } from './Pages/UserPage';
import { RepoDeployConfPage } from './Pages/RepoDeployConfPage';


function App() {


    return (
        <BrowserRouter>
                        <Routes>
                            {/* <Route index element={<WelcomePage />} /> */}
                            <Route index element={<HeaderMenu />} />
                            <Route path="/admin" element={<CommonPage />} />
                            <Route path="/admin/common" element={<CommonPage />} />
                            <Route path="/admin/repository" element={<RepositoryPage />} />
                            <Route path="/admin/system" element={<SystemPage />} />
                            <Route path="/admin/users" element={<UserPage />} />
                            {/* <Route path="/admin/groups" element={<GroupPage />} /> */}
                            <Route path="/admin/notification" element={<NotificationPage />} />
                            <Route path="/admin/tags" element={<TagsPage />} />
                            <Route path="/admin/mail" element={<MailPage />} />
                            <Route path='/admin/repository/design' element={<RepoDesignPage/>} />
                            <Route path='/admin/repository/config' element={<RepoDeployConfPage/>} />
                            <Route path='/admin/repository/deployment' element={<RepoDeploymentPage/>} />

                            </Routes>
        </BrowserRouter>
    );
};

export default App;
