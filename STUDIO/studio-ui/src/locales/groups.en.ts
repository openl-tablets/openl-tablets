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
    invite_group: 'Invite Group',
    cancel: 'Cancel',
    invite: 'Invite',
    role: {
        VIEWER: 'Viewer',
        CONTRIBUTOR: 'Contributor',
        MANAGER: 'Manager'
    },
    action: {
        edit_details: 'Edit Details',
        edit_access_rights: 'Edit Access Rights',
        delete_group: 'Delete Group',
    },
    details: 'Details',
    access_management: 'Access Management',
    design_repositories: 'Design Repositories',
    deploy_repositories: 'Deploy Repositories',
    projects: 'Projects',
    description: 'Description',
    name: 'Name',
    admin: 'Admin',
    name_required: 'Name is required',
    group_name_max_length: 'Group name must be at most 65 characters long',
    description_max_length: 'Description must be at most 200 characters long',
    edit_group: 'Edit Group',
    failed_to_load_groups: 'Failed to load groups. Please try again.',
    retry: 'Retry',
})
