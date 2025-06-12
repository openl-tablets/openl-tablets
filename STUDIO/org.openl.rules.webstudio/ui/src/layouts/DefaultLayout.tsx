import React, { useContext, useMemo } from 'react'
import { Alert, Layout } from 'antd'
import { Header } from 'containers/Header'
import { Outlet } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { RootState } from '../store'
import { useScript } from '../hooks'
import { SystemContext } from '../contexts'

const { Content: AntContent } = Layout

const layoutStyle: React.CSSProperties = {
    backgroundColor: '#fff',
}

export const DefaultLayout = () => {
    const { systemSettings } = useContext(SystemContext)
    useScript(systemSettings?.scripts)

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
    }, [notificationMessage])

    return (
        <Layout style={layoutStyle}>
            <Header />
            <AntContent>
                {Notify}
                <Outlet />
            </AntContent>
        </Layout>
    )
}
