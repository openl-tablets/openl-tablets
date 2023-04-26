import { Card, Button, Divider } from "antd"
import React from "react"
import { useNavigate} from "react-router-dom";


export function RepositoryPage() {
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

    )
}
