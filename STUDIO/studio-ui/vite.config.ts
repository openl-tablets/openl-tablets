import { readdirSync, readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { join } from 'node:path'
import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

const require = createRequire(import.meta.url)

/** The libraries bundled into the pages, with the texts of their licenses, for the About dialog. */
const LICENSES_FILE = 'licenses/frontend-licenses.json'

/** A library as `build.license` lists it, and as its `package.json` names it. */
interface Library {
    name: string
    version: string
}

/** The folder of the package a module belongs to: the one right under the last `node_modules`, as Vite takes it. */
const PACKAGE_ROOT = /^(.*[\\/]node_modules[\\/](?:@[^\\/]+[\\/])?[^\\/]+)[\\/]/

/**
 * Adds to every library of the list `build.license` writes the NOTICE its package ships (`notice`): the attribution a
 * license such as Apache-2.0 asks a redistribution to carry, which bundling leaves behind.
 *
 * Each of two versions of a package bundled gets the NOTICE of its own.
 */
export const libraryNotices = (): Plugin => ({
    name: 'library-notices',
    generateBundle: {
        // After the list is written.
        order: 'post',
        handler(_, bundle) {
            const list = bundle[LICENSES_FILE]
            if (list?.type !== 'asset') {
                return
            }
            const modules = Object.values(bundle).flatMap(output => (output.type === 'chunk' ? output.moduleIds : []))
            const roots = new Set(modules.flatMap(id => (id.startsWith('\0') ? [] : PACKAGE_ROOT.exec(id)?.[1] ?? [])))
            const notices = new Map<string, string>()
            for (const root of roots) {
                const file = readdirSync(root).find(name => /^notice(\.(md|txt))?$/i.test(name))
                if (file) {
                    const { name, version } = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8')) as Library
                    notices.set(`${name}@${version}`, readFileSync(join(root, file), 'utf8').trim())
                }
            }
            const libraries = JSON.parse(Buffer.from(list.source).toString()) as Library[]
            list.source = JSON.stringify(libraries.map(library => (
                { ...library, notice: notices.get(`${library.name}@${library.version}`) }
            )), null, 2)
        },
    },
})

/**
 * The decoder of HTML entities micromark imports draws on a DOM element in its browser build, and a worker has no
 * DOM: resolved the way Node resolves it, the package gives its own build without one.
 *
 * The pages take the same build, so they bundle every library the worker does: Vite lists the licenses of the
 * libraries the pages bundle, never of those only a worker bundles.
 */
const domlessEntityDecoder = (): Plugin => ({
    name: 'domless-entity-decoder',
    enforce: 'pre',
    resolveId: source => (source === 'decode-named-character-reference' ? require.resolve(source) : null),
})

export default defineConfig({
    base: './',
    plugins: [react(), domlessEntityDecoder(), libraryNotices()],
    resolve: {
        tsconfigPaths: true,
    },
    server: {
        port: 3100,
        warmup: {
            clientFiles: ['./src/index.tsx', './src/App.tsx'],
        },
        proxy: {
            '^/ws$': {
                target: 'ws://localhost:8080',
                ws: true,
                changeOrigin: true,
                headers: {
                    Origin: 'http://localhost:8080'
                }
            },
            '/rest': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/login': {
                target: 'http://localhost:8080',
                bypass: req => req.method !== 'POST' ? req.url : undefined
            },
            '/logout' : {
                target: 'http://localhost:8080'
            },
            // The lists of third-party libraries the About dialog reads: the backend serves both.
            '/licenses': {
                target: 'http://localhost:8080',
            },
            // A file of the user guides comes from the backend; a page of a guide is a screen of the application.
            '/docs': {
                target: 'http://localhost:8080',
                bypass: req => (/\.[^/]*$/.test(req.url?.split(/[?#]/)[0] ?? '') ? undefined : req.url),
            },
        },
    },
    // The search of the user guides parses the pages in a worker.
    worker: {
        plugins: () => [domlessEntityDecoder()],
    },
    build: {
        sourcemap: true,
        manifest: true,
        // `libraryNotices` adds the NOTICE of each library.
        license: {
            fileName: LICENSES_FILE,
        },
        rollupOptions: {
            // The API documentation is a page of its own, read without logging in, so it is built as one.
            input: {
                index: './index.html',
                'api-docs': './api-docs.html',
            },
        },
    },
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./vitest.setup.ts'],
        // The full `npm run test` (coverage on) is what CI and the Maven build run, concurrently with the
        // parallel Maven reactor. The CPU starvation plus the coverage instrumentation stretch a heavy
        // real-antd integration test several times over; the slowest ones type through `userEvent.setup({
        // delay: null })` to stay near a second, and this ceiling is the margin for the machine being busy.
        // `test:watch` (no coverage) keeps the fast 5s fail that catches an accidental hang while iterating.
        testTimeout: process.env.CI === 'true' || process.argv.includes('--coverage') ? 20_000 : 5_000,
        // A test file shares its worker with the next one, so a global it stubs (localStorage, for
        // instance) is restored between files instead of leaking into them.
        unstubGlobals: true,
        coverage: {
            reporter: ['lcov', 'text-summary']
        }
    }
})
