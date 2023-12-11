import React from 'react'
import { Form, Input } from 'antd'

export function TypeAzure() {
    return (
        <div>
            <Form
                labelAlign="left"
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 20 }}
            >
                <Form.Item label={(<span>URL* &nbsp;</span>)}>
                    <Input />
                </Form.Item>
                <Form.Item label={(<span>Listener period (sec) &nbsp;</span>)}>
                    <Input defaultValue="10" />
                </Form.Item>

            </Form>
        </div>
    )
}
