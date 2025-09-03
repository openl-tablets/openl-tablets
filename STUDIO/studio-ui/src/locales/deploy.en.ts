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
        deploy_success: 'Deployment successful',
        deploy_error: 'Deployment failed',
    },
})
