import React from 'react'
import MainMenu from 'components/MainMenu'

function DefaultLayout({ children }: { children: React.ReactNode }) {
    return (
        <div>
            <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'center' }}>
                <MainMenu key="admin-menu" />
                <div>
                    {children}
                </div>
            </div>
        </div>
    )
}
export default DefaultLayout
