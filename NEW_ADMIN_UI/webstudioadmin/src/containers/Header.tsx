import React, { useState, CSSProperties, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Layout, Row, Col, Menu } from 'antd'
import './Header.scss'
import { UserMenu } from './header/UserMenu'
import { useNavigate } from 'react-router-dom'
import { UserLogo } from '../components/UserLogo'
import Logo from './header/Logo'

const { Header: AntHeader } = Layout

const titleStyle: CSSProperties = {
    fontSize: 20,
    fontFamily: 'Georgia, Verdana, Helvetica, Arial',
    color: '#384f81',
}

export const Header = () => {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const [ isUserMenuOpen, setIsUserMenuOpen ] = useState(false)

    const onOpenUserMenu = useCallback(() => {
        setIsUserMenuOpen(true)
    }, [])

    const onCloseUserMenu = useCallback(() => {
        setIsUserMenuOpen(false)
    }, [])

    return (
        <AntHeader>
            <Row justify="space-between" style={{ width: '100%' }}>
                <Col span={12}>
                    <Row align="middle">
                        <Col>
                            <div className="header__logo">
                                <Logo />
                            </div>
                        </Col>
                        <Col>
                            <div className="header__title" style={titleStyle}>
                                {t('common:openl_studio')}
                            </div>
                        </Col>
                    </Row>
                </Col>
                <Col span={6}>
                    <Menu
                        mode="horizontal"
                        onClick={({ key }) => {
                            navigate(key)
                        }}
                        style={{
                            flex: 1,
                            minWidth: 0,
                        }}
                    >
                        <Menu.Item key="/redirect/editor">{t('common:menu.editor')}</Menu.Item>
                        <Menu.Item key="/redirect/repository">{t('common:menu.repository')}</Menu.Item>
                    </Menu>
                </Col>
                <Col>
                    <UserLogo onClick={onOpenUserMenu} />
                </Col>
            </Row>
            <UserMenu isOpen={isUserMenuOpen} onClose={onCloseUserMenu} />
        </AntHeader>
    )
}