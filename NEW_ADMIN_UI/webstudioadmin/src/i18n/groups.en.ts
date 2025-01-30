import i18next from 'i18next'

i18next.addResourceBundle('en', 'groups', {
    confirm_deletion: 'Are you sure you want to delete this group?',
    confirm_deletion_title: 'Confirm Deletion',
    table: {
        name: 'Name',
        description: 'Description',
        members: 'Members',
        actions: 'Actions',
        no_results: 'No groups found'
    },
    apply: 'Apply',
    invite_group: 'Invite group',
    cancel: 'Cancel',
    invite: 'Invite',
    role: {
        VIEWER: 'Viewer',
        CONTRIBUTOR: 'Contributor',
        MANAGER: 'Manager'
    }
})