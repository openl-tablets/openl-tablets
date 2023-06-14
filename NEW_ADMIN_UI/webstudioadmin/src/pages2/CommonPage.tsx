import React from 'react';
import { Button, Card, Checkbox, Divider, Input, Form } from 'antd';
import DefaultLayout from '../components/DefaultLayout';

export const CommonPage: React.FC = () => {

    return (
        <DefaultLayout>
            <Card style={{ width: 900, margin: 20 }}>
                <p><b>User Workspace</b></p>
                <Form labelCol={{ span: 7 }}
                    wrapperCol={{ span: 18 }}
                    labelAlign="left">
                    <Form.Item label="Workspace Directory:">
                        <Input defaultValue="./openl-demo/user-workspace" />
                    </Form.Item>
                </Form>
                <Divider />
                <p><b>History</b></p>
                <Form labelCol={{ span: 7 }}
                    wrapperCol={{ span: 18 }}
                    labelAlign="left"
                    labelWrap
                >
                    <Form.Item label="The maximum count of saved changes for each project per user:">
                        <Input defaultValue="100" style={{ marginTop: 7 }} />
                    </Form.Item>
                </Form>
                <Button>Clear all history</Button>
                <Divider />
                <p><b>Other</b></p>
                <Form labelCol={{ span: 7 }}
                    wrapperCol={{ span: 18 }}
                    labelAlign="left"
                    labelWrap>
                    <Form.Item label="Update table properties ('createdOn', 'modifiedBy' etc.) on editing:">
                        <Checkbox style={{ marginTop: 12 }} />
                    </Form.Item>
                    <Form.Item label="Date Format:">
                        <Input defaultValue="MM/dd/yyyy" />
                    </Form.Item>
                    <Form.Item label="Time Format:">
                        <Input defaultValue="hh:mm:ss a" />
                    </Form.Item>
                </Form>
                <Divider />
                <Button style={{ marginTop: 5, color: "green", borderColor: "green" }}>Apply All and Restart</Button>
            </Card>
        </DefaultLayout>
    )
};
