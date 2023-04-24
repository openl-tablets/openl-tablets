import React from "react";
import { HeaderMenu } from "../components/HeaderMenu";
import { AdminMenu } from "../components/AdminMenu";
import { RepositoryPage } from "./RepositoryPage";
import Icon from "@ant-design/icons";
import { Form, Input, Tooltip, Cascader, AutoComplete, Row, Col, Button, Checkbox, Card } from "antd";


export function RepoDeploymentPage() {

    const typeOptions = [
        {
            value: "Database JDBC",
            label: "Database JDBC",
        },
        {
            value: "Database JNDI",
            label: "Database JNDI",
        },
        {
            value: "AWS S3",
            label: "AWS S3",
        },
        {
            value: "Azure Blob Storage",
            label: "Azure Blob Storage",
        },
        {
            value: "Git",
            label: "Git",
        },
        {
            value: "Local",
            label: "Local",
        }
    ]


    return (
        <div>
            <div>
                <HeaderMenu />
            </div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <RepositoryPage />

                <Card bordered={true}
                    style={{
                        width: 500, margin: 20
                    }}>
                    <Form >
                        <Form.Item
                            label={
                                <span>
                                    Name &nbsp;
                                </span>
                            }
                        >
                            <Input placeholder="Deployment" />
                        </Form.Item>

                        <Form.Item
                            label={
                                <span>
                                    Type &nbsp;
                                </span>
                            }
                        >
                            <Cascader options={typeOptions} placeholder="Database (JDBC)" />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    URL* &nbsp;
                                </span>
                            }
                        >
                            <Input placeholder="jdbc:h2:./openl-demo/repositories/deployment/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=20" />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Secure connection &nbsp;
                                </span>
                            }
                        >
                            <Checkbox />
                        </Form.Item>
                    </Form>
                </Card>
            </div>
        </div>
    )
}