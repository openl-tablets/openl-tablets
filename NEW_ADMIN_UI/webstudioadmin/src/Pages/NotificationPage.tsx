import React, { useEffect, useState } from 'react';
import { Card, Input, Button } from 'antd';
import { AdminMenu } from '../components/AdminMenu';


const { TextArea } = Input;

export const NotificationPage: React.FC = () => {
    const [text, setText] = useState("");

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setText(e.target.value);
    };

    const handleRemove = () => {
        setText("");
    };

    return (
        <div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />

                <Card
                    bordered={true}
                    style={{
                        width: 300, margin: 20
                    }}
                >
                    <p><b>Notify all active users</b></p>
                    <p>Message:</p>
                    <TextArea onChange={handleChange} value={text}/>
                    <Button style={{ marginTop: 20, marginRight: 15 }}>Post</Button>
                    <Button onClick={handleRemove}>Remove</Button>
                </Card>
            </div>
        </div>
    )
};