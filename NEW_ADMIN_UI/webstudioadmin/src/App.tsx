import "@refinedev/antd/dist/reset.css";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import React from 'react';
import { NotificationPage } from "Pages/NotificationPage";
import { SystemPage } from 'Pages/SystemPage';
import { CommonPage } from 'Pages/CommonPage';
import { MailPage } from 'Pages/MailPage';
import { TagsPage } from 'Pages/TagsPage';
import { RepoDesignPage } from 'Pages/RepoDesignPage';
import { RepoDeploymentPage } from 'Pages/RepoDeploymentPage';
import { UserPage } from 'Pages/UserPage';
import { RepoDeployConfPage } from 'Pages/RepoDeployConfPage';
import { GroupPage } from 'Pages/GroupPage';
import { NewUser } from "./views/users/NewUser";
import { NewGroup } from "./views/groups/NewGroup";
// import DataProvider from "./components/DataContext";

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
                <Route path='/groups/create' element={<NewGroup />} />
                <Route path='/users/create' element={<NewUser addNewUser={function (newUser: any): void {
                    throw new Error("Function not implemented.");
                }} />} />
            </Routes>
        </BrowserRouter>
    );
};

export default App;
