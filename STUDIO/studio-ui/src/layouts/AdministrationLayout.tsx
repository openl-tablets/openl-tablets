import React from 'react'
import { Outlet } from 'react-router-dom'
import MainMenu from '../components/MainMenu'
import { GroupsProvider } from '../contexts'

export const AdministrationLayout = () => {
    return (
        <GroupsProvider>
            <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'left' }}>
                <MainMenu />
                <div id="content" style={{ padding: 20, width: '100%' }}>
                    <Outlet />
                </div>
            </div>
        </GroupsProvider>
    )
}
