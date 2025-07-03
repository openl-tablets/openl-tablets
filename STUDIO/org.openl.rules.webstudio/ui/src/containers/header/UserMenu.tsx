import React, { FC, useContext } from 'react'
import { Col, Drawer, Menu, Row, Typography } from 'antd'
import './UserMenu.scss'
import { UserLogo } from '../../components/UserLogo'
import { LogoutOutlined, QuestionOutlined, SettingOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useLocation } from 'react-router'
import { useTranslation } from 'react-i18next'
import { PermissionContext, SystemContext, UserContext } from '../../contexts'
import CONFIG from '../../services/config'

interface UserMenuProps {
    isOpen: boolean
    onClose: () => void
}

export const UserMenu: FC<UserMenuProps> = ({ isOpen, onClose }) => {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const { pathname } = useLocation()
    const { userProfile } = useContext(UserContext)
    const { hasAdminPermission } = useContext(PermissionContext)
    const { openlInfo } = useContext(SystemContext)

    const Title = (
        <Row align="middle">
            <Col style={{ marginRight: '10px' }}>
                <UserLogo />
            </Col>
            <Col>
                <div className="user-menu-title">
                    <div className="user-menu-title-username">
                        {userProfile.username}
                    </div>
                    <div className="user-menu-title-email">
                        <Typography.Text style={{ fontWeight: 500 }} type="secondary">
                            {userProfile.email}
                        </Typography.Text>
                    </div>
                </div>
            </Col>
        </Row>
    )

    const onClick = ({ key }: { key: string }) => {
        if (pathname.startsWith('/web/')) {
            navigate(key)
            onClose()
        } else {
            window.location.href = key
        }
    }

    return (
        <Drawer
            closable
            className="drawer-user-menu"
            onClose={onClose}
            open={isOpen}
            placement="right"
            title={Title}
            footer={(
                <Row justify="end">
                    {openlInfo && (
                        <Typography.Text type="secondary">
                            {t('common:user_menu.version', { version: openlInfo['openl.version'] })}
                        </Typography.Text>
                    )}
                </Row>
            )}
        >
            <Menu
                onClick={onClick}
                selectedKeys={[]}
            >
                <Menu.Item key={`${CONFIG.BASE_PATH}/administration/user/profile`} icon={<UserOutlined />}>{t('common:user_menu.my_profile')}</Menu.Item>
                <Menu.Item key={`${CONFIG.BASE_PATH}/administration/user/settings`} icon={<SettingOutlined />}>{t('common:user_menu.my_settings')}</Menu.Item>
                {hasAdminPermission() && (
                    <>
                        <Menu.Divider />
                        <Menu.Item key={`${CONFIG.BASE_PATH}/administration/system`} icon={<ToolOutlined />}>{t('common:user_menu.administration')}</Menu.Item>
                    </>
                )}
                <Menu.Divider />
                <Menu.Item key={`${CONFIG.BASE_PATH}/help`} icon={<QuestionOutlined />}>
                    {t('common:user_menu.help')}
                </Menu.Item>
                <Menu.Item key={`${CONFIG.BASE_PATH}/logout`} icon={<LogoutOutlined />}>
                    {t('common:user_menu.sign_out')}
                </Menu.Item>
            </Menu>
        </Drawer>
    )
}
