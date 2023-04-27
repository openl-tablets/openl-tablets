import React from "react";
import { AdminMenu } from "../components/AdminMenu";
import { RepositoryPage } from "./RepositoryPage";
import { Form, Input, Cascader, Checkbox, Card } from "antd";
import { RemoteRepoModal } from "../components/RemoteRepoModal";
import { LocalPathModal } from "../components/LocalPathModal";
import { ProtectedBranchesModal } from "../components/ProtectedBranchesModal";
import { DefaultBranchModal } from "../components/DefaultBranchModal";
import { BranchNamePatternModal } from "../components/BranchNamePatternModal";
import { InvalidBranchModal } from "../components/InvalidBranchModal";

export const RepoDesignPage:React.FC = () => {

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
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <RepositoryPage />

                <Card bordered={true}
                    style={{
                        width: 650, margin: 20
                    }}>
                    <Form labelCol={{ span: 10 }}
                        wrapperCol={{ span: 18 }}>
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
                        <Form.Item tooltip={{ title: 'Details', icon: <RemoteRepoModal />}}
                            label={
                                <span>
                                    Remote repository &nbsp;
                                </span>
                            }
                        >
                            <Checkbox/>
                        </Form.Item>
                        <Form.Item tooltip={{ title: 'Details', icon: <LocalPathModal />}}
                            label={
                                <span>
                                    Local path* &nbsp;
                                </span>
                            }
                        >
                            <Input placeholder="./openl-demo/repositories/design"  />
                        </Form.Item>
                        <Form.Item tooltip={{ title: 'Details', icon: <ProtectedBranchesModal />}}
                            label={
                                <span>
                                    Protected branches &nbsp;
                                </span>
                            }
                        >
                            <Input />
                        </Form.Item>
                        <p><b>New branch:</b></p>
                        <Form.Item tooltip={{ title: 'Details', icon: <DefaultBranchModal />}}
                            label={
                                <span>
                                    Default branch name &nbsp;
                                </span>
                            }
                        >
                            <Input placeholder="WebStudio/{project-name}/{username}/{current-date}" />
                        </Form.Item>
                        <Form.Item tooltip={{ title: 'Details', icon: <BranchNamePatternModal />}}
                            label={
                                <span>
                                    Branch name pattern &nbsp;
                                </span>
                            }
                        >
                            <Input />
                        </Form.Item>
                        <Form.Item tooltip={{ title: 'Details', icon: <InvalidBranchModal />}}
                            label={
                                <span>
                                    Invalid branch name message hint &nbsp;
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
                            <Checkbox/>
                        </Form.Item>
                        <p>Folder structure configuration. If flat structure is used, all projects are stored in a given folder. If non-flat structure is used, projects locations can be modified.</p>
                        <Form.Item
                            label={
                                <span>
                                    Flat folder structure: &nbsp;
                                </span>
                            }
                        >
                            <Checkbox/>
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
        </div>
    )
};

