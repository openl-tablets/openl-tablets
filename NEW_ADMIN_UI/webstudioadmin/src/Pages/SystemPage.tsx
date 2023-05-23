import { Card, Button, Checkbox, Row, Divider, Input, Form } from 'antd';
import React from 'react';
import DefaultLayout from '../components/DefaultLayout';

export const SystemPage: React.FC = () => {
    return (
        <DefaultLayout>
            <Card
                bordered={true}
                style={{
                    width: 900, margin: 20
                }}
            >
                <p><b>Core</b></p>
                <Form
                    labelCol={{ span: 5 }}
                    wrapperCol={{ span: 18 }}
                    labelAlign="left"
                >
                    <Form.Item label="Dispatching Validation:">
                        <Checkbox
                        />
                    </Form.Item>
                    <Form.Item label="Verify on Edit:">
                        <Checkbox
                        />
                    </Form.Item>

                </Form>
                <Divider />
                <p><b>Testing</b></p>
                <Form>
                    <Form.Item label="Thread number for tests:">
                        <Input placeholder="4" />
                    </Form.Item>
                </Form>
                <Divider />
                <p><b>WebStudio Settings</b></p>
                <p> <b>WARNING!</b> If you click this button, all settings will be restored to default values. All user defined values, such as repository settings, will be lost. Use this button only if you understand the consequences.
                </p>
                <Button danger style={{ marginTop: 15 }}>Restore Defaults and Restart</Button>
                <Divider />
                <Button style={{ marginTop: 5, marginRight: 15, color: "green", borderColor: "green" }}>Apply All and Restart</Button>
            </Card>
        </DefaultLayout>
    )
};

