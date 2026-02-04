import i18next from 'i18next'

i18next.addResourceBundle('en', 'trace', {
    title: 'Execution Trace',

    tree: {
        showDetails: 'Show detailed trace',
        hideDetails: 'Hide failed conditions',
        expandAll: 'Expand All',
        collapseAll: 'Collapse All',
        noNodes: 'No trace data available',
    },

    details: {
        title: 'Node Details',
        parameters: 'Parameters',
        result: 'Returned Result',
        errors: 'Errors',
        table: 'Traced Table',
        noSelection: 'Select a node to view details',
        noParameters: 'No parameters',
        noResult: 'No result',
        noErrors: 'No errors',
        noTable: 'No table view available',
    },

    param: {
        loadValue: 'Load value',
        loading: 'Loading...',
        name: 'Name',
        type: 'Type',
        value: 'Value',
        expand: 'Expand',
        collapse: 'Collapse',
    },

    progress: {
        pending: 'Waiting to start...',
        started: 'Executing trace...',
        completed: 'Trace completed',
        interrupted: 'Trace interrupted',
        error: 'Trace failed',
        cancel: 'Cancel',
    },

    loading: 'Loading trace...',
    loadingDetails: 'Loading details...',
    loadingTable: 'Loading table...',
    loadingChildren: 'Loading...',

    errors: {
        notFound: 'Trace not found. Please execute a trace first.',
        missingParams: 'Missing projectId or tableId in URL.',
        notCompleted: 'Trace execution is still in progress.',
        loadFailed: 'Failed to load trace data.',
        detailsFailed: 'Failed to load node details.',
        tableFailed: 'Failed to load table view.',
        parameterFailed: 'Failed to load parameter value.',
        cancelFailed: 'Failed to cancel trace.',
    },

    nodeTypes: {
        decisiontable: 'Decision Table',
        method: 'Method',
        rule: 'Rule',
        condition: 'Condition',
        spreadsheet: 'Spreadsheet',
        spreadsheetCell: 'Spreadsheet Cell',
        tbasic: 'TBasic',
        tbasicOperation: 'TBasic Operation',
        cmatch: 'Column Match',
        overloadedMethodChoice: 'Overloaded Method',
    },

    severity: {
        ERROR: 'Error',
        WARNING: 'Warning',
        INFO: 'Info',
    },

    modal: {
        title: 'Trace Execution',
        statuses: {
            pending: 'Preparing trace...',
            started: 'Executing trace...',
            completed: 'Trace completed!',
            interrupted: 'Trace was interrupted',
            error: 'Trace failed',
        },
        actions: {
            cancel: 'Cancel',
            close: 'Close',
        },
        errors: {
            startFailed: 'Failed to start trace',
            cancelFailed: 'Failed to cancel trace',
        },
    },
})
