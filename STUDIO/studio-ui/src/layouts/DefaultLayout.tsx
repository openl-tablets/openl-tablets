import React from 'react'
import { Layout } from 'antd'
import { Header } from 'containers/Header'
import { Outlet } from 'react-router-dom'

const { Content: AntContent } = Layout

const layoutStyle: React.CSSProperties = {
    backgroundColor: '#fff',
}

export const DefaultLayout = () => {
    return (
        <Layout style={layoutStyle}>
            <Header />
            <AntContent>
                <Outlet />
            </AntContent>
        </Layout>
    )
}
