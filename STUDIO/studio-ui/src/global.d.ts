declare module '*.svg'
declare module '*.png'
declare module '*.jpg'
declare module '*.jpeg'
declare module '*.gif'

declare global {
    // Declared as a `var` so it surfaces on both `window` and `globalThis`.
    var openl: {
        projectStatus?: import('./services/projectStatus').ProjectStatusBridge
    } | undefined
}

interface FieldObject<T> {
    value: T,
    readOnly?: boolean,
    secure?: boolean,
}

type Field<T> = T | FieldObject<T>

declare type InputTextField = Field<string>

declare type InputNumberField = Field<number>

declare type InputBooleanField = Field<boolean>


export {
    InputTextField,
    InputNumberField,
    InputBooleanField,
}
