// Client-side mapping between the OpenL `rules-deploy.xml` deployment descriptor and a flat form model.
// The file is edited through the generic Files API (GET/PUT). Parsing is DOM-based; serialization keeps
// any elements the form does not manage (e.g. the `configuration` map), so unknown settings survive edits.

import { escapeXml } from '../utils/escapeXml'
import { childValue, childValues, directChild, parseXmlRoot, unmanagedChildren } from './xmlDescriptor'

export interface DeployConfig {
    serviceName: string
    url: string
    version: string
    serviceClass: string
    annotationTemplateClassName: string
    provideRuntimeContext: boolean
    groups: string
    publishers: string[]
    /** Raw inner XML of the `<configuration>` element (custom deployment settings), edited as text. */
    configuration: string
}

/** Publisher types the deployment engine understands (RulesDeploy.PublisherType). */
export const PUBLISHER_TYPES = ['RESTFUL', 'KAFKA'] as const

export const EMPTY_DEPLOY_CONFIG: DeployConfig = {
    serviceName: '',
    url: '',
    version: '',
    serviceClass: '',
    annotationTemplateClassName: '',
    provideRuntimeContext: false,
    groups: '',
    publishers: [],
    configuration: '',
}

const ROOT = 'rules-deploy'
// Elements the form owns and rewrites on save. `interceptingTemplateClassName` is managed so a legacy file
// does not keep a stale duplicate once its value has been migrated into `annotationTemplateClassName`.
const MANAGED = new Set([
    'serviceName', 'url', 'version', 'serviceClass', 'annotationTemplateClassName',
    'interceptingTemplateClassName', 'isProvideRuntimeContext', 'groups', 'publishers', 'configuration',
])

export class DeployConfigParseError extends Error {
    constructor() {
        super('Malformed rules-deploy.xml')
        this.name = 'DeployConfigParseError'
    }
}

export class DeployConfigConfigurationError extends Error {
    constructor() {
        super('Malformed rules-deploy.xml configuration block')
        this.name = 'DeployConfigConfigurationError'
    }
}

/** The serialized inner XML of an element (its children), trimmed. */
const innerXml = (element: Element): string => {
    const serializer = new XMLSerializer()
    return Array.from(element.childNodes).map(node => serializer.serializeToString(node)).join('').trim()
}

/** Parse a rules-deploy descriptor into the form model. A blank descriptor starts from an empty config. */
const rootOf = (xml: string): Element | null => {
    if (!xml.trim()) {
        return null
    }
    const root = parseXmlRoot(xml, ROOT)
    if (!root) {
        throw new DeployConfigParseError()
    }
    return root
}

const validConfigurationFragment = (configuration: string): string => {
    const trimmed = configuration.trim()
    if (!trimmed) {
        return ''
    }
    const doc = new DOMParser().parseFromString(`<configuration>${trimmed}</configuration>`, 'application/xml')
    const root = doc.documentElement
    if (!root || doc.getElementsByTagName('parsererror').length > 0 || root.tagName !== 'configuration') {
        throw new DeployConfigConfigurationError()
    }
    return trimmed
}

export function parseDeployConfig(xml: string): DeployConfig {
    const root = rootOf(xml)
    if (!root) {
        return { ...EMPTY_DEPLOY_CONFIG }
    }
    const text = (tag: string) => childValue(root, tag)
    const publishers = directChild(root, 'publishers')
    const configuration = directChild(root, 'configuration')
    return {
        serviceName: text('serviceName'),
        url: text('url'),
        version: text('version'),
        serviceClass: text('serviceClass'),
        // A legacy descriptor may carry interceptingTemplateClassName; fall back to it (mirrors the JSF editor).
        annotationTemplateClassName: text('annotationTemplateClassName') || text('interceptingTemplateClassName'),
        provideRuntimeContext: text('isProvideRuntimeContext') === 'true',
        groups: text('groups'),
        publishers: publishers ? childValues(publishers, 'publisher') : [],
        configuration: configuration ? innerXml(configuration) : '',
    }
}

/**
 * Serialize the form model back to a rules-deploy descriptor. Elements the editor does not manage are
 * carried over verbatim from the original document so no existing settings are lost.
 */
export function serializeDeployConfig(config: DeployConfig, originalXml: string): string {
    let root: Element | null
    try {
        root = rootOf(originalXml)
    } catch {
        return originalXml
    }
    // Reuse the document already parsed above rather than parsing the same string a second time.
    const preserved = root ? unmanagedChildren(root, MANAGED).map(child => `    ${child}`) : []

    const lines = ['<rules-deploy>']
    lines.push(`    <isProvideRuntimeContext>${config.provideRuntimeContext}</isProvideRuntimeContext>`)
    const scalar = (tag: string, value: string) => {
        const trimmed = value.trim()
        if (trimmed) {
            lines.push(`    <${tag}>${escapeXml(trimmed)}</${tag}>`)
        }
    }
    scalar('serviceName', config.serviceName)
    scalar('serviceClass', config.serviceClass)
    scalar('annotationTemplateClassName', config.annotationTemplateClassName)
    if (config.publishers.length > 0) {
        lines.push('    <publishers>')
        for (const publisher of config.publishers) {
            lines.push(`        <publisher>${escapeXml(publisher)}</publisher>`)
        }
        lines.push('    </publishers>')
    }
    scalar('url', config.url)
    scalar('version', config.version)
    scalar('groups', config.groups)
    const configuration = validConfigurationFragment(config.configuration)
    if (configuration) {
        lines.push('    <configuration>')
        lines.push(configuration)
        lines.push('    </configuration>')
    }
    lines.push(...preserved)
    lines.push('</rules-deploy>')
    return `${lines.join('\n')}\n`
}
