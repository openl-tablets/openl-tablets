import React, { useState } from 'react';
import { Form, Input } from 'antd';
import InfoFieldModal from 'components/modal/InfoFieldModal';


export const Comments = () => {

    const MessageTemplateModal = <InfoFieldModal
        text={
            <>
                <p>Comment message template for Git commits.</p>
                <ul>

                    <li>&#123;user-message&#125;  is replaced by user defined commit message. This part of commit message is mandatory.</li>
                    <li>&#123;commit-type&#125; is a system property for commits to recognize commit type from a message. This part of commit message is mandatory.</li>
                </ul>

                <p>NOTE: Keep default value for non Git repositories.</p>
            </>
        } />
    const UserMessagePaternModal = <InfoFieldModal
        text={
            <>
                <p>A regular expression that is used to validate user message.</p>
            </>
        } />
    const InvalidUserMessageHintModal = <InfoFieldModal
        text={
            <>
                <p>This message is shown to user if user message is not matched to message validation pattern.</p>
            </>
        } />
    const SaveProjectModal = <InfoFieldModal
        text={
            <>
                <ul>
                    This specified value is used as default message for 'Save project' action.
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        } />
    const CreateProjectModal = <InfoFieldModal
        text={
            <>
                <ul>
                    This specified value is used as default message for 'Create project' action.
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        } />
    const ArchiveProjectModal = <InfoFieldModal
        text={
            <>
                <ul>
                    This specified value is used as default message for 'Archive project' action.
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        } />
    const RestoreProjectModal = <InfoFieldModal
        text={
            <>
                <ul>
                    This specified value is used as default message for 'Restore project' action.
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        } />
    const EraseProjectModal = <InfoFieldModal
        text={
            <>
                <ul>
                    This specified value is used as default message for 'Erase project' action.
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        } />
    const CopyProjectModal = <InfoFieldModal
        text={
            <>
                <ul>
                    This specified value is used as default message for 'Copy project' action.
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        } />
    const RestoreFromOldVersionModal = <InfoFieldModal
        text={
            <>
                <ul>
                    This specified value is used as default message when a project is restored from old version.

                    <li>&#123;revision&#125; is replaced by old revision number.</li>
                    <li>&#123;author&#125; is replaced by the author of old project version.</li>
                    <li>&#123;datetime&#125; is replaced by the date of old project version.</li>
                </ul>
            </>
        } />

    return (
        <div>
            <Form
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 20 }}
                labelAlign="left"
            >
                <Form.Item tooltip={{ title: 'Details', icon: MessageTemplateModal }}
                    label={
                        <span>
                            Message template &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="{user-message} Type: {commit-type}." />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: UserMessagePaternModal }}
                    label={
                        <span>
                            User message pattern &nbsp;
                        </span>
                    }
                >
                    <Input />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: InvalidUserMessageHintModal }}
                    label={
                        <span>
                            Invalid user message hint &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Invalid comment: Comment doesn't match validation pattern" />
                </Form.Item>
                <p><b>User message templates:</b></p>
                <Form.Item tooltip={{ title: 'Details', icon: SaveProjectModal }}
                    label={
                        <span>
                            Save project &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Project {project-name} is saved." />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: CreateProjectModal }}
                    label={
                        <span>
                            Create project &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Project {project-name} is created." />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: ArchiveProjectModal }}
                    label={
                        <span>
                            Archive project &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Project {project-name} is archived." />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: RestoreProjectModal }}
                    label={
                        <span>
                            Restore project &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Project {project-name} is restored." />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: EraseProjectModal }}
                    label={
                        <span>
                            Erase project &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Project {project-name} is erased." />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: CopyProjectModal }}
                    label={
                        <span>
                            Copy project &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Copied from: {project-name}." />
                </Form.Item>
                <Form.Item tooltip={{ title: 'Details', icon: RestoreFromOldVersionModal }}
                    label={
                        <span>
                            Restore from old version &nbsp;
                        </span>
                    }
                >
                    <Input defaultValue="Restored from revision of {author} on {datetime}." />
                </Form.Item>
            </Form>
        </div>
    );
}