import { Card, Button, Divider, Col, Row } from "antd"
import React from "react"
import { useNavigate } from "react-router-dom";


export const RepositoryPage: React.FC = () => {
    const navigate = useNavigate();
    const navigateDesign = () => {
        let path = `/repository/design`;
        navigate(path);
    }
    const navigateDeployConfig = () => {
        let path = `/repository/config`;
        navigate(path);
    }
    const navigateDeployment = () => {
        let path = `/repository/deployment`;
        navigate(path);
    }


    return (<div>

        <Card
            bordered={true}
            style={{
                width: 300, margin: 20
            }}

        >
            <p><b>Design repositories</b></p>
            <Col >
                <Row style={{
                    margin: 5
                }}><Button  type="dashed" onClick={navigateDesign}>Design</Button></Row>
                <Row style={{
                    margin: 5
                }}><Button onClick={navigateDesign}>Add repository</Button></Row>
            </Col>

            <Divider />
            <p><b>Deploy Configuration repository</b></p>
            <Button onClick={navigateDeployConfig}>Deploy configuration</Button>
            <Divider />
            <p><b>Deployment repositories</b></p>
            <Row style={{
                margin: 5
            }}><Button type="dashed" onClick={navigateDeployment}>Deployment</Button></Row>
            <Row style={{
                margin: 5
            }}><Button onClick={navigateDesign}>Add repository</Button></Row>
            <Divider />
            <Button>Apply All and Restart</Button>
        </Card>
    </div>

    )
};
