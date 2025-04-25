import React, { FC, useContext } from 'react'
import { Col, Drawer, Menu, Row, Typography } from 'antd'
import './UserMenu.scss'
import { UserLogo } from '../../components/UserLogo'
import { LogoutOutlined, QuestionOutlined, SettingOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { UserContext } from '../../contexts/User'
import {PermissionContext} from "../../contexts";

interface UserMenuProps {
    isOpen: boolean
    onClose: () => void
}

export const UserMenu: FC<UserMenuProps> = ({ isOpen, onClose }) => {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const { userProfile } = useContext(UserContext)
    const { hasAdminPermission } = useContext(PermissionContext)

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

    return (
        <Drawer
            closable
            className="drawer-user-menu"
            onClose={onClose}
            open={isOpen}
            placement="right"
            title={Title}
        >
            <Menu
                selectedKeys={[]}
                onClick={({ key }) => {
                    navigate(key)
                    onClose()
                }}
            >
                <Menu.Item key="/administration/user/profile" icon={<UserOutlined />}>{t('common:user_menu.my_profile')}</Menu.Item>
                <Menu.Item key="/administration/user/settings" icon={<SettingOutlined />}>{t('common:user_menu.my_settings')}</Menu.Item>
                {hasAdminPermission() && (
                    <>
                        <Menu.Divider />
                        <Menu.Item key="/administration/" icon={<ToolOutlined />}>{t('common:user_menu.administration')}</Menu.Item>
                    </>
                )}
                <Menu.Divider />
                <Menu.Item key="/help" icon={<QuestionOutlined />}>
                    TODO:
                    {' '}
                    {t('common:user_menu.help')}
                </Menu.Item>
                <Menu.Item key="/logout" icon={<LogoutOutlined />}>
                    {t('common:user_menu.sign_out')}
                </Menu.Item>
            </Menu>
        </Drawer>
    )
}