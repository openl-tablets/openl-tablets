import React, { FC } from 'react'
import { Col, Drawer, Menu, Row, Typography } from 'antd'
import './UserMenu.scss'
import { UserLogo } from '../../components/UserLogo'
import { LogoutOutlined, QuestionOutlined, SettingOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'

interface UserMenuProps {
    isOpen: boolean
    onClose: () => void
}

export const UserMenu: FC<UserMenuProps> = ({ isOpen, onClose }) => {

    const Title = (
        <Row align="middle">
            <Col style={{ marginRight: '10px' }}>
                <UserLogo />
            </Col>
            <Col>
                <div className="user-menu-title">
                    <div className="user-menu-title-username">
                        TODO: username
                    </div>
                    <div className="user-menu-title-email">
                        <Typography.Text type="secondary">
                            TODO: username@email.com
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
            <Menu>
                <Menu.Item key="1" icon={<UserOutlined />}>TODO: My Profile</Menu.Item>
                <Menu.Item key="2" icon={<SettingOutlined />}>TODO: My Settings</Menu.Item>
                <Menu.Divider />
                <Menu.Item key="3" icon={<ToolOutlined />}>TODO: Administration</Menu.Item>
                <Menu.Divider />
                <Menu.Item key="3" icon={<QuestionOutlined />}>TODO: Help</Menu.Item>
                <Menu.Item key="3" icon={<LogoutOutlined />}>TODO: Sing Out</Menu.Item>
            </Menu>
        </Drawer>
    )
}