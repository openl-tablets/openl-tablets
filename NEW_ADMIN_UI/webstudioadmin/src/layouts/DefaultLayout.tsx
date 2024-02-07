import React, { useMemo } from 'react'
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

    const Notify = useMemo(() => {
        if (notificationMessage) {
            return (<Alert
                key={notificationMessage}
                banner
                closable
                message={notificationMessage}
                type="error"
            />)
        }
        return null
    }, [ notificationMessage ])

    return (
        <Layout style={layoutStyle}>
            <Header />
            <AntContent>
                {Notify}
                <Outlet />
            </AntContent>
            <Footer />
        </Layout>
    )
}
