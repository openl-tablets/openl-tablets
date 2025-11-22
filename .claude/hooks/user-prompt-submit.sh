#!/bin/bash
# Claude Code pre-prompt hook for OpenL Tablets
# This script runs before each user prompt is processed

# Get the current working directory
CWD=$(pwd)

# Detect which module the user is working in
if [[ $CWD == */DEV/* ]]; then
    echo "📍 Context: DEV module - Core rule engine"
    echo "💡 Tip: See .cursor/rules/dev-module.mdc for patterns"
elif [[ $CWD == */STUDIO/* ]]; then
    echo "📍 Context: STUDIO module - WebStudio IDE"
    echo "💡 Tip: See .cursor/rules/studio-module.mdc for patterns"
elif [[ $CWD == */WSFrontend/* ]]; then
    echo "📍 Context: WSFrontend module - Rule Services"
    echo "💡 Tip: See .cursor/rules/wsfrontend-module.mdc for patterns"
fi

# Check if there are uncommitted changes
if git diff --quiet 2>/dev/null; then
    :  # No uncommitted changes, do nothing
else
    echo "⚠️  You have uncommitted changes"
fi

# Return success
exit 0
