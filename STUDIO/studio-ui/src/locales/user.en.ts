import i18next from 'i18next'

i18next.addResourceBundle('en', 'user', {
    profile: {
        name: 'Name',
        account: 'Account',
        change_password: 'Change Password',
        username: 'Username',
        email: 'Email',
        first_name: 'First Name (Given Name)',
        last_name: 'Last Name (Family Name)',
        display_name: 'Display Name',
        current_password: 'Current Password',
        new_password: 'New Password',
        confirm_password: 'Confirm Password',
    },
    settings: {
        table_settings: 'Table Settings',
        show_header: 'Show Header',
        show_formulas: 'Show Formulas',
        default_order: 'Default Order',
        testing_settings: 'Testing Settings',
        tests_per_page: 'Tests Per Page',
        failures_only: 'Failures Only',
        compound_result: 'Compound Result',
        trace_settings: 'Trace Settings',
        show_numbers_without_formatting: 'Show Numbers Without Formatting',
    },
    user_profile_updated_successfully: 'User profile updated successfully',
    user_settings_updated_successfully: 'User settings updated successfully',
})
