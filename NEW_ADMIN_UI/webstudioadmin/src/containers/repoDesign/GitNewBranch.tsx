import React, { useState } from 'react'
import {
    Checkbox, Divider, Form, Input,
} from 'antd'
import InfoFieldModal from 'components/modal/InfoFieldModal'
import { Comments } from './Comments'

export function GitNewBranch() {
    const [ hide, setHide ] = useState(true)

    const DefaultBranchModal = (
        <InfoFieldModal
            text={(
                <>
                    <p>This is a pattern for new branches in Git repository.</p>
                    <ul>
                        <li>&#123;project - name&#125; is replaced by project name.</li>
                        <li>&#123;username&#125; is replaced by username.</li>
                        <li>&#123;current - date&#125; is replaced by current date.</li>
                    </ul>
                    <p>NOTE: Must not contain the following characters:&#92; &#58; &#42; &#63; &#34; &#39; &#60; &#62; &#124; &#123; &#125; &#126; &#94; </p>
                </>
            )}
        />
    )

    const BranchNamePatternModal = (
        <InfoFieldModal
            text={(
                <p>Additional regular expression that will be used to validate the name of the new branch.</p>
            )}
        />
    )
    const InvalidBranchModal = (
        <InfoFieldModal
            text={(
                <p>An error message that will be shown to the user when trying to create a new branch with a name that does not match the additional regular expression.</p>
            )}
        />
    )
    return (
        <div>

            <Form
                labelAlign="left"
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 20 }}
            >
                <p>
                    <b>New branch:</b>
                </p>
                <Form.Item
                    label={(<span>Default branch name &nbsp;</span>)}
                    tooltip={{ title: 'Details', icon: DefaultBranchModal }}
                >
                    <Input defaultValue="WebStudio/{project-name}/{username}/{current-date}" />
                </Form.Item>
                <Form.Item
                    label={(<span>Branch name pattern &nbsp;</span>)}
                    tooltip={{ title: 'Details', icon: BranchNamePatternModal }}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label={(
                        <span>
                            Invalid branch name
                            <br />
                            {' '}
                            message hint &nbsp;
                        </span>
                    )}
                    tooltip={{ title: 'Details', icon: InvalidBranchModal }}
                >
                    <Input />
                </Form.Item>
                <Divider />
                <b>
                    <Form.Item label={(<span>Customize comments &nbsp;</span>)}>
                        <Checkbox onChange={() => setHide(!hide)} />
                    </Form.Item>
                </b>
                {!hide && (<Comments />)}
                <Divider />
                <p>Folder structure configuration. If flat structure is used, all projects are stored in a given folder. If non-flat structure is used, projects locations can be modified.</p>
                <Form.Item label={(<span>Flat folder structure: &nbsp;</span>)}>
                    <Checkbox onChange={() => setHide(!hide)} />
                </Form.Item>
                {!hide && (
                    <Form.Item label={(<span>Path &nbsp;</span>)}>
                        <Input defaultValue="DESIGN/rules/" />
                    </Form.Item>
                )}
            </Form>
        </div>
    )
}
