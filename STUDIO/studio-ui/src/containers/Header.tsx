import React, { useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Avatar, Layout, Row, Col, Menu, MenuProps, Alert } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { useStyles } from './Header.styles'
import { UserMenu } from './header/UserMenu'
import { Link, useLocation } from 'react-router-dom'
import Logo from '../components/Logo'
import { hasDeploymentRepositories } from '../services/deployments'
import { SystemContext } from '../contexts'
import { useAppNavigate, useScript } from '../hooks'
import { useNotificationStore } from 'store'

type MenuItem = Required<MenuProps>['items'][number];

const { Header: AntHeader } = Layout

export const Header = () => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
    const [lastWsMessage, setLastWsMessage] = useState<string>('')
    const { systemSettings } = useContext(SystemContext)
    const { notification } = useNotificationStore()
    const appNavigate = useAppNavigate()
    const { pathname } = useLocation()
    useScript(systemSettings?.scripts)

    const onOpenUserMenu = useCallback(() => {
        setIsUserMenuOpen(true)
    }, [])

    const onCloseUserMenu = useCallback(() => {
        setIsUserMenuOpen(false)
    }, [])

    // Deployments are only worth a tab for a user who may read at least one deployment repository.
    const [showDeployments, setShowDeployments] = useState(false)
    useEffect(() => {
        let current = true
        void hasDeploymentRepositories().then(available => {
            if (current) {
                setShowDeployments(available)
            }
        })
        return () => {
            current = false
        }
    }, [])

    const menuItems: MenuItem[] = [
        {
            key: '/',
            label: t('common:menu.editor'),
        },
        {
            key: '/projects',
            label: t('common:menu.projects'),
        },
        ...(showDeployments ? [{
            key: '/deployments',
            label: t('common:menu.deployments'),
        }] : []),
    ]

    const Notify = useMemo(() => {
        // Show WebSocket message if available, otherwise show store notification
        const messageToShow = lastWsMessage || notification

        if (messageToShow) {
            return (<Alert
                key={messageToShow}
                banner
                title={messageToShow}
                type="error"
                closable={{
                    onClose: () => {
                        if (lastWsMessage) {
                            setLastWsMessage('')
                        }
                    },
                }}
            />)
        }
        return null
    }, [notification, lastWsMessage])

    const activeKeyFromPath = useMemo(() => {
        // Project pages (/projects/<id>) still belong to the Projects tab.
        if (pathname.startsWith('/projects')) {
            return '/projects'
        }
        if (pathname.startsWith('/deployments')) {
            return '/deployments'
        }
        // The legacy pages under faces/ are the Editor's own screens.
        if (pathname.startsWith('/faces/')) {
            return '/'
        }
        return pathname
    }, [pathname])

    return (
        <>
            <AntHeader className={styles.header}>
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
                                    {/* The Editor is a server-rendered page, so the title is a plain document link. */}
                                    <Link reloadDocument to="/">{t('common:openl_studio')}</Link>
                                </div>
                            </Col>
                        </Row>
                    </Col>
                    <Col span={10}>
                        <Menu
                            items={menuItems}
                            mode="horizontal"
                            onClick={({ key }) => appNavigate(key)}
                            selectedKeys={[activeKeyFromPath]}
                            style={{
                                flex: 1,
                                minWidth: 0,
                            }}
                        />
                    </Col>
                    <Col>
                        <Avatar icon={<UserOutlined />} onClick={onOpenUserMenu} />
                    </Col>
                </Row>
                <UserMenu isOpen={isUserMenuOpen} onClose={onCloseUserMenu} />
            </AntHeader>
            {Notify}
        </>
    )
}
