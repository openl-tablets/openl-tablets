// Client-side mapping between the OpenL `rules-deploy.xml` deployment descriptor and a flat form model.
// The file is edited through the generic Files API (GET/PUT). The model is read out of the XML and
// written back by the mapping below; elements the form does not manage survive edits as they stand.

import { emptyOf, MalformedXmlError, parseXmlRoot, readXml, writeXml, type XmlMapping } from './xmlDescriptor'

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
    /** Serialized elements the form does not manage, kept as declared and written back verbatim. */
    preserved: string[]
}

/** Publisher types the deployment engine understands (RulesDeploy.PublisherType). */
export const PUBLISHER_TYPES = ['RESTFUL', 'KAFKA'] as const

/**
 * How the model is kept in the descriptor, in the order the elements are written.
 *
 * <p>The descriptor is written the way a migrate leaves it, so a save never puts the migrate back on offer:
 * the runtime context is left out when off — written out, `false` is the line a migrate takes away — and a
 * template class a legacy descriptor names the old way is written back under the current name only.
 */
const MAPPING: XmlMapping<DeployConfig> = {
    root: 'rules-deploy',
    fields: {
        provideRuntimeContext: { kind: 'flag', tag: 'isProvideRuntimeContext' },
        serviceName: { kind: 'text', tag: 'serviceName' },
        serviceClass: { kind: 'text', tag: 'serviceClass' },
        annotationTemplateClassName: {
            kind: 'text',
            tag: 'annotationTemplateClassName',
            formerly: ['interceptingTemplateClassName'],
        },
        publishers: { kind: 'list', wrapper: 'publishers', item: 'publisher' },
        url: { kind: 'text', tag: 'url' },
        version: { kind: 'text', tag: 'version' },
        groups: { kind: 'text', tag: 'groups' },
        configuration: { kind: 'xml', tag: 'configuration' },
        preserved: { kind: 'rest' },
    },
}

export const EMPTY_DEPLOY_CONFIG: DeployConfig = emptyOf(MAPPING)

/**
 * Parse a rules-deploy descriptor into the form model. A blank descriptor starts from an empty config; one
 * that does not parse, or is not a rules-deploy descriptor, is refused with a {@link MalformedXmlError}.
 */
export function parseDeployConfig(xml: string): DeployConfig {
    if (!xml.trim()) {
        return EMPTY_DEPLOY_CONFIG
    }
    const root = parseXmlRoot(xml, MAPPING.root)
    if (!root) {
        throw new MalformedXmlError('rules-deploy.xml')
    }
    return readXml(MAPPING, root)
}

/** Serialize the form model back to a rules-deploy descriptor, the elements the form does not manage included. */
export const serializeDeployConfig = (config: DeployConfig): string => writeXml(MAPPING, config)
