import i18next from 'i18next'

i18next.addResourceBundle('en', 'project', {
    update_project_modal: {
        title: 'Update Project',
        source_zip: 'Zip archive',
        source_folder: 'Folder',
        zip_hint: 'Click or drag a zip archive to this area',
        folder_hint: 'Click or drag a folder to this area',
        folder_selected_one: '{{count}} file is selected.',
        folder_selected_other: '{{count}} files are selected.',
        replace_note: 'The project content is replaced with the upload: project files absent from it are deleted.',
        only_zip: 'Only zip files are accepted.',
        confirm_button: 'Update',
    },
    update_module_modal: {
        title: 'Update Module',
        hint: 'Click or drag an Excel file to this area',
        only_excel: 'Only xls, xlsx, and xlsm files are accepted.',
        name_differs: 'The selected file differs from the current "{{- file}}" module file. Ensure the correct file is chosen.',
        confirm_button: 'Update',
    },
    notifications: {
        project_updated: 'Project updated',
        project_updated_description: 'The "{{- project}}" project was updated successfully.',
        project_update_failed: 'Failed to update the project',
        module_updated: 'Module updated',
        module_updated_description: 'The "{{- file}}" file was updated successfully.',
        module_update_failed: 'Failed to update the module',
    },
})
