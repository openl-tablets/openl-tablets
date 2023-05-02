import React from 'react'
import { Button, Card, Checkbox, Divider, Input } from "antd";
import { AdminMenu } from "../components/AdminMenu";

export const CommonPage:React.FC = () => {
    
    return (
        <div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />

                <Card style={{ width: 450, margin: 20 }}>
                    <p><b>User Workspace</b></p>
                    <p>Workspace Directory:</p>
                    <Input defaultValue="./openl-demo/user-workspace" />
                    <Divider />
                    <p><b>History</b></p>
                    <p>The maximum count of saved changes for each project per user:</p>
                    <Input defaultValue="100" />
                    <Button style={{ marginTop: 20 }}>Clear all history</Button>
                    <Divider />
                    <p><b>Other</b></p>
                    <p>Update table properties ('createdOn', 'modifiedBy' etc.) on editing:</p>
                    <Checkbox
                    />
                    <p style={{marginTop: 15}}>Date Format:</p>
                    <Input defaultValue="MM/dd/yyyy" />
                    <p style={{marginTop: 15}}>Time Format:</p>
                    <Input defaultValue="hh:mm:ss a" />
                    <Divider />
                    <Button style={{ marginTop: 20 }}>Apply All and Restart</Button>
                </Card>
            </div>
        </div>
    )
};
