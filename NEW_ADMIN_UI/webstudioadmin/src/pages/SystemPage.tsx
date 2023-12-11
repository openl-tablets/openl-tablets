import { Card, Button, Checkbox, Divider, Input, Form } from 'antd'
import React from 'react'
import DefaultLayout from 'layouts/DefaultLayout'
import { WarningFilled } from '@ant-design/icons'

export const SystemPage: React.FC = () => (
    <DefaultLayout>
        <Card
            bordered
            style={{ width: 900, margin: 20 }}
        >
            <p>
                <b>Core</b>
            </p>
            <Form
                labelAlign="left"
                labelCol={{ span: 5 }}
                wrapperCol={{ span: 18 }}
            >
                <Form.Item label="Dispatching Validation:">
                    <Checkbox />
                </Form.Item>
                <Form.Item label="Verify on Edit:">
                    <Checkbox />
                </Form.Item>

            </Form>
            <Divider />
            <p>
                <b>Testing</b>
            </p>
            <Form>
                <Form.Item label="Thread number for tests:">
                    <Input placeholder="4" />
                </Form.Item>
            </Form>
            <Divider />
            <p>
                <b>WebStudio Settings</b>
            </p>
            <p>
                <WarningFilled style={{ color: 'red' }} />
                <b style={{ color: 'red' }}>WARNING!</b>
                {' '}
                If you click this button, all settings will be restored to default values. All user defined values, such as repository settings, will be lost. Use this button only if you understand the consequences.
            </p>
            <Button danger style={{ marginTop: 15 }}>Restore Defaults and Restart</Button>
        </Card>
        <Card style={{ width: 900, margin: 20 }}>
            <p>
                <b>User Workspace</b>
            </p>
            <Form
                labelAlign="left"
                labelCol={{ span: 7 }}
                wrapperCol={{ span: 18 }}
            >
                <Form.Item label="Workspace Directory:">
                    <Input defaultValue="./openl-demo/user-workspace" />
                </Form.Item>
            </Form>
            <Divider />
            <p>
                <b>History</b>
            </p>
            <Form
                labelWrap
                labelAlign="left"
                labelCol={{ span: 7 }}
                wrapperCol={{ span: 18 }}
            >
                <Form.Item label="The maximum count of saved changes for each project per user:">
                    <Input defaultValue="100" style={{ marginTop: 7 }} />
                </Form.Item>
            </Form>
            <Button>Clear all history</Button>
            <Divider />
            <p>
                <b>Other</b>
            </p>
            <Form
                labelWrap
                labelAlign="left"
                labelCol={{ span: 7 }}
                wrapperCol={{ span: 18 }}
            >
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
        </Card>
        <Divider />
        <Button style={{ marginTop: 5, color: 'green', borderColor: 'green' }}>Apply All and Restart</Button>
    </DefaultLayout>
)
