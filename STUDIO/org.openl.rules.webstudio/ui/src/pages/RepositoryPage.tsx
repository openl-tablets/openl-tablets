import React, {useEffect, useState} from 'react'
import {useLocation, useNavigate} from 'react-router-dom'
import {Button, Menu, Row} from 'antd'

export const RepositoryPage: React.FC = () => {
    const location = useLocation()
    const navigate = useNavigate()
    const [selectedKeys, setSelectedKeys] = useState<string[]>([])

    useEffect(() => {
        setSelectedKeys([location.pathname])
    }, [location.pathname])

    return (
        <div>
            <Row style={{ marginBottom: 20, width: 900 }}>
                <Menu
                    mode="horizontal"
                    selectedKeys={selectedKeys}
                    onClick={({ key }) => {
                        setSelectedKeys([key])
                        navigate(key)
                    }}
                >
                    <Menu.Item key="/web/administration/repository/design">Design repositories</Menu.Item>
                    <Menu.Item key="/web/administration/repository/config">Deploy Configuration repository</Menu.Item>
                    <Menu.Item key="/web/administration/repository/deployment">Deployment repositories</Menu.Item>
                    <Menu.Item>
                        <Button style={{ color: 'green', borderColor: 'green' }}>Apply All and Restart</Button>
                    </Menu.Item>
                </Menu>
            </Row>
        </div>
    )
}
