import React from 'react'
import InfoFieldModal from '../../components/modal/InfoFieldModal'

// @ts-ignore
export const ProtectedBranchesModal = (
    <InfoFieldModal
        text={(
            <>
                The list of protected branches must be comma separated.
                <ul>
                    <li>
                        <b>?</b> - maches any single characters
                    </li>
                    <li>
                        <b>*</b> - matches simple branch names like <b>master</b>. If a branch name has a path separator, it will be skipped
                    </li>
                    <li>
                        <b>**</b> - matches all branches
                    </li>
                    <li>
                        <b>*.*</b> - matches simple branches containing a dot
                    </li>
                    <li>
                        <b>*.{'{'}10,11{'}'}</b> - matches branch ending with <b>.10</b> or <b>.11</b>
                    </li>
                </ul>
                <b>Example: </b>release-*
            </>

        )}
    />
)

export const URLModal = (
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

export const BranchModal = (
    <InfoFieldModal
        text="The main branch to commit changes. Usually, 'master' branch."
    />
)

export const DefaultBranchModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This is a pattern for new branches in Git repository.</p>
                <ul>
                    <li>&#123;project - name&#125; is replaced by project name.</li>
                    <li>&#123;username&#125; is replaced by username.</li>
                    <li>&#123;current - date&#125; is replaced by current date.</li>
                </ul>
                <p><b>NOTE:</b> Must not contain the following characters:&#92; &#58; &#42; &#63; &#34; &#39; &#60; &#62; &#124; &#123; &#125; &#126; &#94; </p>
            </>
        )}
    />
)

export const NewBranchRegexModal = (
    <InfoFieldModal
        text="An additional regular expression that will be used to validate the new branch name."
    />
)

export const MewBranchRegexErrorModal = (
    <InfoFieldModal
        text="An error message that will be shown to the user when trying to create a new branch with a name that does not match the additional regular expression."
    />
)

export const MessageTemplateModal = (
    <InfoFieldModal
        text={(
            <>
                <p>Comment message template for Git commits.</p>
                <ul>

                    <li>&#123;user-message&#125;  is replaced by user defined commit message. This part of commit message is mandatory.</li>
                    <li>&#123;commit-type&#125; is a system property for commits to recognize commit type from a message. This part of commit message is mandatory.</li>
                </ul>
                <p><b>NOTE:</b> Keep default value for non Git repositories.</p>
            </>
        )}
    />
)

export const UserMessagePatternModal = (
    <InfoFieldModal text={(<p>A regular expression that is used to validate user message.</p>)} />
)

export const InvalidUserMessageHintModal = (
    <InfoFieldModal
        text={(
            <p>This message is shown to user if user message is not matched to message validation pattern.</p>
        )}
    />
)

export const MessageSaveProjectModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This specified value is used as default message for 'Save project' action.</p>
                <ul>
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        )}
    />
)

export const MessageCreateProjectModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This specified value is used as default message for 'Create project' action.</p>
                <ul>
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        )}
    />
)

export const MessageArchiveProjectModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This specified value is used as default message for 'Archive project' action.</p>
                <ul>

                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        )}
    />
)

export const MessageRestoreProjectModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This specified value is used as default message for 'Restore project' action.</p>
                <ul>
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        )}
    />
)

export const MessageEraseProjectModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This specified value is used as default message for 'Erase project' action.</p>
                <ul>
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        )}
    />
)

export const MessageCopyProjectModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This specified value is used as default message for 'Copy project' action.</p>
                <ul>
                    <li>&#123;project-name&#125; is replaced by current project in the message.</li>
                </ul>
            </>
        )}
    />
)

export const MessageRestoreFromOldVersionModal = (
    <InfoFieldModal
        text={(
            <>
                <p>This specified value is used as default message when a project is restored from old version.</p>
                <ul>
                    <li>&#123;revision&#125; is replaced by old revision number.</li>
                    <li>&#123;author&#125; is replaced by the author of old project version.</li>
                    <li>&#123;datetime&#125; is replaced by the date of old project version.</li>
                </ul>
            </>
        )}
    />
)
