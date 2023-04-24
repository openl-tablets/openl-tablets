import React from 'react';
import { Card, Input, Button, Space, Grid, Col, Divider } from 'antd';
import { AdminMenu } from '../components/AdminMenu';
import { HeaderMenu } from '../components/HeaderMenu';


const { TextArea } = Input;

export function NotificationPage() {
    return (<div>
        <div>
            <HeaderMenu />
        </div>
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
