import i18next from 'i18next'

i18next.addResourceBundle('en', 'users', {
    add_user: 'Add User',
    edit_user: 'Edit User',
    users_table: {
        username: 'Username',
        first_name: 'First Name',
        last_name: 'Last Name',
        email: 'Email',
        full_name: 'Full Name',
        groups: 'Groups',
        actions: 'Actions',
        email_not_verified: 'Email not verified',
    },
    confirm_delete_user: 'Are you sure you want to delete this user?',
    confirm_deletion: 'Confirm Deletion',
    first_last: 'First Last',
    last_first: 'Last First',
    other: 'Other',
    details: 'Details',
    access_management: 'Access Management',
    design_repositories: 'Design Repositories',
    deploy_repositories: 'Deploy Repositories',
    projects: 'Projects',
    account: 'Account',
    name: 'Name',
    edit_modal: {
        username: 'Username',
        email: 'Email',
        password: 'Password',
        first_name: 'First Name (Given Name)',
        last_name: 'Last Name (Family Name)',
        display_name: 'Display Name',
        groups: 'Groups',
        create: 'Create',
        update: 'Update',
        cancel: 'Cancel',
        first_name_max_length: 'First Name must be at most 25 characters long',
        last_name_max_length: 'Last Name must be at most 25 characters long',
        display_name_max_length: 'Display Name must be at most 64 characters long',
        password_required: 'Password is required',
        email_invalid: 'Invalid email address',
        email_max_length: 'Email must be at most 254 characters long',
        username_required: 'Username is required',
    },
    action: {
        edit_details: 'Edit Details',
        edit_access_rights: 'Edit Access Rights',
        delete_user: 'Delete User',
    },
    resend_verification_email: 'Resend Verification Email',
    resend_verification_email_timer: 'You can request a new email in {{seconds}} seconds.',
    email_verification_warning: 'Please verify your new email address. Check your mailbox for a verification link.',
    email_verified_title: 'Email verified!',
    email_verified_message: 'Your email address has been successfully verified.',
    email_verified_redirect: 'You will be redirected to the main page in {{seconds}} seconds...',
    go_to_main_page: 'Go to OpenL Studio',
    verification_failed_title: 'Verification failed',
    verification_failed_message_1: 'We could not verify your email. The link may be invalid or expired.',
    verification_failed_message_2: 'Please request a new verification email or contact support.',
    verification_failed_resend_prompt: 'If you want to receive a new verification email, click the button below:',
    send_verification_email: 'Send Verification Email',
    cannot_determine_current_user: 'Cannot determine current user.',
    verification_email_sent: 'Verification email sent. Please check your mailbox.',
    failed_to_send_verification_email: 'Failed to send verification email.',
})
