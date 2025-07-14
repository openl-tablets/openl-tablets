import React, { useMemo } from 'react'
import { Layout } from 'antd'
import { Header } from 'containers/Header'
import { Outlet } from 'react-router-dom'
import { useAppStore } from 'store'
import Forbidden from 'pages/403'
import NotFound from 'pages/404'
import ServerError from 'pages/500'

const { Content: AntContent } = Layout

const layoutStyle: React.CSSProperties = {
    backgroundColor: '#fff',
}

export const DefaultLayout = () => {
    const { showForbidden, showNotFound, showServerError } = useAppStore()

    const content = useMemo(() => {
        if (showForbidden) {
            return <Forbidden />
        }
        if (showNotFound) {
            return <NotFound />
        }
        if (showServerError) {
            return <ServerError />
        }
        return <Outlet />
    }, [showForbidden, showNotFound, showServerError])

    return (
        <Layout style={layoutStyle}>
            <Header />
            <AntContent>
                {content}
            </AntContent>
        </Layout>
    )
}
