import { Card, Button, Checkbox, Row, Divider, Input } from "antd"
import React from "react"
import { AdminMenu } from "../components/AdminMenu"


export function SystemPage() {
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
                <p><b>Core</b></p>
                <Row>
                    <p>Dispatching Validation: &nbsp;</p>
                    <Checkbox
                    // onChange={onChange}
                    />
                </Row>
                <Row>
                    <p>Verify on Edit: &nbsp;</p>
                    <Checkbox
                    // onChange={onChange}
                    />
                </Row>
                <Divider />
                <p><b>Testing</b></p>
                <p>Thread number for tests:</p>
                <Input placeholder="4" />
                <Divider />
                <p><b>WebStudio Settings</b></p>
                <p> <b>WARNING!</b> If you click this button, all settings will be restored to default values. All user defined values, such as repository settings, will be lost. Use this button only if you understand the consequences.
                </p>
                <Button danger style={{ marginTop: 20 }}>Restore Defaults and Restart</Button>
                <Divider />
                <Button style={{ marginTop: 20, marginRight: 15 }}>Apply All and Restart</Button>
            </Card>
        </div>
    </div>
    )
}