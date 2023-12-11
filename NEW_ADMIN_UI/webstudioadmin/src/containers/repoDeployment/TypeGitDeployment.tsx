import React, { useState } from 'react'
import { Form, Input, Checkbox } from 'antd'
import InfoFieldModal from 'components/modal/InfoFieldModal'

export function TypeGitDeployment() {
    const [ open, setOpen ] = useState(true)

    const LocalPathModal = (
        <InfoFieldModal
            text={(
                <>
                    A local path to directory for Git repository. WebStudio uses this specified path to upload Git repository from the server and works with it.
                    <br />
                    <b>NOTE:</b>
                    {' '}
                    Read/Write rights for specified directory is mandatory for WebStudio.
                </>
            )}
        />
    )

    const RemoteRepoModal = (
        <InfoFieldModal
            text={(
                <ul>
                    <li>If checked, use remote Git repository. WebStudio will pull and push changes to it.</li>
                    <li>If unchecked, repository is stored in local file system only.</li>
                </ul>
            )}
        />
    )

    const ProtectedBranchesModal = (
        <InfoFieldModal
            text={(
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
            )}
        />
    )

    const URLModal = (
        <InfoFieldModal
            text={(
                <p>
                    Remote URL or local path to Git repository.
                    <br />
                    <b>For example:</b>
                    {' '}
                    https://github.com/git-repo/git-repo.git or /var/local-git-repo
                </p>
            )}
        />
    )

    return (
        <div>
            <Form labelAlign="left" labelCol={{ span: 8 }} wrapperCol={{ span: 20 }}>

                <Form.Item
                    label={(<span>Remote repository &nbsp;</span>)}
                    tooltip={{ title: 'Details', icon: RemoteRepoModal }}
                >
                    <Checkbox onChange={() => setOpen(!open)} />
                </Form.Item>
                {open && (
                    <Form
                        labelAlign="left"
                        labelCol={{ span: 8 }}
                        wrapperCol={{ span: 20 }}
                    >
                        <Form.Item
                            label={(<span>Local path* &nbsp;</span>)}
                            tooltip={{ title: 'Details', icon: LocalPathModal }}
                        >
                            <Input defaultValue="./openl-demo/repositories/design" />
                        </Form.Item>
                        <Form.Item
                            label={(<span>Protected branches &nbsp;</span>)}
                            tooltip={{ title: 'Details', icon: ProtectedBranchesModal }}
                        >
                            <Input />
                        </Form.Item>
                    </Form>
                )}
                {!open && (
                    <Form
                        labelAlign="left"
                        labelCol={{ span: 8 }}
                        wrapperCol={{ span: 20 }}
                    >
                        <Form.Item
                            label={(<span>URL* &nbsp;</span>)}
                            tooltip={{ title: 'Details', icon: URLModal }}
                        >
                            <Input />
                        </Form.Item>
                        <Form.Item label={(<span>Login &nbsp;</span>)}>
                            <Input />
                        </Form.Item>
                        <Form.Item label={(<span>Password &nbsp;</span>)}>
                            <Input.Password />
                        </Form.Item>
                        <Form.Item label={(<span>Local path &nbsp;</span>)}>
                            <Input defaultValue="./openl-demo/repositories/name" />
                        </Form.Item>
                        <Form.Item label={(<span>Branch &nbsp;</span>)}>
                            <Input />
                        </Form.Item>
                        <Form.Item label={(<span>Protected branches &nbsp;</span>)}>
                            <Input />
                        </Form.Item>
                        <Form.Item label={(<span>Changes check interval (sec) &nbsp;</span>)}>
                            <Input defaultValue="10" />
                        </Form.Item>
                        <Form.Item label={(<span>Connection timeout (sec) &nbsp;</span>)}>
                            <Input defaultValue="60" />
                        </Form.Item>
                    </Form>
                )}
            </Form>
        </div>
    )
}
