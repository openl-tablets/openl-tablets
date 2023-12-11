import React from 'react'
import { Form, Input } from 'antd'

export function TypeAWSS3() {
    return (
        <div>
            <Form
                labelAlign="left"
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 20 }}
            >
                <Form.Item label={(<span>Service endpoint &nbsp;</span>)}>
                    <Input />
                </Form.Item>
                <Form.Item label={(<span>Bucket name * &nbsp;</span>)}>
                    <Input />
                </Form.Item>
                <Form.Item label={(<span>Region name * &nbsp;</span>)}>
                    <Input />
                </Form.Item>
                <Form.Item label={(<span>Access key &nbsp;</span>)}>
                    <Input />
                </Form.Item>
                <Form.Item label={(<span>Secret key &nbsp;</span>)}>
                    <Input />
                </Form.Item>
                <Form.Item label={(<span>Listener period (sec) &nbsp;</span>)}>
                    <Input defaultValue="10" />
                </Form.Item>
                <Form.Item label={(<span>SSE algorithm &nbsp;</span>)}>
                    <Input />
                </Form.Item>
            </Form>
        </div>
    )
}
