import js from '@eslint/js'
import reactHooks from 'eslint-plugin-react-hooks'
import stylisticJsx from '@stylistic/eslint-plugin-jsx'
import tsParser from '@typescript-eslint/parser'
import globals from 'globals'
import tsPlugin from '@typescript-eslint/eslint-plugin'

export default [
    {
        files: ['./src/**/*.{js,jsx,ts,tsx}'],
        ignores: ['./dist/**', './node_modules/**'],
        languageOptions: {
            parser: tsParser,
            parserOptions: {
                ecmaVersion: 'latest',
                sourceType: 'module',
                ecmaFeatures: { jsx: true },
                project: './tsconfig.json',
            },
            globals: {
                ...globals.browser
            },
        },
        plugins: {
            '@stylistic/jsx': stylisticJsx,
            'react-hooks': reactHooks,
            '@typescript-eslint': tsPlugin,
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
            'indent': ['error', 4, { 'SwitchCase': 1 }],
            'quotes': ['error', 'single', { 'avoidEscape': true }],
            'jsx-quotes': ['error', 'prefer-double'],
            'object-curly-spacing': ['error', 'always', { 'arraysInObjects': false, 'objectsInObjects': true }],
            'array-bracket-spacing': ['error', 'never', { 'arraysInArrays': false, 'objectsInArrays': false }],
            'computed-property-spacing': ['error', 'never'],
            'no-extra-semi': 'error',
            'semi-spacing': 'error',
            'comma-spacing': ['error', { 'before': false, 'after': true }],
            'semi': ['error', 'never'],
            'no-console': ['warn', { 'allow': ['warn', 'error']}],
            'comma-dangle': ['error', {
                'arrays': 'only-multiline',
                'objects': 'only-multiline',
                'imports': 'only-multiline',
                'exports': 'only-multiline',
                'functions': 'never',
            }],
            // JSX stylistic rules
            '@stylistic/jsx/jsx-child-element-spacing': ['error'],
            '@stylistic/jsx/jsx-closing-bracket-location': ['error', 'line-aligned'],
            '@stylistic/jsx/jsx-closing-tag-location': 'error',
            '@stylistic/jsx/jsx-curly-brace-presence': ['error', { 'props': 'never', 'children': 'never', 'propElementValues': 'always' }],
            '@stylistic/jsx/jsx-curly-newline': ['error', { 'multiline': 'consistent', 'singleline': 'consistent' }],
            '@stylistic/jsx/jsx-curly-spacing': [2, 'never'],
            '@stylistic/jsx/jsx-equals-spacing': ['error', 'never'],
            '@stylistic/jsx/jsx-first-prop-new-line': ['error', 'multiline'],
            '@stylistic/jsx/jsx-indent': ['error', 4, { 'checkAttributes': true, 'indentLogicalExpressions': true }],
            '@stylistic/jsx/jsx-indent-props': ['error', 4],
            '@stylistic/jsx/jsx-max-props-per-line': ['error', { 'maximum': 1, 'when': 'multiline' }],
            '@stylistic/jsx/jsx-newline': ['error', { 'prevent': true }],
            '@stylistic/jsx/jsx-props-no-multi-spaces': 'error',
            '@stylistic/jsx/jsx-self-closing-comp': ['error', { 'component': true, 'html': true }],
            '@stylistic/jsx/jsx-sort-props': ['warn', {
                'callbacksLast': false,
                'shorthandFirst': true,
                'shorthandLast': false,
                'multiline': 'last',
                'ignoreCase': true,
                'noSortAlphabetically': false,
                'reservedFirst': true,
                'locale': 'auto'
            }],
            '@stylistic/jsx/jsx-tag-spacing': ['error', { 'beforeSelfClosing': 'always' }],
            '@stylistic/jsx/jsx-wrap-multilines': ['warn', {
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
]
