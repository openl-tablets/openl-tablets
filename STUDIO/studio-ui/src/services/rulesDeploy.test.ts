import { describe, expect, it } from 'vitest'
import {
    DeployConfigConfigurationError,
    DeployConfigParseError,
    EMPTY_DEPLOY_CONFIG,
    parseDeployConfig,
    serializeDeployConfig,
} from './rulesDeploy'

const SAMPLE = `<rules-deploy>
    <isProvideRuntimeContext>false</isProvideRuntimeContext>
    <serviceName>example-simple</serviceName>
    <publishers>
        <publisher>RESTFUL</publisher>
    </publishers>
    <url>example-simple</url>
</rules-deploy>`

describe('parseDeployConfig', () => {
    it('reads scalar, boolean, and publisher fields', () => {
        const config = parseDeployConfig(SAMPLE)
        expect(config.serviceName).toBe('example-simple')
        expect(config.url).toBe('example-simple')
        expect(config.provideRuntimeContext).toBe(false)
        expect(config.publishers).toEqual(['RESTFUL'])
    })

    it('returns an empty config for blank XML', () => {
        expect(parseDeployConfig('')).toEqual(EMPTY_DEPLOY_CONFIG)
    })

    it('rejects malformed XML instead of treating it as a new descriptor', () => {
        expect(() => parseDeployConfig('not xml <')).toThrow(DeployConfigParseError)
    })

    it('reads groups and falls back from a legacy interceptingTemplateClassName', () => {
        const config = parseDeployConfig(`<rules-deploy>
    <interceptingTemplateClassName>com.acme.Tpl</interceptingTemplateClassName>
    <groups>ADMIN, USER</groups>
</rules-deploy>`)
        expect(config.annotationTemplateClassName).toBe('com.acme.Tpl')
        expect(config.groups).toBe('ADMIN, USER')
    })
})

describe('serializeDeployConfig', () => {
    it('round-trips the sample through parse and serialize', () => {
        const reparsed = parseDeployConfig(serializeDeployConfig(parseDeployConfig(SAMPLE), SAMPLE))
        expect(reparsed).toEqual(parseDeployConfig(SAMPLE))
    })

    it('parses and round-trips the configuration (XML) block', () => {
        const withConfig = `<rules-deploy>
    <serviceName>svc</serviceName>
    <configuration>
        <entry>
            <string>key</string>
            <string>value</string>
        </entry>
    </configuration>
</rules-deploy>`
        const parsed = parseDeployConfig(withConfig)
        expect(parsed.configuration).toContain('<entry>')
        expect(parsed.configuration).toContain('<string>key</string>')

        const out = serializeDeployConfig(parsed, withConfig)
        expect(out).toContain('<configuration>')
        expect(out).toContain('<string>value</string>')
        expect(parseDeployConfig(out).configuration).toContain('<string>key</string>')
    })

    it('preserves elements the editor does not manage', () => {
        const withCustom = `<rules-deploy>
    <serviceName>svc</serviceName>
    <lazyModulesForCompilationPatterns>keep-me</lazyModulesForCompilationPatterns>
</rules-deploy>`
        const out = serializeDeployConfig(parseDeployConfig(withCustom), withCustom)
        expect(out).toContain('<lazyModulesForCompilationPatterns>keep-me</lazyModulesForCompilationPatterns>')
    })

    it('writes a fresh descriptor when there is no original', () => {
        const config = { ...EMPTY_DEPLOY_CONFIG, serviceName: 'new-svc', provideRuntimeContext: true, publishers: ['KAFKA']}
        const out = serializeDeployConfig(config, '')
        expect(out).toContain('<rules-deploy>')
        expect(out).toContain('<serviceName>new-svc</serviceName>')
        expect(out).toContain('<isProvideRuntimeContext>true</isProvideRuntimeContext>')
        expect(out).toContain('<publisher>KAFKA</publisher>')
        expect(parseDeployConfig(out).serviceName).toBe('new-svc')
    })

    it('migrates a legacy interceptingTemplateClassName and drops the stale element', () => {
        const legacy = `<rules-deploy>
    <interceptingTemplateClassName>com.acme.Tpl</interceptingTemplateClassName>
</rules-deploy>`
        const out = serializeDeployConfig(parseDeployConfig(legacy), legacy)
        expect(out).toContain('<annotationTemplateClassName>com.acme.Tpl</annotationTemplateClassName>')
        expect(out).not.toContain('interceptingTemplateClassName')
    })

    it('round-trips the groups field', () => {
        const config = { ...EMPTY_DEPLOY_CONFIG, groups: 'ADMIN,USER' }
        const out = serializeDeployConfig(config, '')
        expect(out).toContain('<groups>ADMIN,USER</groups>')
        expect(parseDeployConfig(out).groups).toBe('ADMIN,USER')
    })

    it('escapes special characters in scalar values', () => {
        const config = { ...EMPTY_DEPLOY_CONFIG, url: 'a&b<c' }
        const out = serializeDeployConfig(config, '')
        expect(out).toContain('<url>a&amp;b&lt;c</url>')
        expect(parseDeployConfig(out).url).toBe('a&b<c')
    })

    it('rejects malformed configuration XML fragments', () => {
        const config = {
            ...EMPTY_DEPLOY_CONFIG,
            configuration: '</configuration><serviceName>evil</serviceName>',
        }

        expect(() => serializeDeployConfig(config, '')).toThrow(DeployConfigConfigurationError)
    })

    it('preserves malformed original XML verbatim if serialization is called', () => {
        const original = '<rules-deploy><serviceName>svc</rules-deploy>'

        expect(serializeDeployConfig({ ...EMPTY_DEPLOY_CONFIG, serviceName: 'new' }, original)).toBe(original)
    })
})
