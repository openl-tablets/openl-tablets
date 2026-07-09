import React from 'react'
import { Help } from 'containers/Help'
import { ProjectPage } from 'containers/ProjectPage'

/**
 * Maps a `data-island` marker to the React component rendered into that placeholder.
 * Legacy JSF fragments carry `<div data-island="<name>" data-*>`; {@link JsfIslandHost} portals
 * the matching component into each placeholder, passing the element's dataset as props source.
 */
export const ISLAND_REGISTRY: Record<string, (dataset: DOMStringMap) => React.ReactNode> = {
    'project-page': (dataset) => <ProjectPage projectId={dataset['projectId'] ?? ''} />,
    help: () => <Help />,
}
