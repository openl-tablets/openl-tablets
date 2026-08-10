import i18next from 'i18next'

i18next.addResourceBundle('en', 'merge', {
    title: 'Sync updates - {{projectName}}',

    branches: {
        current: 'Current branch',
        target: 'Merge with branch',
        select_placeholder: 'Select a branch',
        show_all: 'Show every branch of the repository',
        load_failed: 'Could not read the branches of the repository.',
        target_without_project: 'Branch "{{branch}}" does not hold this project yet. '
            + 'Sending your updates creates the project there.',
        show_all_hint: 'By default only the branches that hold this project are offered. '
            + 'Turn this on to merge into a branch that does not hold it yet, such as the main branch '
            + 'for a project created in its own branch.',
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
    },

    blocked: {
        protected: 'There are changes to merge, but the branch "{{branch}}" is protected and you may not merge into it.',
        locked: 'There are changes to merge, but the branch "{{branch}}" is locked by another operation. Try again later.',
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
        by: 'by {{author}}',
        at: '{{date}}',
        commit: 'Commit: {{commit}}',
        not_exists: 'File does not exist in this version',
    },

    resolution: {
        use_yours: 'Use yours',
        use_theirs: 'Use theirs',
        use_base: 'Use base',
        upload_custom: 'Upload merged file',
        delete_yours: 'Delete it',
        resolved: 'Resolved',
    },

    compare: {
        title: 'Compare File Versions',
        download_yours: 'Download your version',
        download_theirs: 'Download their version',
        download_base: 'Download base version',
    },

    upload: {
        title: 'Upload Merged File',
        description: 'Upload the manually merged file to resolve this conflict.',
        select_file: 'Select file',
        selected: 'Selected: {{filename}}',
        apply: 'Apply',
    },

    buttons: {
        cancel: 'Cancel',
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
        compare_failed: 'Failed to open file comparison.',
        all_conflicts_required: 'Please resolve all conflicts before saving.',
    },

    bypass: {
        title: 'Bypass branch protection?',
        description: 'The branch "{{branch}}" is protected. Confirming will merge into it anyway. This action cannot be undone.',
        description_both: 'Both branches are protected: sending into "{{send}}" and receiving into "{{receive}}". Confirming will merge anyway. This action cannot be undone.',
        confirm: 'Confirm bypass and merge',
        action_tooltip: 'Target branch is protected. Merging will require explicit bypass confirmation.',
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
