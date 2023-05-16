import React, { useState } from 'react';
import { Form, Input, Checkbox, Divider } from 'antd';
import InfoFieldModal from 'components/modal/InfoFieldModal';
import { Comments } from '../repoDesign/Comments';


export const TypeGitDeployment = () => {
    const [active, setActive] = useState(true);
    const [hide, setHide] = useState(true);
    const [open, setOpen] = useState(true);


    const LocalPathModal = <InfoFieldModal
        text={
            <>
                A local path to directory for Git repository. Webstudio uses this specified path to upload Git repository from the server and works with it.
                <br></br><b>NOTE:</b> Read/Write rights for specified directory is mandatory for Webstudio.
            </>
        }
    />

    const RemoteRepoModal = <InfoFieldModal
        text={
            <>
                <ul><li>If checked, use remote Git repository. WebStudio will pull and push changes to it.</li>
                    <li>If unchecked, repository is stored in local file system only.</li></ul>
            </>
        } />

    const ProtectedBranchesModal = <InfoFieldModal
        text={
            <>
                <ol>
                    The list of protected branches must be comma separated.
                    <li>? - maches any single characters</li>
                    <li>* - matches simple branch names like master. If a branch name has a path separator, it will be skipped</li>
                    <li>** - matches all branches</li>
                    <li>*.* - matches simple branches containing a dot</li>
                    <li>*.&#123;10, 11&#125;- matches branch ending with .10 or .11</li>
                </ol>
                Example: release-*
            </>
        } />

    const URLModal = <InfoFieldModal
        text={
            <>
                <p>Remote URL or local path to Git repository.<br></br><b>For example:</b> https://github.com/git-repo/git-repo.git or /var/local-git-repo</p>
            </>
        } />

    return (
        <div>
            {active && (

                <Form
                    labelCol={{ span: 10 }}
                    wrapperCol={{ span: 18 }}
                    labelAlign="left">

                    <Form.Item tooltip={{ title: 'Details', icon: RemoteRepoModal }}
                        label={
                            <span>
                                Remote repository &nbsp;
                            </span>
                        }
                    >
                        <Checkbox onChange={() => setOpen(!open)} />
                    </Form.Item>
                    {open && (
                        <Form labelCol={{ span: 10 }}
                            wrapperCol={{ span: 18 }}
                            labelAlign="left">
                            <Form.Item tooltip={{ title: 'Details', icon: LocalPathModal }}
                                label={
                                    <span>
                                        Local path* &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="./openl-demo/repositories/design" />
                            </Form.Item>
                            <Form.Item tooltip={{ title: 'Details', icon: ProtectedBranchesModal }}
                                label={
                                    <span>
                                        Protected branches &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                        </Form>
                    )}

                    {!open && (
                        <Form
                            labelCol={{ span: 10 }}
                            wrapperCol={{ span: 18 }}
                            labelAlign="left">
                            <Form.Item tooltip={{ title: 'Details', icon: URLModal }}
                                label={
                                    <span>
                                        URL* &nbsp;
                                    </span>
                                }
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Login &nbsp;
                                    </span>
                                }
                            >
                                <Input />
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
                            <Form.Item
                                label={
                                    <span>
                                        Local path &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="./openl-demo/repositories/name" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Branch &nbsp;
                                    </span>
                                }
                            >
                                <Input />
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
                            <Form.Item
                                label={
                                    <span>
                                        Changes check interval (sec) &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="10" />
                            </Form.Item>
                            <Form.Item
                                label={
                                    <span>
                                        Connection timetout (sec) &nbsp;
                                    </span>
                                }
                            >
                                <Input defaultValue="60" />
                            </Form.Item>
                        </Form>
                    )}
                </Form>
            )}
        </div>
    );
}