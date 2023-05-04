import { Button, Card, Col, Divider, Form, Grid, Input, Row, Table } from "antd";
import React, { useState } from "react";
import { AdminMenu } from "./AdminMenu";

export const NewGroup: React.FC = () => {
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [privileges, setPrivileges] = useState([]);
    const [selectionType, setSelectionType] = useState<'checkbox'>('checkbox');



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


    const dataSource = [
        {
            key: '1',
            privilege: 'View projects',
            administrators: '',
            analysts: '✓',
            deployers: '✓',
            developers: '✓',
            testers: '✓',
            viewers: '✓',
        },
        {
            key: '2',
            privilege: 'Create projects',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '✓',
            testers: '',
            viewers: '',
        },
        {
            key: '3',
            privilege: 'Edit projects',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '✓',
            testers: '',
            viewers: '',
        },
        {
            key: '4',
            privilege: 'Erase projects',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '✓',
            testers: '',
            viewers: '',
        },
        {
            key: '5',
            privilege: 'Delete projects',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '✓',
            testers: '',
            viewers: '',
        },
        {
            key: '6',
            privilege: 'Unlock projects',
            administrators: '',
            analysts: '',
            deployers: '',
            developers: '',
            testers: '',
            viewers: '',
        },
        {
            key: '7',
            privilege: 'Deploy projects',
            administrators: '',
            analysts: '',
            deployers: '✓',
            developers: '',
            testers: '',
            viewers: '',
        },
        {
            key: '8',
            privilege: 'Create deploy configuration',
            administrators: '',
            analysts: '',
            deployers: '✓',
            developers: '',
            testers: '',
            viewers: '',
        },
        {
            key: '9',
            privilege: 'Edit deploy configuration',
            administrators: '',
            analysts: '',
            deployers: '✓',
            developers: '',
            testers: '',
            viewers: '',
        },
        {
            key: '10',
            privilege: 'Delete deploy configuration',
            administrators: '',
            analysts: '',
            deployers: '✓',
            developers: '',
            testers: '',
            viewers: '',
        },
        {
            key: '11',
            privilege: 'Erase deploy configuration',
            administrators: '',
            analysts: '',
            deployers: '✓',
            developers: '',
            testers: '',
            viewers: '',
        },
        {
            key: '12',
            privilege: 'Unlock deploy configuration',
            administrators: '',
            analysts: '',
            deployers: '',
            developers: '',
            testers: '',
            viewers: '',
        }, {
            key: '13',
            privilege: 'Create tables',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '✓',
            testers: '',
            viewers: '',
        },
        {
            key: '14',
            privilege: 'Edit tables',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '✓',
            testers: '',
            viewers: '',
        },
        {
            key: '15',
            privilege: 'Remove tables',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '✓',
            testers: '',
            viewers: '',
        },
        {
            key: '16',
            privilege: 'Run tables',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '',
            testers: '✓',
            viewers: '',
        },
        {
            key: '17',
            privilege: 'Trace tables',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '',
            testers: '✓',
            viewers: '',
        },
        {
            key: '18',
            privilege: 'Benchamark tables',
            administrators: '',
            analysts: '✓',
            deployers: '',
            developers: '',
            testers: '✓',
            viewers: '',
        }, {
            key: '19',
            privilege: 'Administrate',
            administrators: '✓',
            analysts: '',
            deployers: '',
            developers: '',
            testers: '',
            viewers: '',
        },


    ];

    const columns = [
        {
            title: 'Privilege',
            dataIndex: 'privilege',
            key: 'privilege',
        },
        {
            title: 'Administrators',
            dataIndex: 'administrators',
            key: 'administrators',
        },
        {
            title: 'Analysts',
            dataIndex: 'analysts',
            key: 'analysts',
        },
        {
            title: 'Deployers',
            dataIndex: 'deployers',
            key: 'deployers',
        },
        {
            title: 'Developers',
            dataIndex: 'developers',
            key: 'developers',
        },
        {
            title: 'Testers',
            dataIndex: 'testers',
            key: 'testers',
        },
        {
            title: 'Viewers',
            dataIndex: 'viewers',
            key: 'viewers',
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
                        dataSource={dataSource} columns={columns} />
                    <Row style={{ float: "right" }}>
                        <Button onClick={handleSubmit} >Save</Button>
                    </Row>

                </Card></div>
        </div>
    )

};

