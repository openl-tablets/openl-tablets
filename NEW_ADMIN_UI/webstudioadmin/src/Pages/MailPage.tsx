import { Card, Button, Checkbox, Row } from "antd";
import React from "react";
import { HeaderMenu } from "../components/HeaderMenu";
import { AdminMenu } from "../components/AdminMenu";



export function MailPage() {
    return ( <div>
        <div>
        <HeaderMenu />
        </div>
        <div style={{display: "flex", flexDirection: "row"}}>
        <AdminMenu />
               
        <Card bodyStyle={{height: 100}}
            bordered={true}
            style={{
                width: 300, margin:20
            }}
        >
            <Row >
                <p><b>Email server configuration</b></p>
                <p>Enable email address verification: &nbsp;</p>
                <Checkbox
                        // onChange={onChange}
                />
            </Row>
            <Button style={{marginTop: 20, marginRight: 15}}>Apply All and Restart</Button>
        </Card>
        </div>           
        </div>
    )
}