import i18next from 'i18next'

i18next.addResourceBundle('en', 'execution', {
    input: {
        form: 'Form',
        json: 'JSON',
        jsonPlaceholder: 'Input JSON here',
        jsonInvalid: 'Invalid JSON: {{message}}',
        runtimeContext: 'Runtime Context',
        create: 'Create',
        edit: 'Edit',
        clear: 'Clear',
        add: 'Add',
        remove: 'Remove',
        yes: 'true',
        no: 'false',
        key: 'Key',
        moduleOnly: 'Within Current Module Only',
        moduleOnlyLocked: 'Only the current module can be used while the project is loading or other modules have errors.',
    },

    testCases: {
        title: 'Test Cases',
        id: 'ID',
        description: 'Description',
        total: 'Total test cases: {{count}}',
        noCase: 'Select a test case',
    },
})
