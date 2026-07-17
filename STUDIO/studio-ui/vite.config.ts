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
        // CI runs vitest concurrently with the parallel Maven build, which starves the workers
        // of CPU, so antd-heavy tests overrun the default 5s timeout
        testTimeout: process.env.CI === 'true' ? 15_000 : 5_000,
        coverage: {
            reporter: ['lcov', 'text-summary']
        }
    }
})
