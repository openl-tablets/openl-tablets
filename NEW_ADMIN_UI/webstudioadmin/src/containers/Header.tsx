import React, { useState, CSSProperties, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Layout, Row, Col, Menu, MenuProps } from 'antd'
import './Header.scss'
import { UserMenu } from './header/UserMenu'
import { Link } from 'react-router-dom'
import { UserLogo } from '../components/UserLogo'
import Logo from './header/Logo'
import { claimMenu } from '../plugins'

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
    // const router = plugins.getRouter()
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
    const [activeKey, setActiveKey] = useState(window.location.pathname)

    const onOpenUserMenu = useCallback(() => {
        setIsUserMenuOpen(true)
    }, [])

    const onCloseUserMenu = useCallback(() => {
        setIsUserMenuOpen(false)
    }, [])

    const editorMenuItems = claimMenu.map((item: any) => {
        if (item.parent === EDITOR) {
            return {
                key: basePath + item.path,
                label: t(item.label),
            }
        }
    })

    const menuItems = () => {
        const defaultMenuItems: MenuProps['items'] = [
            {
                key: basePath + '/',
                label: t('common:menu.advanced_editor'),
            },
            {
                key: basePath + '/faces/pages/modules/repository/index.xhtml',
                label: t('common:menu.repository'),
            }
        ]

        const additionalMenuItems = editorMenuItems.length ? [{
            label: t('common:menu.editor'),
            children: [...editorMenuItems]
        }] : []

        return [
            ...defaultMenuItems,
            ...additionalMenuItems
        ]
    }

    return (
        <AntHeader>
            <Row justify="space-between" style={{ width: '100%' }}>
                <Col span={6}>
                    <Row align="middle">
                        <Col>
                            <div className="header__logo">
                                <Logo />
                            </div>
                        </Col>
                        <Col>
                            <div className="header__title">
                                <Link style={titleStyle} to="/" >{t('common:openl_studio')}</Link>
                            </div>
                        </Col>
                    </Row>
                </Col>
                <Col span={10}>
                    <Menu
                        activeKey={activeKey}
                        items={menuItems()}
                        mode="horizontal"
                        onClick={({ key }) => {
                            window.location = key
                        }}
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