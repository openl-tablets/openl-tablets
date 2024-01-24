import React from 'react'
import MainMenu from 'components/MainMenu'
import { Layout } from 'antd'
import { Header } from 'containers/Header'
import { Footer } from '../containers/Footer'

const { Content: AntContent } = Layout

const layoutStyle: React.CSSProperties = {
    backgroundColor: '#fff',
    minHeight: '100vh',
}

function DefaultLayout({ children }: { children: React.ReactNode }) {
    return (
        <Layout style={layoutStyle}>
            <Header />
            <AntContent>
                <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'left' }}>
                    <MainMenu />
                    <div style={{ padding: 20, width: '100%' }}>
                        {children}
                    </div>
                </div>
            </AntContent>
            <Footer />
        </Layout>
    )
}

export default DefaultLayout
