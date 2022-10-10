import {nodeResolve} from "@rollup/plugin-node-resolve"
import typescript from "rollup-plugin-ts"
import {lezer} from "@lezer/generator/rollup"

export default {
    input: "./jsmodules/text-editor.ts",
    output: {
        file: "./target/webapp/javascript/text-editor.bundle.js",
        format: "iife",
        name: "textEditor"
    },
    plugins: [nodeResolve(), lezer(), typescript()]
}
