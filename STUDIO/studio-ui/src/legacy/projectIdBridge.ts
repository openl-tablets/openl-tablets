/**
 * Bridge that exposes project-id encoding to the legacy JSF/RichFaces pages running in the same
 * document. The bridge is a side-effect import — pulling it into the app sets
 * {@code globalThis.openl.encodeProjectId} once. Readiness is announced by the {@code openl:ready}
 * event dispatched from {@code ./index}.
 *
 * <p>A legacy page needs this to address a project by id in a REST URL. Encoding it there by hand is
 * what broke every non-ASCII name, so the one implementation the React screens use is published here
 * instead of being copied into a page script.
 */
import { encodeProjectId } from '../services/projectId'

/** Shape published to {@code globalThis.openl.encodeProjectId} for legacy JSF callers. */
export type EncodeProjectId = (repositoryId: string, projectName: string) => string

globalThis.openl = globalThis.openl ?? {}
globalThis.openl.encodeProjectId = encodeProjectId
