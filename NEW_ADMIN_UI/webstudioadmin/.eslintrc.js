module.exports = {
    'root': true,
    'plugins': [
        '@stylistic/jsx'
    ],
    'parser': '@typescript-eslint/parser',
    'extends': [
        'react-app',
        'react-app/jest',
        'plugin:react-hooks/recommended'
    ],
    'rules': {
        'indent': [ 'error', 4 ],
        'quotes': [ 'error', 'single', { 'avoidEscape': true }],
        'jsx-quotes': [ 'error', 'prefer-double' ],
        'object-curly-spacing': [ 'error', 'always', { 'arraysInObjects': false, 'objectsInObjects': true }],
        'array-bracket-spacing': [ 'error', 'always', { 'arraysInArrays': false, 'objectsInArrays': false }],
        'computed-property-spacing': [ 'error', 'always' ],
        'no-extra-semi': 'error',
        'semi-spacing': 'error',
        'comma-spacing': [ 'error', { 'before': false, 'after': true }],
        'semi': [ 'error', 'never' ],
        'no-console': [ 'error', { 'allow': [ 'warn', 'error' ]}],
        'comma-dangle': [ 'error', {
            'arrays': 'only-multiline',
            'objects': 'only-multiline',
            'imports': 'only-multiline',
            'exports': 'only-multiline',
            'functions': 'never',
        }],
        // JSX
        '@stylistic/jsx/jsx-child-element-spacing': [ 'error' ],
        '@stylistic/jsx/jsx-closing-bracket-location': [ 'error', 'line-aligned' ],
        '@stylistic/jsx/jsx-closing-tag-location': 'error',
        '@stylistic/jsx/jsx-curly-brace-presence': [ 'error', { 'props': 'never', 'children': 'never', 'propElementValues': 'always' }],
        '@stylistic/jsx/jsx-curly-newline': [ 'error', { 'multiline': 'consistent', 'singleline': 'consistent' }],
        '@stylistic/jsx/jsx-curly-spacing': [ 2, 'never' ],
        '@stylistic/jsx/jsx-equals-spacing': [ 'error', 'never' ],
        '@stylistic/jsx/jsx-first-prop-new-line': [ 'error', 'multiline' ],
        '@stylistic/jsx/jsx-indent': [ 'error', 4, { 'checkAttributes': true, 'indentLogicalExpressions': true }],
        '@stylistic/jsx/jsx-indent-props': [ 'error', 4 ],
        '@stylistic/jsx/jsx-max-props-per-line': [ 'error', { 'maximum': 1, 'when': 'multiline' }],
        '@stylistic/jsx/jsx-newline': [ 'error', { 'prevent': true }],
        '@stylistic/jsx/jsx-one-expression-per-line': [ 'error', { 'allow': 'single-child' }],
        '@stylistic/jsx/jsx-props-no-multi-spaces': 'error',
        '@stylistic/jsx/jsx-self-closing-comp': [ 'error', { 'component': true, 'html': true }],
        '@stylistic/jsx/jsx-sort-props': [ 'warn', {
            'callbacksLast': true,
            'shorthandFirst': true,
            'shorthandLast': false,
            'multiline': 'ignore',
            'ignoreCase': true,
            'noSortAlphabetically': false,
            'reservedFirst': true,
            'locale': 'auto'
        }],
        '@stylistic/jsx/jsx-tag-spacing': [ 'error', { 'beforeSelfClosing': 'always' }],
        '@stylistic/jsx/jsx-wrap-multilines': [ 'warn', {
            'declaration': 'parens',
            'assignment': 'parens',
            'return': 'parens',
            'arrow': 'parens',
            'condition': 'ignore',
            'logical': 'ignore',
            'prop': 'ignore'
        }],
    },
}