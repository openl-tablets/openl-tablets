import reactHooks from 'eslint-plugin-react-hooks'
import perfectionist from 'eslint-plugin-perfectionist'
import stylistic from '@stylistic/eslint-plugin'
import tseslint from 'typescript-eslint'

export default [
    {
        files: ['./src/**/*.{js,jsx,ts,tsx}'],
        ignores: ['./dist/**', './node_modules/**'],
        languageOptions: {
            parser: tseslint.parser,
            parserOptions: {
                ecmaVersion: 'latest',
                sourceType: 'module',
                ecmaFeatures: { jsx: true },
            },
        },
        plugins: {
            '@stylistic': stylistic,
            'react-hooks': reactHooks,
            '@typescript-eslint': tseslint.plugin,
            'perfectionist': perfectionist,
        },
        rules: {
            'no-unused-vars': 'off',
            '@typescript-eslint/no-unused-vars': ['warn', {
                vars: 'all',
                args: 'after-used',
                ignoreRestSiblings: true,
                varsIgnorePattern: '^_',
                argsIgnorePattern: '^_',
                caughtErrorsIgnorePattern: '^_',
            }],
            '@stylistic/indent': ['error', 4, { 'SwitchCase': 1 }],
            '@stylistic/quotes': ['error', 'single', { 'avoidEscape': true }],
            '@stylistic/jsx-quotes': ['error', 'prefer-double'],
            '@stylistic/object-curly-spacing': ['error', 'always', { 'arraysInObjects': false, 'objectsInObjects': true }],
            '@stylistic/array-bracket-spacing': ['error', 'never', { 'arraysInArrays': false, 'objectsInArrays': false }],
            '@stylistic/computed-property-spacing': ['error', 'never'],
            '@stylistic/no-extra-semi': 'error',
            '@stylistic/semi-spacing': 'error',
            '@stylistic/comma-spacing': ['error', { 'before': false, 'after': true }],
            '@stylistic/semi': ['error', 'never'],
            'no-console': 'error',
            'react-hooks/rules-of-hooks': 'error',
            'react-hooks/exhaustive-deps': 'warn',
            // Static Ant Design pop-ups render outside React and ignore the theme — use services/popups
            'no-restricted-imports': ['error', {
                'paths': [{
                    'name': 'antd',
                    'importNames': ['notification', 'message'],
                    'message': 'Static pop-ups ignore the theme; take them from App.useApp().',
                }],
            }],
            'no-restricted-syntax': ['error', {
                'selector': "MemberExpression[object.name='Modal']"
                    + "[property.name=/^(confirm|info|success|error|warning)$/]",
                'message': 'Static dialogs ignore the theme; take modal from App.useApp().',
            }],
            '@stylistic/comma-dangle': ['error', {
                'arrays': 'only-multiline',
                'objects': 'only-multiline',
                'imports': 'only-multiline',
                'exports': 'only-multiline',
                'functions': 'never',
                'enums': 'only-multiline',
                'tuples': 'only-multiline',
                // `<T,>` is how a .tsx file tells a generic arrow function from a JSX tag
                'generics': 'ignore',
            }],
            // JSX stylistic rules
            '@stylistic/jsx-child-element-spacing': ['error'],
            '@stylistic/jsx-closing-bracket-location': ['error', 'line-aligned'],
            '@stylistic/jsx-closing-tag-location': 'error',
            '@stylistic/jsx-curly-brace-presence': ['error', { 'props': 'never', 'children': 'never', 'propElementValues': 'always' }],
            '@stylistic/jsx-curly-newline': ['error', { 'multiline': 'consistent', 'singleline': 'consistent' }],
            '@stylistic/jsx-curly-spacing': [2, 'never'],
            '@stylistic/jsx-equals-spacing': ['error', 'never'],
            '@stylistic/jsx-first-prop-new-line': ['error', 'multiline'],
            '@stylistic/jsx-indent-props': ['error', 4],
            '@stylistic/jsx-max-props-per-line': ['error', { 'maximum': 1, 'when': 'multiline' }],
            '@stylistic/jsx-newline': ['error', { 'prevent': true }],
            '@stylistic/no-multi-spaces': 'error',
            '@stylistic/jsx-self-closing-comp': ['error', { 'component': true, 'html': true }],
            'perfectionist/sort-jsx-props': ['warn', {
                'type': 'alphabetical',
                'order': 'asc',
                'ignoreCase': true,
                'groups': ['reserved', 'shorthand-prop', 'unknown', 'multiline-prop'],
                'customGroups': [
                    { 'groupName': 'reserved', 'elementNamePattern': '^(?:key|ref)$' },
                ],
            }],
            '@stylistic/jsx-tag-spacing': ['error', { 'beforeSelfClosing': 'always' }],
            '@stylistic/jsx-wrap-multilines': ['warn', {
                'declaration': 'parens',
                'assignment': 'parens',
                'return': 'parens',
                'arrow': 'parens',
                'condition': 'ignore',
                'logical': 'ignore',
                'prop': 'ignore'
            }],
        },
    },
    {
        // A component or a hook reaches the pop-ups through App.useApp(); the bridge is for services and stores
        files: ['./src/{components,containers,hooks,layouts,pages,providers}/**/*.{ts,tsx}'],
        ignores: ['./src/**/*.test.{ts,tsx}'],
        rules: {
            'no-restricted-imports': ['error', {
                'paths': [
                    {
                        'name': 'antd',
                        'importNames': ['notification', 'message'],
                        'message': 'Static pop-ups ignore the theme; take them from App.useApp().',
                    },
                    {
                        'name': 'services/popups',
                        'message': 'A component or a hook takes notification and modal from App.useApp().',
                    },
                ],
            }],
        },
    },
    {
        // The bridge wraps the static pop-ups, and tests stub them
        files: ['./src/services/popups.tsx', './src/**/*.test.{ts,tsx}'],
        rules: {
            'no-restricted-imports': 'off',
            'no-restricted-syntax': 'off',
        },
    },
    {
        // The central error logger is the only sanctioned console call site
        files: ['./src/utils/errorHandling.ts'],
        rules: {
            'no-console': 'off',
        },
    },
]
