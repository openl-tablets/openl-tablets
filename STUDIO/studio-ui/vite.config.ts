import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    base: './',
    plugins: [react()],
    resolve: {
        tsconfigPaths: true,
    },
    server: {
        port: 3100,
        warmup: {
            clientFiles: ['./src/index.tsx', './src/App.tsx'],
        },
        proxy: {
            '/web/ws': {
                target: 'ws://localhost:8080',
                ws: true,
                changeOrigin: true,
                headers: {
                    Origin: 'http://localhost:8080'
                }
            },
            '/web': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/login': {
                target: 'http://localhost:8080',
                bypass: req => req.method !== 'POST' ? req.url : undefined
            },
            '/logout' : {
                target: 'http://localhost:8080'
            }
        },
    },
    build: {
        sourcemap: true,
        manifest: true,
    },
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./vitest.setup.ts'],
        // The full `npm run test` (coverage on) is what CI and the Maven build run, both concurrently
        // with the parallel Maven reactor: the CPU starvation plus the coverage instrumentation makes
        // antd-heavy tests overrun the default 5s. `test:watch` (no coverage) keeps the fast 5s fail.
        testTimeout: process.env.CI === 'true' || process.argv.includes('--coverage') ? 15_000 : 5_000,
        // A test file shares its worker with the next one, so a global it stubs (localStorage, for
        // instance) is restored between files instead of leaking into them.
        unstubGlobals: true,
        coverage: {
            reporter: ['lcov', 'text-summary']
        }
    }
})
