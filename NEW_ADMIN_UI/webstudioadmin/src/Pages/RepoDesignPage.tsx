import React, { useState } from "react";
import { AdminMenu } from "../components/AdminMenu";
import { RepositoryPage } from "./RepositoryPage";
import { Form, Input, Cascader, Checkbox, Card } from "antd";
import { RemoteRepoModal } from "../components/RemoteRepoModal";
import { LocalPathModal } from "../components/LocalPathModal";
import { ProtectedBranchesModal } from "../components/ProtectedBranchesModal";
import { DefaultBranchModal } from "../components/DefaultBranchModal";
import { BranchNamePatternModal } from "../components/BranchNamePatternModal";
import { InvalidBranchModal } from "../components/InvalidBranchModal";
import { MessageTemplateModal } from "../components/MessageTemplateModal";
import { InvalidUserMessageHintModal } from "../components/InvalidUserMessageHindModal";
import { SaveProjectModal } from "../components/SaveProjectModal";
import { UserMessagePaternModal } from "../components/UserMessagePatternModal";
import { CreateProjectModal } from "../components/CreateProjectModal";
import { ArchiveProjectModal } from "../components/ArchiveProjectModal";
import { RestoreProjectModal } from "../components/RestoreProjectModal";
import { EraseProjectModal } from "../components/EraseProjectModal";
import { CopyProjectModal } from "../components/CopyProjectModal";
import { RestoreFromOldVersionModal } from "../components/RestoreFromOldVersionModal";

export const RepoDesignPage: React.FC = () => {

    const [active, setActive] = useState(true);
    const [hide, setHide] = useState(true);


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
                {active && (
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
                                <Checkbox />
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
                            <p><b>New branch:</b></p>
                            <Form.Item tooltip={{ title: 'Details', icon: <DefaultBranchModal /> }}
                                label={
                                    <span>
                                        Default branch name &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="WebStudio/{project-name}/{username}/{current-date}" />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <BranchNamePatternModal /> }}
                                label={
                                    <span>
                                        Branch name pattern &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <InvalidBranchModal /> }}
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
                                <Checkbox onChange={() => setActive(!active)} />
                            </Form.Item>
                            <p>Folder structure configuration. If flat structure is used, all projects are stored in a given folder. If non-flat structure is used, projects locations can be modified.</p>
                            <Form.Item
                                label={
                                    <span>
                                        Flat folder structure: &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox onChange={() => setHide(!hide)} />
                            </Form.Item>
                            {!hide && (
                                <Form.Item
                                    label={
                                        <span>
                                            Path &nbsp;
                                        </span>
                                    }
                                >
                                    <Input defaultValue="DESIGN/rules/" />
                                </Form.Item>
                            )}
                        </Form>
                    </Card>
                )}


                {!active && (
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
                                <Checkbox />
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
                            <p><b>New branch:</b></p>
                            <Form.Item tooltip={{ title: 'Details', icon: <DefaultBranchModal /> }}
                                label={
                                    <span>
                                        Default branch name &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="WebStudio/{project-name}/{username}/{current-date}" />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <BranchNamePatternModal /> }}
                                label={
                                    <span>
                                        Branch name pattern &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <InvalidBranchModal /> }}
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
                                <Checkbox checked onChange={() => setActive(!active)} />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <MessageTemplateModal /> }}
                                label={
                                    <span>
                                        Message template &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="{user-message} Type: {commit-type}." />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <UserMessagePaternModal /> }}
                                label={
                                    <span>
                                        User message pattern &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <InvalidUserMessageHintModal /> }}
                                label={
                                    <span>
                                        Invalid user message hint &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Invalid comment: Comment doesn't match validation pattern" />
                            </Form.Item>
                            <p>User message templates:</p>
                            <Form.Item tooltip={{ title: 'Details', icon: <SaveProjectModal /> }}
                                label={
                                    <span>
                                        Save project &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Project {project-name} is saved." />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <CreateProjectModal /> }}
                                label={
                                    <span>
                                        Create project &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Project {project-name} is created." />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <ArchiveProjectModal /> }}
                                label={
                                    <span>
                                        Archive project &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Project {project-name} is archived." />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <RestoreProjectModal /> }}
                                label={
                                    <span>
                                        Restore project &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Project {project-name} is restored." />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <EraseProjectModal /> }}
                                label={
                                    <span>
                                        Erase project &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Project {project-name} is erased." />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <CopyProjectModal /> }}
                                label={
                                    <span>
                                        Copy project &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Copied from: {project-name}." />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: <RestoreFromOldVersionModal /> }}
                                label={
                                    <span>
                                        Restore from old version &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="Restored from revision of {author} on {datetime}." />
                            </Form.Item>


                            <p>Folder structure configuration. If flat structure is used, all projects are stored in a given folder. If non-flat structure is used, projects locations can be modified.</p>
                            <Form.Item
                                label={
                                    <span>
                                        Flat folder structure: &nbsp;
                                    </span>
                                }
                            >
                                <Checkbox onChange={() => setHide(!hide)} />
                            </Form.Item>
                            {!hide && (
                                <Form.Item
                                    label={
                                        <span>
                                            Path &nbsp;
                                        </span>
                                    }
                                >
                                    <Input defaultValue="DESIGN/rules/" />
                                </Form.Item>
                            )}
                        </Form>
                    </Card>
                )}
            </div>
        </div>
    )
};

