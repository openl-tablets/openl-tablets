import React, { useState } from 'react';
import { Form, Input, Checkbox } from 'antd';


export const TypeLocal = () => {
    const [active, setActive] = useState(true);
    const [open, setOpen] = useState(true);

    return (
        <div>
            {active && (

                <Form
                    labelCol={{ span: 10 }}
                    wrapperCol={{ span: 18 }}
                    labelAlign="left">
                    <Form.Item label={
                        <span>
                            Local path * &nbsp;
                        </span>
                    }
                    >
                        <Input defaultValue="jdbc:h2:./openl-demo/repositories/deployment/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=20" />
                    </Form.Item>
                </Form>
            )}
        </div>
    );
}