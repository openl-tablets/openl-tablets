import { FC, lazy, Suspense, useContext, useState } from 'react'
import { Avatar, Drawer, Flex, Menu, type MenuProps, Typography } from 'antd'
import { useStyles } from './UserMenu.styles'
import {
    InfoCircleOutlined,
    KeyOutlined,
    LogoutOutlined,
    QuestionOutlined,
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
    const { appVersion, isPersonalAccessTokenEnabled, systemSettings } = useContext(SystemContext)
    // Undefined until About is chosen for the first time, so the dialog is not loaded before.
    const [isAboutOpen, setIsAboutOpen] = useState<boolean>()

    const Title = (
        <Flex align="center" gap="small">
            <Avatar icon={<UserOutlined />} />
            <div>
                <div>{userProfile?.username}</div>
                <Typography.Text style={{ fontWeight: 500 }} type="secondary">
                    {userProfile?.email}
                </Typography.Text>
            </div>
        </Flex>
    )

    const items: MenuProps['items'] = [
        { key: '/administration/user/profile', icon: <UserOutlined />, label: t('common:user_menu.my_profile') },
        ...(isPersonalAccessTokenEnabled ? [
            {
                key: '/administration/user/tokens',
                icon: <KeyOutlined />,
                label: t('common:user_menu.personal_access_tokens'),
            },
        ] : []),
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
                    <Flex justify="end">
                        <Typography.Text type="secondary">
                            {t('common:user_menu.version', { version: appVersion })}
                        </Typography.Text>
                    </Flex>
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
