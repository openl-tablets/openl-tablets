import React, { useState } from 'react';
import { Form, Input, Checkbox} from 'antd';


export const TypeDatabaseJNDI = () => {
    const [active, setActive] = useState(true);
    const [open, setOpen] = useState(true);

    return (
        <div>
            {active && (

                <Form
                    labelCol={{ span: 8 }}
                    wrapperCol={{ span: 20 }}
                    labelAlign="left">

                    <Form.Item label={
                        <span>
                            URL* &nbsp;
                        </span>
                    }
                    >
                        <Input defaultValue="java:comp/env/jdbc/DB" />
                    </Form.Item>

                    <Form.Item
                        label={
                            <span>
                                Secure connection &nbsp;
                            </span>
                        }
                    >
                        <Checkbox onChange={() => setOpen(!open)} />
                    </Form.Item>


                    {!open && (
                        <Form
                            labelCol={{ span: 8 }}
                            wrapperCol={{ span: 20 }}
                            labelAlign="left">
                            <Form.Item label={
                                <span>
                                    Login &nbsp;
                                </span>
                            }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item label={
                                <span>
                                    Password &nbsp;
                                </span>
                            }
                            >
                                <Input />
                            </Form.Item>
                        </Form>
                    )}

                </Form>
            )}
        </div>
    );
}