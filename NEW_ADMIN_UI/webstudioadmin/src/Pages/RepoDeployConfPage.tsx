import React, { useState } from "react";
import { AdminMenu } from "../components/AdminMenu";
import { Card, Form, Cascader, Checkbox, Input } from "antd";
import { RepositoryPage } from "./RepositoryPage";
import { LocalPathModal } from "../components/LocalPathModal";
import { ProtectedBranchesModal } from "../components/ProtectedBranchesModal";
import { RemoteRepoModal } from "../components/RemoteRepoModal";
import { ModalBranch } from "../components/ModalBranch";


export const RepoDeployConfPage: React.FC = () => {

    const [active, setActive] = useState(true);
    const [hide, setHide] = useState(true);

    const options = [
        {
            value: "Design",
            label: "Design",
        }
    ]

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
    const onChange = () => [
        setActive(!active),

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
                        <Form labelCol={{ span: 10 }}
                            wrapperCol={{ span: 18 }}>
                            <Form.Item
                                label={
                                    <span>
                                        Use design repository &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox checked onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Repository &nbsp;
                                    </span>
                                }
                            >
                                <Cascader options={options} placeholder="Design" />
                            </Form.Item>
                        </Form>
                    </Card>
                )}
                {!active && hide && (
                    <Card bordered={true}
                        style={{
                            width: 500, margin: 20
                        }}>
                        <Form labelCol={{ span: 10 }}
                            wrapperCol={{ span: 18 }}>
                            <Form.Item
                                label={
                                    <span>
                                        Use design repository &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Name &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Design" />
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
                            <Form.Item tooltip={{ title: 'Details', icon: <RemoteRepoModal /> }}
                                label={
                                    <span>
                                        Remote repository &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox onChange={() => setHide(!hide)} />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <LocalPathModal /> }}
                                label={
                                    <span>
                                        Local path* &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="./openl-demo/repositories/design" />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <ProtectedBranchesModal /> }}
                                label={
                                    <span>
                                        Protected branches &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Path &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="DESIGN/rules/" />
                            </Form.Item>
                        </Form>
                    </Card>

                )}
                {!active && !hide && (
                    <Card bordered={true}
                        style={{
                            width: 500, margin: 20
                        }}>
                        <Form >
                            <Form.Item
                                label={
                                    <span>
                                        Use design repository &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Name &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Design" />
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
                            <Form.Item tooltip={{ title: 'Details', icon: <RemoteRepoModal /> }}
                                label={
                                    <span>
                                        Remote repository &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox checked onChange={() => setHide(!hide)} />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        URL: &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Login: &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="admin" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Password: &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <LocalPathModal /> }}
                                label={
                                    <span>
                                        Local path* &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="./openl-demo/repositories/design" />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <ModalBranch /> }}
                                label={
                                    <span>
                                        Branches &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <ProtectedBranchesModal /> }}
                                label={
                                    <span>
                                        Protected branches &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Changes check interval(sec) &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="10" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Connection timeout(sec) &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="60" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Path &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="DESIGN/rules/" />
                            </Form.Item>
                        </Form>
                    </Card>

                )}
            </div>
        </div>
    )
};
