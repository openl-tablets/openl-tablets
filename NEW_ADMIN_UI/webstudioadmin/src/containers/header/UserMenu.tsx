import React, { FC } from 'react'
import { Col, Drawer, Menu, Row, Typography } from 'antd'
import './UserMenu.scss'
import { UserLogo } from '../../components/UserLogo'
import { LogoutOutlined, QuestionOutlined, SettingOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { RootState } from '../../store/store'
import { useSelector } from 'react-redux'

interface UserMenuProps {
    isOpen: boolean
    onClose: () => void
}

export const UserMenu: FC<UserMenuProps> = ({ isOpen, onClose }) => {
    const { t } = useTranslation()
    const navigate = useNavigate()

    const { username, email } = useSelector((state: RootState) => state.user.profile)

    const Title = (
        <Row align="middle">
            <Col style={{ marginRight: '10px' }}>
                <UserLogo />
            </Col>
            <Col>
                <div className="user-menu-title">
                    <div className="user-menu-title-username">
                        {username}
                    </div>
                    <div className="user-menu-title-email">
                        <Typography.Text style={{ fontWeight: 500 }} type="secondary">
                            {email}
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
                <Menu.Item key="/user/profile" icon={<UserOutlined />}>{t('common:user_menu.my_profile')}</Menu.Item>
                <Menu.Item key="/user/settings" icon={<SettingOutlined />}>{t('common:user_menu.my_settings')}</Menu.Item>
                <Menu.Divider />
                <Menu.Item key="/" icon={<ToolOutlined />}>{t('common:user_menu.administration')}</Menu.Item>
                <Menu.Divider />
                <Menu.Item key="3" icon={<QuestionOutlined />}>
                    TODO:
                    {' '}
                    {t('common:user_menu.help')}
                </Menu.Item>
                <Menu.Item key="3" icon={<LogoutOutlined />}>
                    TODO:
                    {' '}
                    {t('common:user_menu.sign_out')}
                </Menu.Item>
            </Menu>
        </Drawer>
    )
}