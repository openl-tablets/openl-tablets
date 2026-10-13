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
        up_to_date_receive: 'You have all their updates. Nothing to merge.',
        up_to_date_send: 'They have all your updates. Nothing to merge.',
    },

    scope: {
        tab_one: '{{label}} · {{count}} project',
        tab_other: '{{label}} · {{count}} projects',
        whole_branch: 'Sync merges whole branches: the changes of every project below are merged, '
            + 'not only those of the current project.',
        nothing: 'Nothing to merge.',
        unknown: 'Sync could not tell what this option brings in.',
        current: 'Current project',
        other: 'Other project',
        outside: 'Files outside projects',
        changed_files_one: '{{count}} changed file',
        changed_files_other: '{{count}} changed files',
        changed_in_both: 'Changed in both branches',
        changed_in_both_hint: 'Sync merges these files by itself where the changes do not overlap, '
            + 'and asks you to resolve the rest.',
        hidden_one: 'and {{count}} project you cannot read',
        hidden_other: 'and {{count}} projects you cannot read',
        hidden_only_one: 'This option changes only {{count}} project you cannot read.',
        hidden_only_other: 'This option changes only {{count}} projects you cannot read.',
    },

    summary: {
        title: 'Merge Summary',
        merged: 'Merged "{{source}}" into "{{target}}".',
        resolved: 'Conflicts resolved and "{{source}}" merged into "{{target}}".',
        nothing: 'The merge changed no file you can read.',
        hidden_only_one: 'The merge changed only {{count}} project you cannot read.',
        hidden_only_other: 'The merge changed only {{count}} projects you cannot read.',
        sheet_from: 'Sheet "{{sheet}}" taken from {{branch}}',
        sheet_both: 'Sheet "{{sheet}}" combined from both branches',
        sheets_hint: 'Sync merged this workbook by itself, sheet by sheet: a sheet one branch changed is taken from '
            + 'that branch, and a sheet both branches changed is combined from the changes of both.',
        compare: 'Compare',
        compare_hint: 'Compare the file before and after the merge',
        close: 'Close',
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
        resolved: 'Resolved',
    },

    compare: {
        title: 'Compare File Versions',
        download_yours: 'Download your version',
        download_theirs: 'Download their version',
        download_base: 'Download base version',
        deleted_yours: 'Deleted in your version',
        deleted_theirs: 'Deleted in their version',
        deleted_base: 'Deleted in base version',
    },

    upload: {
        select_file: 'Select file',
    },

    buttons: {
        cancel: 'Cancel',
        resolve: 'Save and Resolve',
    },

    errors: {
        load_failed: 'Failed to load project information.',
        merge_failed: 'Merge operation failed.',
        cancel_failed: 'Failed to cancel the merge.',
        resolve_failed: 'Failed to resolve conflicts.',
        check_failed: 'Failed to check merge status.',
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
