import { describe, expect, it } from 'vitest'
import { EMPTY_DEPLOY_CONFIG, parseDeployConfig, serializeDeployConfig } from './rulesDeploy'
import { MalformedXmlError } from './xmlDescriptor'

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
        expect(() => parseDeployConfig('not xml <')).toThrow(MalformedXmlError)
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
        const reparsed = parseDeployConfig(serializeDeployConfig(parseDeployConfig(SAMPLE)))
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

        const out = serializeDeployConfig(parsed)
        expect(out).toContain('<configuration>')
        expect(out).toContain('<string>value</string>')
        expect(parseDeployConfig(out).configuration).toContain('<string>key</string>')
    })

    it('writes a configuration typed as text into the file as XML, not as escaped text', () => {
        // What the editor hands over: one line of markup, with an entity in an attribute and in a text, a
        // comment and a CDATA section.
        const typed = '<entry key="a &amp; b"><value>x &lt; y</value></entry><!-- note -->'
            + '<raw><![CDATA[1 < 2 && 3 > 2]]></raw>'
        const out = serializeDeployConfig({ ...EMPTY_DEPLOY_CONFIG, serviceName: 'svc', configuration: typed })

        expect(out).toBe(`<rules-deploy>
    <serviceName>svc</serviceName>
    <configuration>
        <entry key="a &amp; b">
            <value>x &lt; y</value>
        </entry>
        <!-- note -->
        <raw><![CDATA[1 < 2 && 3 > 2]]></raw>
    </configuration>
</rules-deploy>
`)
        // A second save of the file it wrote changes nothing: no markup is escaped on the way back in.
        expect(serializeDeployConfig(parseDeployConfig(out))).toBe(out)
    })

    it('preserves elements the editor does not manage', () => {
        const withCustom = `<rules-deploy>
    <serviceName>svc</serviceName>
    <lazyModulesForCompilationPatterns>keep-me</lazyModulesForCompilationPatterns>
</rules-deploy>`
        const out = serializeDeployConfig({ ...parseDeployConfig(withCustom), serviceName: 'renamed' })
        expect(out).toContain('<serviceName>renamed</serviceName>')
        expect(out).toContain('<lazyModulesForCompilationPatterns>keep-me</lazyModulesForCompilationPatterns>')
    })

    it('writes a fresh descriptor from an empty config', () => {
        const config = { ...EMPTY_DEPLOY_CONFIG, serviceName: 'new-svc', provideRuntimeContext: true, publishers: ['KAFKA']}
        const out = serializeDeployConfig(config)
        expect(out).toContain('<rules-deploy>')
        expect(out).toContain('<serviceName>new-svc</serviceName>')
        expect(out).toContain('<isProvideRuntimeContext>true</isProvideRuntimeContext>')
        expect(out).toContain('<publisher>KAFKA</publisher>')
        expect(parseDeployConfig(out).serviceName).toBe('new-svc')
    })

    it('leaves the runtime context out when it is off, the way a migrate leaves the descriptor', () => {
        // A descriptor that says `false` says what the engine assumes anyway. Written back, that line is what a
        // migrate would take away again, so the save would put the migrate on offer after every edit.
        const out = serializeDeployConfig({ ...parseDeployConfig(SAMPLE), serviceName: 'renamed' })
        expect(out).not.toContain('isProvideRuntimeContext')
        expect(out).toContain('<serviceName>renamed</serviceName>')
        expect(parseDeployConfig(out).provideRuntimeContext).toBe(false)
    })

    it('migrates a legacy interceptingTemplateClassName and drops the stale element', () => {
        const legacy = `<rules-deploy>
    <interceptingTemplateClassName>com.acme.Tpl</interceptingTemplateClassName>
</rules-deploy>`
        const out = serializeDeployConfig(parseDeployConfig(legacy))
        expect(out).toContain('<annotationTemplateClassName>com.acme.Tpl</annotationTemplateClassName>')
        expect(out).not.toContain('interceptingTemplateClassName')
    })

    it('round-trips the groups field', () => {
        const config = { ...EMPTY_DEPLOY_CONFIG, groups: 'ADMIN,USER' }
        const out = serializeDeployConfig(config)
        expect(out).toContain('<groups>ADMIN,USER</groups>')
        expect(parseDeployConfig(out).groups).toBe('ADMIN,USER')
    })

    it('escapes special characters in scalar values', () => {
        const config = { ...EMPTY_DEPLOY_CONFIG, url: 'a&b<c' }
        const out = serializeDeployConfig(config)
        expect(out).toContain('<url>a&amp;b&lt;c</url>')
        expect(parseDeployConfig(out).url).toBe('a&b<c')
    })

    it('rejects malformed configuration XML fragments', () => {
        const config = {
            ...EMPTY_DEPLOY_CONFIG,
            configuration: '</configuration><serviceName>evil</serviceName>',
        }

        expect(() => serializeDeployConfig(config)).toThrow(MalformedXmlError)
    })
})
