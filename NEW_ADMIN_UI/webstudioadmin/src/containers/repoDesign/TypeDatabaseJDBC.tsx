import React, { useState } from 'react'
import { Form, Input, Checkbox } from 'antd'

export function TypeDatabaseJDBC() {
    const [ open, setOpen ] = useState(true)

    return (
        <div>
            <Form
                labelAlign="left"
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 20 }}
            >

                <Form.Item label={(<span>URL* &nbsp;</span>)}>
                    <Input defaultValue="jdbc:h2:./openl-demo/repositories/db/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=20" />
                </Form.Item>
                <Form.Item label={(<span>Secure connection &nbsp;</span>)}>
                    <Checkbox onChange={() => setOpen(!open)} />
                </Form.Item>
                {!open && (
                    <Form
                        labelAlign="left"
                        labelCol={{ span: 8 }}
                        wrapperCol={{ span: 20 }}
                    >
                        <Form.Item label={(<span>Login &nbsp;</span>)}>
                            <Input />
                        </Form.Item>
                        <Form.Item label={(<span>Password &nbsp;</span>)}>
                            <Input />
                        </Form.Item>
                    </Form>
                )}
            </Form>
        </div>
    )
}
