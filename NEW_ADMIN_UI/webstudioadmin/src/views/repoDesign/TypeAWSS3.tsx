import React, { useState } from 'react';
import { Form, Input} from 'antd';

export const TypeAWSS3 = () => {
    const [active, setActive] = useState(true);

    return (
        <div>
            {active && (

                <Form
                    labelCol={{ span: 8 }}
                    wrapperCol={{ span: 20 }}
                    labelAlign="left">

                    <Form.Item label={
                        <span>
                            Service endpoint &nbsp;
                        </span>
                    }
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item label={
                        <span>
                            Bucket name * &nbsp;
                        </span>
                    }
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item label={
                        <span>
                            Region name * &nbsp;
                        </span>
                    }
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item label={
                        <span>
                            Access key &nbsp;
                        </span>
                    }
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item label={
                        <span>
                            Secret key &nbsp;
                        </span>
                    }
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item label={
                        <span>
                            Listener period (sec) &nbsp;
                        </span>
                    }
                    >
                        <Input defaultValue="10" />
                    </Form.Item>
                    <Form.Item label={
                        <span>
                            SSE algorithm &nbsp;
                        </span>
                    }
                    >
                        <Input />
                    </Form.Item>
                   
                </Form>
            )}
        </div>
    );
}