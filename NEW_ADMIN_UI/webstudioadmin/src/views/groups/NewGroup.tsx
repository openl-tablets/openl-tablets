import { Button, Card, Col, Form, Input, Row, Table } from 'antd';
import React, { useState } from 'react';
import { AdminMenu } from '../../components/AdminMenu';
import CreateGroupDataSource from './CreateGroupDataSource';

export const NewGroup: React.FC = () => {
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [privileges, setPrivileges] = useState([]);
    const [selectionType, setSelectionType] = useState<"checkbox">("checkbox");



    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault();
        const newGroup = {
            name,
            description,
            privileges,
        };
        setName("");
        setDescription("");
        setPrivileges([]);
    };

    const columns = [
        {
            title: "Privilege",
            dataIndex: "privilege",
            key: "privilege",
        },
        {
            title: "Administrators",
            dataIndex: "administrators",
            key: "administrators",
        },
        {
            title: "Analysts",
            dataIndex: "analysts",
            key: "analysts",
        },
        {
            title: "Deployers",
            dataIndex: "deployers",
            key: "deployers",
        },
        {
            title: "Developers",
            dataIndex: "developers",
            key: "developers",
        },
        {
            title: "Testers",
            dataIndex: "testers",
            key: "testers",
        },
        {
            title: "Viewers",
            dataIndex: "viewers",
            key: "viewers",
        },
    ];

    return (
        <div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <Card style={{ margin: 20, width: 1200 }} title="Create new group">
                    <Form layout="vertical" >
                        <Row justify="start">
                            <Col style={{ width: 200, margin: 5 }}>
                                <Form.Item label="Name">
                                    <Input id="userName" value={name} onChange={(e) => setName(e.target.value)} />
                                </Form.Item>
                            </Col>
                            <Col style={{ width: 500, margin: 5 }}>
                                <Form.Item label="Description">
                                    <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
                                </Form.Item>
                            </Col>
                        </Row>
                    </Form>
                    {/* <Divider /> */}
                    <Table rowSelection={{
                        type: selectionType,
                    }}
                        dataSource={CreateGroupDataSource} columns={columns} />
                    <Row style={{ float: "right" }}>
                        <Button onClick={handleSubmit} >Save</Button>
                    </Row>

                </Card></div>
        </div>
    )

};

