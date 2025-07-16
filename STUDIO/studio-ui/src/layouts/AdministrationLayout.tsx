import React from 'react'
import { Outlet } from 'react-router-dom'
import MainMenu from '../components/MainMenu'

export const AdministrationLayout = () => {
    return (
        <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'left' }}>
            <MainMenu />
            <div id="content" style={{ padding: 20, width: '100%' }}>
                <Outlet />
            </div>
        </div>
    )
}
