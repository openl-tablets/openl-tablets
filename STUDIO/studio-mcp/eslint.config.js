import tsParser from '@typescript-eslint/parser'
import tsPlugin from '@typescript-eslint/eslint-plugin'
import stylistic from '@stylistic/eslint-plugin'

export default [
    {
        files: ['**/*.ts'],
        languageOptions: {
            parser: tsParser,
            parserOptions: {
                ecmaVersion: 2025,
                sourceType: 'module',
                project: './tsconfig.json',
            },
        },
        plugins: {
            '@typescript-eslint': tsPlugin,
            '@stylistic': stylistic,
        },
        rules: {
            // The formatting of studio-ui, its JSX rules aside
            '@stylistic/indent': ['error', 4, { 'SwitchCase': 1 }],
            '@stylistic/quotes': ['error', 'single', { 'avoidEscape': true }],
            // `{ a }` and `[a]` on one line; a list that wraps puts each item on a line of its own.
            // Parentheses and angle brackets are left as written.
            '@stylistic/list-style': ['error', { 'overrides': { '()': 'off', '<>': 'off' } }],
            '@stylistic/computed-property-spacing': ['error', 'never'],
            '@stylistic/no-extra-semi': 'error',
            '@stylistic/semi-spacing': 'error',
            '@stylistic/comma-spacing': ['error', { 'before': false, 'after': true }],
            '@stylistic/semi': ['error', 'never'],
            // The members of a type as studio-ui writes them: a line each without a delimiter, or commas on one line
            '@stylistic/member-delimiter-style': ['error', {
                'multiline': { 'delimiter': 'none' },
                'singleline': { 'delimiter': 'comma', 'requireLast': false },
            }],
            '@stylistic/no-multi-spaces': 'error',
            '@stylistic/comma-dangle': ['error', {
                'arrays': 'only-multiline',
                'objects': 'only-multiline',
                'imports': 'only-multiline',
                'exports': 'only-multiline',
                'functions': 'never',
                'enums': 'only-multiline',
                'tuples': 'only-multiline',
                'generics': 'ignore',
            }],
            '@typescript-eslint/no-explicit-any': 'warn', // A warning, not an error: the few remaining any usages are deliberate
            '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', ignoreRestSiblings: true }],
            '@typescript-eslint/explicit-function-return-type': [
                'error',
                {
                    allowExpressions: true,
                    allowTypedFunctionExpressions: true,
                    allowHigherOrderFunctions: true,
                },
            ],
            '@typescript-eslint/no-non-null-assertion': 'warn',
            'no-console': ['warn', { allow: ['error', 'warn'] }],
        },
    },
    {
        // Test files exercise edge shapes on purpose: casting a malformed payload,
        // asserting a value the type system cannot know is present. Enforcing the
        // production rules here would trade real coverage for ceremony.
        files: ['src/**/*.test.ts', 'src/testing/**/*.ts'],
        rules: {
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-non-null-assertion': 'off',
            '@typescript-eslint/explicit-function-return-type': 'off',
        },
    },
    {
        ignores: ['dist/**', 'node_modules/**', 'coverage/**', '*.js'],
    },
]
