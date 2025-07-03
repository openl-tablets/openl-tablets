import React, { useState, CSSProperties, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Layout, Row, Col, Menu, MenuProps } from 'antd'
import './Header.scss'
import { UserMenu } from './header/UserMenu'
import { Link } from 'react-router-dom'
import { UserLogo } from '../components/UserLogo'
import Logo from './header/Logo'
import CONFIG from '../services/config'

type MenuItem = Required<MenuProps>['items'][number];

const { Header: AntHeader } = Layout

const EDITOR = 'EDITOR'

const titleStyle: CSSProperties = {
    fontSize: 20,
    fontFamily: 'Georgia, Verdana, Helvetica, Arial',
    color: '#384f81',
}

const basePath = process.env.BASE_PATH || ''

export const Header = () => {
    const { t } = useTranslation()
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
    const [activeKey, setActiveKey] = useState(window.location.pathname)

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

    const goTo = (key = '/') => {
        window.location.href = basePath + key
    }

    return (
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
                        activeKey={activeKey}
                        items={menuItems}
                        mode="horizontal"
                        onClick={({ key }) => goTo(key)}
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
    )
}