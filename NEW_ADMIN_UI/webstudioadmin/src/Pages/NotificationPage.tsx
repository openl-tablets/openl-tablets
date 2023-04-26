import React from 'react';
import { Card, Input, Button } from 'antd';
import { AdminMenu } from '../components/AdminMenu';


const { TextArea } = Input;

export function NotificationPage() {
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
                <TextArea />
                <Button style={{ marginTop: 20, marginRight: 15 }}>Post</Button>
                <Button>Remove</Button>
            </Card>
        </div>
    </div>
    )
}
