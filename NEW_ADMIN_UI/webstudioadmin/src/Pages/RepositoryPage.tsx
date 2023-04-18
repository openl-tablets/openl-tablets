import { Card, Button, Divider } from "antd"
import TextArea from "antd/es/input/TextArea"
import React from "react"
import { AdminMenu } from "../components/AdminMenu"
import { HeaderMenu } from "../components/HeaderMenu"
import { Link, useNavigate, Route, Routes } from "react-router-dom";


export function RepositoryPage() {
    const navigate = useNavigate();
    const navigateDesign = () => {
        let path = `/admin/repository/design`;
        navigate(path);
    }
    const navigateDeployConfig = () => {
        let path = `/admin/repository/config`;
        navigate(path);
    }
    const navigateDeployment = () => {
        let path = `/admin/repository/deployment`;
        navigate(path);
    }

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
                <p><b>Design repositories</b></p>
                <Button onClick={navigateDesign} >Design</Button>
                <Divider />
                <p><b>Deploy Configuration repository</b></p>
                <Button onClick={navigateDeployConfig}>Deploy configuration</Button>
                <Divider />
                <p><b>Deployment repositories</b></p>
                <Button onClick={navigateDeployment}>Deployment</Button>
                <Divider />
                <Button>Apply All and Restart</Button>
            </Card>
        </div>
    </div>
    )
}
