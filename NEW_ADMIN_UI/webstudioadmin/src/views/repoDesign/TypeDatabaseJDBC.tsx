import React, { useState } from 'react';
import { Form, Input, Checkbox } from 'antd';


export const TypeDatabaseJDBC = () => {
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
                        <Input defaultValue="jdbc:h2:./openl-demo/repositories/db/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=20" />
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