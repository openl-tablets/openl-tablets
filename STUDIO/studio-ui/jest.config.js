/** @type {import('jest').Config} */
module.exports = {
    preset: 'ts-jest',
    testEnvironment: 'jsdom',
    roots: ['<rootDir>/src', '<rootDir>/test'],
    testMatch: ['**/*.test.ts', '**/*.test.tsx'],
    moduleNameMapper: {
        '^@/(.*)$': '<rootDir>/src/$1',
    },
    setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
    moduleDirectories: ['node_modules', 'src'],
}
