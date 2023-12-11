import React from 'react'
import { Form, Input } from 'antd'

export function TypeLocal() {
    return (
        <div>
            <Form
                labelAlign="left"
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 20 }}
            >
                <Form.Item label={(<span>Local path * &nbsp;</span>)}>
                    <Input defaultValue="jdbc:h2:./openl-demo/repositories/deployment/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=20" />
                </Form.Item>
            </Form>
        </div>
    )
}
