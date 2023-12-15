import React from 'react'
import MainMenu from 'components/MainMenu'

function DefaultLayout({ children }: { children: React.ReactNode }) {
    return (
        <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'left' }}>
            <MainMenu />
            <div style={{ padding: 20, width: '100%' }}>
                {children}
            </div>
        </div>
    )
}
export default DefaultLayout
