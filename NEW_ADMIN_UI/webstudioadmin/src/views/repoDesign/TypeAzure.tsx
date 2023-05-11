import React, { useState } from 'react';
import { Form, Input} from 'antd';


export const TypeAzure = () => {
    const [active, setActive] = useState(true);

    return (
        <div>
            {active && (

                <Form
                    labelCol={{ span: 10 }}
                    wrapperCol={{ span: 18 }}
                    labelAlign="left">

                    <Form.Item label={
                        <span>
                            URL* &nbsp;
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
                    
                </Form>
            )}
        </div>
    );
}