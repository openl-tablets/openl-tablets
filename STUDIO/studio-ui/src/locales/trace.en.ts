import i18next from 'i18next'

i18next.addResourceBundle('en', 'trace', {
    title: 'Execution Trace',

    tree: {
        title: 'Call tree',
        modeSimple: 'Tree',
        modeCallStack: 'Call Stack',
        runToHint: 'Run execution to here',
        resultHint: 'View result',
    },

    debug: {
        resume: 'Resume — run to the next breakpoint or the end',
        pause: 'Pause — stop at the next step',
        stepInto: 'Step into — go inside the next called rule',
        stepOver: 'Step over — run the next line without going inside',
        stepOut: 'Step out — finish this rule and return to its caller',
        stop: 'Stop debugging',
        callStack: 'Call stack',
        notSuspended: 'Execution is not suspended',
        breakpoints: 'Breakpoints',
        noBreakpoints: 'No breakpoints. Add one from a table below.',
        addBreakpoint: 'Add breakpoint',
        addBreakpointPlaceholder: 'Find a table by name…',
        removeBreakpoint: 'Remove breakpoint',
        steps: 'Steps',
        noSteps: 'No steps',
        executing: 'executing…',
        pending: 'pending',
        status: {
            PENDING: 'Starting…',
            RUNNING: 'Running',
            SUSPENDED: 'Suspended',
            COMPLETED: 'Completed',
            ERROR: 'Error',
            TERMINATED: 'Terminated',
        },
    },

    error: {
        inTable: 'in {{table}}',
        atLocation: 'at {{location}}',
        technicalDetails: 'Show technical details',
    },

    decision: {
        fired: 'Fired: {{rules}}',
        noneFired: 'No rule fired',
        notYetFired: 'No rule has fired yet.',
        breakOnFire: 'Break when a rule fires',
        breakOnFireHint: "Suspend whenever this table fires a rule — when all of a rule's conditions match.",
        breakpointLabel: '{{table}} — on rule fired',
        ruleBreakpointLabel: '{{table}} — when {{rule}} fires',
        breakOnRule: 'Break on rule',
        breakOnRulePlaceholder: 'Pick rules to break on…',
    },

    // Explains the cell/row colours shared by the traced table, the spreadsheet grid and the decision panel.
    legend: {
        current: 'Current step',
        result: 'Result',
        conditionMet: 'Condition met',
        conditionNotMet: 'Condition not met',
    },

    copy: {
        parameters: 'Copy parameters as JSON',
        result: 'Copy result as JSON',
        copied: 'Copied!',
        failed: 'Failed to copy to clipboard',
    },

    details: {
        parameters: 'Parameters',
        result: 'Returned Result',
        errors: 'Errors',
        table: 'Traced Table',
        decision: 'Decision',
        noSelection: 'Select a stack frame to view its variables',
        noParameters: 'No parameters',
        noResult: 'No result',
    },

    table: {
        truncated: 'Showing the first {{count}} of {{total}} rows. Open in Excel to see the full table.',
    },

    param: {
        loadValue: 'Load value',
    },

    loadingDetails: 'Loading details...',
    loadingTable: 'Loading table...',

    errors: {
        notFound: 'Trace not found. Please execute a trace first.',
        missingParams: 'Missing projectId or tableId in URL.',
        tableFailed: 'Failed to load table view.',
        parameterFailed: 'Failed to load parameter value.',
    },

    severity: {
        ERROR: 'Error',
    },

    modal: {
        title: 'Trace Execution',
        statuses: {
            started: 'Executing trace...',
        },
        errors: {
            startFailed: 'Failed to start trace',
        },
    },
})
