import React from "react";
import { HeaderMenu } from "../components/HeaderMenu";
import { AdminMenu } from "../components/AdminMenu";
import { RepositoryPage } from "./RepositoryPage";
import Icon from "@ant-design/icons";
import { Form, Input, Tooltip, Cascader, AutoComplete, Row, Col, Button, Checkbox, Card } from "antd";


export function RepoDesignPage() {

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
        }
    ]


    return (
        <div>

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
                            <Input placeholder="Please input your name" />
                        </Form.Item>

                        <Form.Item
                            label={
                                <span>
                                    Type &nbsp;
                                </span>
                            }
                        >
                            <Cascader options={typeOptions} placeholder="Git" />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Remote repository &nbsp;
                                </span>
                            }
                        >
                            <Checkbox />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Local path* &nbsp;
                                </span>
                            }
                        >
                            <Input placeholder="./openl-demo/repositories/design" />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Protected branches &nbsp;
                                </span>
                            }
                        >
                            <Input />
                        </Form.Item>
                        <p><b>New branch:</b></p>
                        <Form.Item
                            label={
                                <span>
                                    Default branch name &nbsp;
                                </span>
                            }
                        >
                            <Input placeholder="WebStudio/{project-name}/{username}/{current-date}" />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Branch name pattern &nbsp;
                                </span>
                            }
                        >
                            <Input />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Invalid branch name message hint: &nbsp;
                                </span>
                            }
                        >
                            <Input />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Customize comments &nbsp;
                                </span>
                            }
                        >
                            <Checkbox />
                        </Form.Item>
                        <p>Folder structure configuration. If flat structure is used, all projects are stored in a given folder. If non-flat structure is used, projects locations can be modified.</p>
                        <Form.Item
                            label={
                                <span>
                                    Flat folder structure: &nbsp;
                                </span>
                            }
                        >
                            <Checkbox />
                        </Form.Item>
                        <Form.Item
                            label={
                                <span>
                                    Path &nbsp;
                                </span>
                            }
                        >
                            <Input placeholder="DESIGN/rules/" />
                        </Form.Item>
                    </Form>
                </Card>

        </div>
    )
}