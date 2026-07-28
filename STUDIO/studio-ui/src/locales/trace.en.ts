import i18next from 'i18next'

i18next.addResourceBundle('en', 'trace', {
    title: 'Execution trace',

    watch: {
        title: 'Watch',
        titleHint: 'Capture the value of chosen cells on every run of their table, to see how an intermediate value changes.',
        add: 'Add',
        addHint: 'Add this cell to the watch list.',
        addPlaceholder: 'Cell name ($Factor) or ref (R2C3)',
        collect: 'Collect',
        collectHint: 'Run the trace to the end and record the value of each watched cell on every run of its table.',
        empty: 'No values captured — run a table that computes these cells.',
        truncated: 'Too many values — the series is capped and may miss late executions.',
        showing: 'Showing the first {{shown}} of {{total}} executions.',
        noValue: '—',
        replayHint: 'Replay: restart and run to this table to inspect it live',
    },

    tree: {
        title: 'Call tree',
        showDetailed: 'Show detailed trace',
        modeSimple: 'Tree',
        modeCallStack: 'Execution Path',
        runToHint: 'Run execution to here',
        resultHint: 'View result',
        replayHint: 'Replay — restart and run to this table to inspect it live',
        replayStepHint: 'Replay — restart and run to this step to inspect it live',
        timeTotal: 'Total',
        timeSelf: 'Self',
        dispatchTitle: 'Chosen from {{count}} versions by dimension properties:',
        referenceTag: 'ref',
        referenceHint: 'Uses a step that already executed — click to jump to it',
        passHint: 'Execution #{{n}} of this table in the run',
        more: '+{{count}} more executions',
        loading: 'Loading…',
        truncated: 'This tree is truncated for size — some branches are dropped. Hot Spots still count every call.',
        notRetained: '+{{count}} sub-calls not retained — tree too large. Hot Spots still count them.',
    },

    hotspots: {
        tab: 'Hot Spots',
        title: 'Hot spots',
        summary: '{{tables}} tables · {{invocations}} invocations · {{total}}',
        colTable: 'Table',
        colRuns: 'Runs',
        colSelf: 'Self',
        colTotal: 'Total',
        replayHint: 'Replay — restart and run to this table to inspect it live',
        more: 'Showing the {{shown}} slowest of {{total}} tables — every call is counted.',
        empty: 'Hot spots appear after a profiling run finishes. Turn on Profiling and run to the end.',
    },

    debug: {
        resume: 'Resume — run to the next breakpoint or the end',
        pause: 'Pause — stop at the next step',
        stepInto: 'Step into — go inside the next called rule',
        stepOver: 'Step over — run the next line without going inside',
        stepOut: 'Step out — finish this rule and return to its caller',
        rerun: 'Rerun — restart the trace from the beginning',
        profiling: 'Profiling',
        profilingHint: 'Keep the executed call tree (timings + replay). It uses more memory and runs slower, and switching restarts the trace.',
        profilingNotice: 'Profiling keeps the whole executed tree — it uses more memory and runs slower. Turn it off when you do not need the executed branches.',
        callStack: 'Execution Path',
        notSuspended: 'The calculation is not paused',
        runningNotice: 'Calculating… A large request can take a while. Use Pause to interrupt.',
        breakpoints: 'Breakpoints',
        breakpointsHint: 'A breakpoint pauses the run when a chosen table is about to execute, so you can inspect it. Add one by name below.',
        noBreakpoints: 'No breakpoints. Add one from a table below.',
        addBreakpoint: 'Add breakpoint',
        addBreakpointPlaceholder: 'Find a table by name…',
        removeBreakpoint: 'Remove breakpoint',
        steps: 'Steps',
        noSteps: 'No steps',
        executing: 'executing…',
        pending: 'pending',
        status: {
            pending: 'Starting…',
            running: 'Calculating',
            suspended: 'Paused',
            completed: 'Finished',
            error: 'Failed',
            terminated: 'Stopped',
        },
    },

    simple: {
        run: 'Run',
        advanced: 'Advanced',
        pressRun: 'Click Run to calculate and see every rule that executed.',
        calculating: 'Calculating…',
        preparing: 'Preparing the calculation tree… {{loaded}} of {{total}} rules',
        inspectHint: 'Click to see the values this rule received and the result it produced',
    },

    error: {
        inTable: 'in {{table}}',
        atLocation: 'at {{location}}',
        technicalDetails: 'Show technical details',
    },

    decision: {
        fired: 'Fired: {{rules}}',
        firedCount: 'Fired {{count}} rules: {{rules}}, …',
        showAllRules: 'Show all {{count}} rules',
        showFewer: 'Show fewer',
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
    },

    details: {
        parameters: 'Parameters',
        result: 'Result',
        errors: 'Errors',
        table: 'Traced table',
        decision: 'Decision',
        noSelection: 'Select a step to view its details',
        noParameters: 'No parameters',
        noResult: 'No result',
    },

    table: {
        truncated: 'Showing the first {{count}} of {{total}} rows. Open in Excel to see the full table.',
    },

    param: {
        loadValue: 'Load value',
    },

    loadingDetails: 'Loading details…',
    loadingTable: 'Loading table…',

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
        title: 'Trace execution',
        statuses: {
            started: 'Executing trace…',
        },
        errors: {
            startFailed: 'Failed to start trace',
        },
    },
})
