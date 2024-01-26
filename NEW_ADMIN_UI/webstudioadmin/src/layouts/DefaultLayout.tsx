import React from 'react'
import MainMenu from 'components/MainMenu'
import { Alert, Layout } from 'antd'
import { Header } from 'containers/Header'
import { Footer } from '../containers/Footer'
import { Outlet } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { RootState } from '../store'

const { Content: AntContent } = Layout

const layoutStyle: React.CSSProperties = {
    backgroundColor: '#fff',
    minHeight: '100vh',
}

export const DefaultLayout = () => {

    const notificationMessage = useSelector((state: RootState) => state.notification.notification)

    return (
        <Layout style={layoutStyle}>
            <Header />
            <AntContent>
                {notificationMessage && <Alert
                    banner
                    closable
                    message={notificationMessage}
                    type="error"
                />}
                <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'left' }}>
                    <MainMenu />
                    <div style={{ padding: 20, width: '100%' }}>
                        <Outlet />
                    </div>
                </div>
            </AntContent>
            <Footer />
        </Layout>
    )
}
