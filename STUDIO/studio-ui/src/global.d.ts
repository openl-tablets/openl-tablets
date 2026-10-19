declare module '*.svg'
declare module '*.png'
declare module '*.jpg'
declare module '*.jpeg'
declare module '*.gif'

/**
 * A setting the server wraps: one an environment variable sets, which cannot be changed here (`readOnly`), or a secret
 * one, whose value is not shown (`secret`). A request takes the value alone.
 */
interface FieldObject<T> {
    value: T,
    readOnly?: boolean,
    secret?: boolean,
}

/** A setting as the server sends it: the value alone, or wrapped with its flags. */
type Field<T> = T | FieldObject<T>

declare type InputTextField = Field<string>

declare type InputNumberField = Field<number>

declare type InputBooleanField = Field<boolean>


export {
    Field,
    InputTextField,
    InputNumberField,
    InputBooleanField,
}
