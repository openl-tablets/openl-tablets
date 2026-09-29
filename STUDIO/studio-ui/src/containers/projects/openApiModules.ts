import type { DescriptorOpenApi } from '../../services/rulesDescriptor'

/**
 * Whether the two names are one module name, letter case aside.
 *
 * <p>The rules and the data types are generated into a module each, and a workbook named after one module would
 * be the workbook of the other wherever letter case is not told apart. A name not given yet is none of them.
 */
export const namesOneModule = (algorithmModuleName: string | undefined, modelModuleName: string | undefined) => {
    const algorithm = algorithmModuleName?.trim().toLowerCase() ?? ''
    return algorithm !== '' && algorithm === (modelModuleName?.trim().toLowerCase() ?? '')
}

/**
 * Whether the settings generate the rules and the data types into modules of one name.
 *
 * <p>Only generation writes modules: a specification the project is reconciled against names none.
 */
const generatesIntoOneModule = (openapi: DescriptorOpenApi | undefined) =>
    openapi?.mode === 'GENERATION' && namesOneModule(openapi.algorithmModuleName, openapi.modelModuleName)

/**
 * Whether the edited settings generate the rules and the data types into modules of one name, where the saved
 * settings did not.
 *
 * <p>Only an edit that makes the names one is refused, as the server refuses it: a project that already saved them
 * so keeps its other settings editable, and the Generate tables dialog tells what stands in the way.
 */
export const makesOneModule = (saved: DescriptorOpenApi | undefined, edited: DescriptorOpenApi | undefined) =>
    generatesIntoOneModule(edited) && !generatesIntoOneModule(saved)
