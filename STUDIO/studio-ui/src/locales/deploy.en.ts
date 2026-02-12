import i18next from 'i18next'

i18next.addResourceBundle('en', 'deploy', {
    title: 'Deploy "{{projectName}}" project',
    repository: {
        label: 'Repository',
        placeholder: 'Select a repository',
    },
    deployment_name: {
        label: 'Deployment Name',
        placeholder: 'Select a deployment name or enter new name',
    },
    comment: {
        label: 'Comment',
        placeholder: 'Enter a comment',
    },
    buttons: {
        cancel: 'Cancel',
        deploy: 'Deploy',
    },
    validation: {
        repository_required: 'Please select a repository',
        deployment_name_required: 'Please enter a deployment name',
        comment_required: 'Please enter a comment',
    },
    messages: {
        deploying: 'Deploying...',
        deploying_configuration: 'Deploying configuration...',
        deploy_success: 'Deployment successful',
        deploy_error: 'Deployment failed',
    },
    notifications: {
        deploy_configuration_added: 'Deploy Configuration added',
        deploy_configuration_added_description: 'The deployment configuration has been successfully added.',
        deploy_failed: 'Deploy Failed',
        deploy_failed_description: 'Failed to deploy configuration. Please try again.',
        no_deploy_rights: 'You do not have permission to deploy to the selected repository. Please select another repository.',
        no_deploy_rights_short: 'No permission to deploy. Select another repository.',
    },
})
