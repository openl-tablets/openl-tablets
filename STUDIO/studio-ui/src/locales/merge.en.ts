import i18next from 'i18next'

i18next.addResourceBundle('en', 'merge', {
    title: 'Sync updates - {{projectName}}',

    branches: {
        current: 'Current branch',
        target: 'Merge with branch',
        select_placeholder: 'Select a branch',
    },

    actions: {
        receive: 'Receive their updates',
        receive_description: 'Merge changes from selected branch into current branch',
        send: 'Send your updates',
        send_description: 'Merge changes from current branch into selected branch',
    },

    status: {
        checking: 'Checking merge status...',
        merging: 'Merging branches...',
        up_to_date_receive: 'You have all their updates. Nothing to merge.',
        up_to_date_send: 'They have all your updates. Nothing to merge.',
        protected_warning: 'Warning: The target branch is protected.',
        locked_warning: 'Project is currently being edited in another branch.',
    },

    conflicts: {
        title: 'Resolve Conflicts',
        description: 'The following files have conflicts that need to be resolved:',
        message_label: 'Merge message',
        message_placeholder: 'Enter merge commit message',
        file_column: 'File',
        compare_column: 'Compare',
        resolution_column: 'Resolution',
    },

    revisions: {
        yours: 'Your version',
        theirs: 'Their version',
        base: 'Base version',
        by: 'by {{- author}}',
        at: '{{- date}}',
        commit: 'Commit: {{- commit}}',
        not_exists: 'File does not exist in this version',
    },

    resolution: {
        use_yours: 'Use yours',
        use_theirs: 'Use theirs',
        use_base: 'Use base',
        upload_custom: 'Upload merged file',
        delete_yours: 'Delete it',
        delete_theirs: 'Delete it',
    },

    compare: {
        title: 'Compare File Versions',
        download_yours: 'Download your version',
        download_theirs: 'Download their version',
        download_base: 'Download base version',
        side_yours: 'Your Version',
        side_theirs: 'Their Version',
        side_base: 'Base Version',
        excel_notice: 'Excel files can be downloaded for comparison.',
    },

    upload: {
        title: 'Upload Merged File',
        description: 'Upload the manually merged file to resolve this conflict.',
        select_file: 'Select file',
        selected: 'Selected: {{filename}}',
        apply: 'Apply',
    },

    commit_info: {
        title: 'Configure Git Commit Info',
        description: 'Git requires author information for commits. Please configure your name and email.',
        display_name: 'Display Name',
        display_name_placeholder: 'Enter your display name',
        email: 'Email',
        email_placeholder: 'Enter your email address',
    },

    buttons: {
        cancel: 'Cancel',
        save: 'Save',
        close: 'Close',
        resolve: 'Save and Resolve',
        download: 'Download',
    },

    messages: {
        merge_success: 'Branches merged successfully.',
        resolve_success: 'Conflicts resolved successfully.',
        cancelled: 'Merge cancelled.',
    },

    errors: {
        load_failed: 'Failed to load project information.',
        merge_failed: 'Merge operation failed.',
        resolve_failed: 'Failed to resolve conflicts.',
        check_failed: 'Failed to check merge status.',
        download_failed: 'Failed to download file.',
        upload_required: 'Please upload a file for custom resolution.',
        all_conflicts_required: 'Please resolve all conflicts before saving.',
        commit_info_failed: 'Failed to save commit information.',
    },

    notifications: {
        merge_success: 'Merge Successful',
        merge_success_description: 'The branches have been merged successfully.',
        resolve_success: 'Conflicts Resolved',
        resolve_success_description: 'All conflicts have been resolved and the merge is complete.',
        merge_cancelled: 'Merge Cancelled',
        merge_cancelled_description: 'The merge operation has been cancelled.',
    },
})
