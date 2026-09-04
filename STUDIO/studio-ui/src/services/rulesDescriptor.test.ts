import { describe, expect, it } from 'vitest'
import { EMPTY_RULES_DESCRIPTOR, parseRulesDescriptor, serializeRulesDescriptor } from './rulesDescriptor'

describe('parseRulesDescriptor', () => {
    it('reads the declared text of rules.xml', () => {
        const descriptor = parseRulesDescriptor(`
            <project>
                <name>Pricing</name>
                <comment>A pricing ruleset</comment>
                <modules>
                    <module>
                        <rules-root path="rules/*.xlsx"/>
                        <method-filter>
                            <includes><value>calc*</value><value>rate*</value></includes>
                            <excludes><value>debug*</value></excludes>
                        </method-filter>
                    </module>
                </modules>
                <properties-file-name-pattern>%lob%-%state%</properties-file-name-pattern>
                <properties-file-name-pattern>Tests-*</properties-file-name-pattern>
                <properties-file-name-processor>com.acme.Processor</properties-file-name-processor>
                <exposed-methods>
                    <include>SayHello</include>
                    <exclude>secret*</exclude>
                </exposed-methods>
                <openapi>
                    <path>openapi.json</path>
                    <model-module-name>Model</model-module-name>
                    <algorithm-module-name>Algo</algorithm-module-name>
                    <mode>GENERATION</mode>
                </openapi>
            </project>
        `)

        expect(descriptor.description).toBe('A pricing ruleset')
        expect(descriptor.versionPatterns).toEqual(['%lob%-%state%', 'Tests-*'])
        expect(descriptor.propertiesFileNameProcessor).toBe('com.acme.Processor')
        expect(descriptor.exposedMethods).toEqual({ includes: ['SayHello'], excludes: ['secret*']})
        expect(descriptor.openapi).toEqual({ path: 'openapi.json', mode: 'GENERATION', modelModuleName: 'Model', algorithmModuleName: 'Algo' })
        // Each declared module carries its own method filter.
        expect(descriptor.moduleDeclarations).toEqual([
            { name: '', path: 'rules/*.xlsx', methodFilter: { includes: ['calc*', 'rate*'], excludes: ['debug*']} },
        ])
    })

    it('reads an empty descriptor from a blank, malformed, or foreign file', () => {
        expect(parseRulesDescriptor('')).toEqual(EMPTY_RULES_DESCRIPTOR)
        expect(parseRulesDescriptor('<project><comment>x')).toEqual(EMPTY_RULES_DESCRIPTOR)
        expect(parseRulesDescriptor('<rules-deploy/>')).toEqual(EMPTY_RULES_DESCRIPTOR)
    })

    it('leaves out the parts the file does not declare', () => {
        const descriptor = parseRulesDescriptor('<project><name>Bare</name></project>')

        expect(descriptor.description).toBe('')
        expect(descriptor.versionPatterns).toEqual([])
        expect(descriptor.propertiesFileNameProcessor).toBeUndefined()
        expect(descriptor.exposedMethods).toBeUndefined()
        expect(descriptor.openapi).toBeUndefined()
        expect(descriptor.moduleDeclarations).toEqual([])
    })
})

describe('serializeRulesDescriptor', () => {
    it('rewrites the managed elements and keeps the modules, classpath and unknowns verbatim', () => {
        const original = `
            <project>
                <name>Pricing</name>
                <modules>
                    <module><rules-root path="rules/Main.xlsx"/></module>
                </modules>
                <dependencies><dependency><name>Old Dep</name></dependency></dependencies>
                <comment>old</comment>
                <properties-file-name-pattern>old-*</properties-file-name-pattern>
            </project>
        `
        // The UI edits a parsed descriptor, so the modules it did not touch are carried in it.
        const out = serializeRulesDescriptor({
            ...parseRulesDescriptor(original),
            description: 'new & shiny',
            versionPatterns: ['%lob%-%state%', 'Tests-*'],
            sources: ['groovy/', 'lib/*.jar'],
            dependencies: [{ name: 'Common', autoIncluded: true }],
            exposedMethods: { includes: ['calc*'], excludes: ['debug*']},
        }, original)

        // Managed elements reflect the edit.
        expect(out).toContain('<comment>new &amp; shiny</comment>')
        expect(out).toContain('<properties-file-name-pattern>%lob%-%state%</properties-file-name-pattern>')
        expect(out).toContain('<properties-file-name-pattern>Tests-*</properties-file-name-pattern>')
        expect(out).toContain('<include>calc*</include>')
        expect(out).toContain('<exclude>debug*</exclude>')
        expect(out).not.toContain('old-*')
        // The declared classpath and dependencies are rewritten from the edit, not the old ones.
        expect(out).toContain('<entry path="groovy/"/>')
        expect(out).toContain('<entry path="lib/*.jar"/>')
        expect(out).toContain('<name>Common</name>')
        expect(out).toContain('<autoIncluded>true</autoIncluded>')
        expect(out).not.toContain('Old Dep')
        // The modules are carried over.
        expect(out).toContain('<name>Pricing</name>')
        expect(out).toContain('rules/Main.xlsx')

        // The result round-trips through the reader.
        const reparsed = parseRulesDescriptor(out)
        expect(reparsed.description).toBe('new & shiny')
        expect(reparsed.versionPatterns).toEqual(['%lob%-%state%', 'Tests-*'])
        expect(reparsed.sources).toEqual(['groovy/', 'lib/*.jar'])
        expect(reparsed.dependencies).toEqual([{ name: 'Common', autoIncluded: true }])
        expect(reparsed.exposedMethods).toEqual({ includes: ['calc*'], excludes: ['debug*']})
    })

    it('rewrites the declared modules and keeps each module method filter', () => {
        const out = serializeRulesDescriptor({
            ...EMPTY_RULES_DESCRIPTOR,
            moduleDeclarations: [
                { name: 'Main', path: 'rules/Main.xlsx' },
                { name: 'Rules', path: 'rules/**/*.xlsx', methodFilter: { includes: ['calc*'], excludes: []} },
            ],
        }, '<project><name>P</name></project>')

        expect(out).toContain('<rules-root path="rules/Main.xlsx"/>')
        expect(out).toContain('<rules-root path="rules/**/*.xlsx"/>')
        expect(out).toContain('<method-filter>')
        expect(out).toContain('<includes>')
        expect(out).toContain('<value>calc*</value>')

        const reparsed = parseRulesDescriptor(out)
        expect(reparsed.moduleDeclarations).toEqual([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'Rules', path: 'rules/**/*.xlsx', methodFilter: { includes: ['calc*'], excludes: []} },
        ])
    })

    it('keeps a module child it does not manage, so an edit never drops it', () => {
        const original = `
            <project>
                <name>P</name>
                <modules>
                    <module>
                        <name>Main</name>
                        <rules-root path="rules/Main.xlsx"/>
                        <webstudio-configuration><table-properties/></webstudio-configuration>
                    </module>
                </modules>
            </project>
        `
        // A blank field is edited; the module and its unmanaged webstudio-configuration are untouched.
        const out = serializeRulesDescriptor({ ...parseRulesDescriptor(original), description: 'now set' }, original)

        expect(out).toContain('<comment>now set</comment>')
        expect(out).toContain('<rules-root path="rules/Main.xlsx"/>')
        // The child the reader does not model survives the round-trip verbatim.
        expect(out).toContain('<webstudio-configuration><table-properties/></webstudio-configuration>')
        expect(parseRulesDescriptor(out).moduleDeclarations[0]!.preserved)
            .toEqual(['<webstudio-configuration><table-properties/></webstudio-configuration>'])
    })

    it('drops a managed element that was cleared', () => {
        const out = serializeRulesDescriptor(EMPTY_RULES_DESCRIPTOR, '<project><comment>gone</comment></project>')

        expect(out).not.toContain('comment')
        expect(out.trim()).toBe('<project>\n</project>')
    })
})
