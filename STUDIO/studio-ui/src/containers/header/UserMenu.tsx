import { FC, lazy, Suspense, useContext, useState } from 'react'
import { Avatar, Col, Drawer, Menu, type MenuProps, Row, Typography } from 'antd'
import { useStyles } from './UserMenu.styles'
import {
    InfoCircleOutlined,
    LogoutOutlined,
    QuestionOutlined,
    SettingOutlined,
    ToolOutlined,
    UserOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { PermissionContext, SystemContext } from '../../contexts'
import { useAppNavigate } from '../../hooks'
import { useUserStore } from 'store'

// A chunk of its own: neither the dialog nor the lists of licenses it reads are loaded before About is chosen.
const AboutModal = lazy(() => import('./AboutModal').then(module => ({ default: module.AboutModal })))

/** The key of the item that opens the About dialog; every other key is the address the item navigates to. */
const ABOUT = 'about'

interface UserMenuProps {
    isOpen: boolean
    onClose: () => void
}

export const UserMenu: FC<UserMenuProps> = ({ isOpen, onClose }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const appNavigate = useAppNavigate()
    const userProfile = useUserStore(state => state.userProfile)
    const { hasAdminPermission } = useContext(PermissionContext)
    const { appVersion, systemSettings } = useContext(SystemContext)
    // Undefined until About is chosen for the first time, so the dialog is not loaded before.
    const [isAboutOpen, setIsAboutOpen] = useState<boolean>()

    const Title = (
        <Row align="middle">
            <Col style={{ marginRight: '10px' }}>
                <Avatar icon={<UserOutlined />} />
            </Col>
            <Col>
                <div className="user-menu-title">
                    <div className="user-menu-title-username">
                        {userProfile?.username}
                    </div>
                    <div className="user-menu-title-email">
                        <Typography.Text style={{ fontWeight: 500 }} type="secondary">
                            {userProfile?.email}
                        </Typography.Text>
                    </div>
                </div>
            </Col>
        </Row>
    )

    const items: MenuProps['items'] = [
        { key: '/administration/user/profile', icon: <UserOutlined />, label: t('common:user_menu.my_profile') },
        { key: '/administration/user/settings', icon: <SettingOutlined />, label: t('common:user_menu.my_settings') },
        ...(hasAdminPermission() ? [
            { type: 'divider' as const },
            { key: '/administration/system', icon: <ToolOutlined />, label: t('common:user_menu.administration') },
        ] : []),
        { type: 'divider' },
        { key: '/help', icon: <QuestionOutlined />, label: t('common:user_menu.help') },
        { key: ABOUT, icon: <InfoCircleOutlined />, label: t('common:user_menu.about') },
        ...(systemSettings?.userMode ? [
            { key: '/logout', icon: <LogoutOutlined />, label: t('common:user_menu.sign_out') },
        ] : []),
    ]

    const onClick = ({ key }: { key: string }) => {
        if (key === ABOUT) {
            setIsAboutOpen(true)
        } else {
            appNavigate(key)
        }
        onClose()
    }

    return (
        <>
            <Drawer
                closable
                className={styles.drawer}
                onClose={onClose}
                open={isOpen}
                placement="right"
                title={Title}
                footer={(
                    <Row justify="end">
                        <Typography.Text type="secondary">
                            {t('common:user_menu.version', { version: appVersion })}
                        </Typography.Text>
                    </Row>
                )}
            >
                <Menu items={items} onClick={onClick} selectedKeys={[]} />
            </Drawer>
            {isAboutOpen !== undefined && (
                <Suspense>
                    <AboutModal onClose={() => setIsAboutOpen(false)} open={isAboutOpen} />
                </Suspense>
            )}
        </>
    )
}
