import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Button, Menu, Row } from 'antd';

export const RepositoryPage: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [selectedKeys, setSelectedKeys] = useState<string[]>([]);

    useEffect(() => {
        setSelectedKeys([location.pathname]);
    }, [location.pathname]);

    return (
        <div>
            <Row style={{ margin: 20, width: 1200 }} >
                <Menu
                    onClick={({ key }) => {
                        setSelectedKeys([key]);
                        navigate(key);
                    }}
                    selectedKeys={selectedKeys}
                    mode='horizontal'
                >
                    <Menu.Item key="/repository/design" >
                        Design repositories
                    </Menu.Item>
                    <Menu.Item key="/repository/config">
                        Deploy Configuration repository
                    </Menu.Item>
                    <Menu.Item key="/repository/deployment">
                        Deployment repositories
                    </Menu.Item>
                    <Menu.Item><Button style={{color:"green", borderColor:"green"}}>Apply All and Restart</Button></Menu.Item>
                </Menu>
            </Row>
        </div>
    );
};