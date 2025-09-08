import React, { useState, CSSProperties, useCallback, useContext, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Layout, Row, Col, Menu, MenuProps, Alert } from 'antd'
import './Header.scss'
import { UserMenu } from './header/UserMenu'
import { Link } from 'react-router-dom'
import { UserLogo } from '../components/UserLogo'
import Logo from '../components/Logo'
import { CONFIG } from '../services'
import { SystemContext } from '../contexts'
import { useScript } from '../hooks'
import { useNotificationStore } from 'store'

type MenuItem = Required<MenuProps>['items'][number];

const { Header: AntHeader } = Layout

const titleStyle: CSSProperties = {
    fontSize: 20,
    fontFamily: 'Georgia, Verdana, Helvetica, Arial',
    color: '#384f81',
}

export const Header = () => {
    const { t } = useTranslation()
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
    const [activeKey, setActiveKey] = useState(window.location.pathname)
    const [lastWsMessage, setLastWsMessage] = useState<string>('')
    const { systemSettings } = useContext(SystemContext)
    const { notification } = useNotificationStore()
    useScript(systemSettings?.scripts)

    const onOpenUserMenu = useCallback(() => {
        setIsUserMenuOpen(true)
    }, [])

    const onCloseUserMenu = useCallback(() => {
        setIsUserMenuOpen(false)
    }, [])

    const menuItems: MenuItem[] = [
        {
            key: `${CONFIG.CONTEXT}/`,
            label: t('common:menu.editor'),
        },
        {
            key: `${CONFIG.CONTEXT}/faces/pages/modules/repository/index.xhtml`,
            label: t('common:menu.repository'),
        }
    ]

    const goTo = (key = `${CONFIG.CONTEXT}/`) => {
        window.location.href =  key
    }

    const Notify = useMemo(() => {
        // Show WebSocket message if available, otherwise show store notification
        const messageToShow = lastWsMessage || notification
        
        if (messageToShow) {
            return (<Alert
                key={messageToShow}
                banner
                closable
                message={messageToShow}
                type="error"
                onClose={() => {
                    if (lastWsMessage) {
                        setLastWsMessage('')
                    }
                }}
            />)
        }
        return null
    }, [notification, lastWsMessage])

    const activeKeyFromPath = useMemo(() => {
        return  window.location.pathname
    }, [])

    return (
        <>
            <AntHeader>
                <Row justify="space-between" style={{ width: '100%' }}>
                    <Col span={6}>
                        <Row align="middle">
                            <Col>
                                <div className="header-logo">
                                    <Logo />
                                </div>
                            </Col>
                            <Col>
                                <div className="header-title">
                                    <Link onClick={() => goTo()} style={titleStyle} to="">{t('common:openl_studio')}</Link>
                                </div>
                            </Col>
                        </Row>
                    </Col>
                    <Col span={10}>
                        <Menu
                            items={menuItems}
                            mode="horizontal"
                            onClick={({ key }) => goTo(key)}
                            selectedKeys={[activeKeyFromPath]}
                            style={{
                                flex: 1,
                                minWidth: 0,
                            }}
                        />
                    </Col>
                    <Col>
                        <UserLogo onClick={onOpenUserMenu} />
                    </Col>
                </Row>
                <UserMenu isOpen={isUserMenuOpen} onClose={onCloseUserMenu} />
            </AntHeader>
            {Notify}
        </>
    )
}
