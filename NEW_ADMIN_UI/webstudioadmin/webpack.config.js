const HtmlWebpackPlugin = require('html-webpack-plugin')
const path = require('path')
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')
const Dotenv = require('dotenv-webpack')
const ModuleFederationPlugin = require('@module-federation/enhanced').ModuleFederationPlugin

module.exports = {
    mode: 'development',
    entry: './src/index',
    output: {
        publicPath: 'http://localhost:3100/',
    },
    devServer: {
        static: {
            directory: path.join(__dirname, 'dist'),
        },
        port: 3100,
        // proxy: {
        //     '/api': {
        //         target: 'http://localhost:8080/',
        //         pathRewrite: { '^/api': '/webstudio/rest' },
        //     },
        // },
        historyApiFallback: true,
        client: {
            overlay: false,
        },
    },
    resolve: {
        extensions: ['.ts', '.tsx', '.js'],
        plugins: [new TsconfigPathsPlugin()],
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                loader: 'babel-loader',
                exclude: /node_modules/,
                options: {
                    presets: ['@babel/preset-react', '@babel/preset-typescript'],
                },
            },
            {
                test: /\.s[ac]ss$/i,
                use: [
                    // Creates `style` nodes from JS strings
                    'style-loader',
                    // Translates CSS into CommonJS
                    'css-loader',
                    // Compiles Sass to CSS
                    'sass-loader',
                ],
            },
        ],
    },
    plugins: [
        new Dotenv(),
        new ModuleFederationPlugin({
            name: 'webstudio_ui',
            filename: 'webstudio.js',
            shared: {
                '@ant-design/icons': { singleton: true },
                'antd': { singleton: true },
                'i18next': { singleton: true },
                'react': { singleton: true },
                'react-dom': { singleton: true },
                'react-final-form': { singleton: true },
                'react-i18next': { singleton: true },
            },
            exposes: {
                './apiCall': './src/services/apiCall',
                './form': './src/components/form',
                './config': './src/services/config',
                './store': './src/store',
            },
        }),
        new HtmlWebpackPlugin({
            template: './public/index.html',
        }),
    ],
}
