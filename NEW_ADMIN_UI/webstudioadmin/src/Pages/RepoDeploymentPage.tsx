import React, { useState } from "react";
import { AdminMenu } from "../components/AdminMenu";
import { RepositoryPage } from "./RepositoryPage";
import { Form, Input, Cascader, Checkbox, Card } from "antd";


export const RepoDeploymentPage: React.FC = () => {
    const [active, setActive] = useState(true);

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
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <RepositoryPage />
                {active && (
                    <Card bordered={true}
                        style={{
                            width: 500, margin: 20
                        }}>
                        <Form labelCol={{ span: 7 }}
                            wrapperCol={{ span: 18 }}>
                            <Form.Item
                                label={
                                    <span>
                                        Name &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Deployment" />
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
                                <Input defaultValue="jdbc:h2:./openl-demo/repositories/deployment/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=20" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Secure connection &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox onChange={() => setActive(!active)} />
                            </Form.Item>
                        </Form>
                    </Card>
                )}
                {!active && (
                    <Card bordered={true}
                        style={{
                            width: 500, margin: 20
                        }}>
                        <Form labelCol={{ span: 7 }}
                            wrapperCol={{ span: 18 }}>
                            <Form.Item
                                label={
                                    <span>
                                        Name &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Deployment" />
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
                                <Input defaultValue="jdbc:h2:./openl-demo/repositories/deployment/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=20" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Secure connection &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox checked onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Login &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="admin" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Password &nbsp;
                                    </span>
                                }
                            >
                                <Input.Password />
                            </Form.Item>
                        </Form>
                    </Card>

                )}
            </div>
        </div>
    )
};
