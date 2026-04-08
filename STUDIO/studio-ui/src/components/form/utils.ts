/**
 * Unwraps FieldObject values (e.g. { value: "...", readOnly: true }) for Form.Item getValueProps.
 * Prevents [object Object] from being displayed in form inputs when the backend returns
 * a wrapped field with readOnly or secret flags.
 *
 * @param clearSecret If true, returns undefined for secret fields (used by InputPassword).
 */
export function getFieldValueProps(val: unknown, options?: { clearSecret?: boolean }): { value: unknown } {
    if (val !== null && typeof val === 'object' && !Array.isArray(val)) {
        const obj = val as Record<string, unknown>
        if (options?.clearSecret && obj.secret) {
            return { value: undefined }
        }
        return { value: obj.value }
    }
    return { value: val }
}
