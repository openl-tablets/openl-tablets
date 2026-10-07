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
    messages: {
        deploying: 'Deploying...',
        deploying_project: 'Deploying project...',
    },
    notifications: {
        deploy_success: 'Project deployed',
        deploy_success_description: 'Project "{{projectName}}" has been deployed.',
        deploy_failed: 'Deployment failed',
        deploy_failed_description: 'Failed to deploy the project. Please try again.',
        no_deploy_rights: 'You do not have permission to deploy to the selected repository. Please select another repository.',
        no_deploy_rights_short: 'No permission to deploy. Select another repository.',
        main_branch_only: 'This repository takes a project only from the main branch of its design repository, and the project is on "{{branch}}". Switch the project to the main branch, or pick another repository.',
    },
})
