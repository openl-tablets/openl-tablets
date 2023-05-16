import { Card, Button, Checkbox, Row, Form, Input } from 'antd';
import React, { useState } from 'react';
import DefaultLayout from '../components/DefaultLayout';

export const MailPage: React.FC = () => {
    const [active, setActive] = useState(true);

    return (
        <DefaultLayout>
                <Card bordered={true}
                    style={{
                        width: 300, margin: 20
                    }}
                >
                    <Row >
                        <p><b>Email server configuration</b></p>
                        <p>Enable email address verification: &nbsp;</p>
                        <Checkbox onChange={() => setActive(!active)} />
                    </Row>
                    {!active && (
                        <Form labelCol={{ span: 8 }}
                            wrapperCol={{ span: 16 }}
                            labelAlign="left">
                            <Form.Item
                                label={
                                    <span>
                                        URL &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Username &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="admin" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Password &nbsp;
                                    </span>
                                }
                            >
                                <Input.Password />
                            </Form.Item>
                        </Form>
                    )}
                    <Button style={{ marginTop: 15, marginRight: 15, color:"green", borderColor:"green" }}>Apply All and Restart</Button>
                </Card>
            </DefaultLayout>
    )
};
