/// <reference types="react-scripts" />

declare global {
    interface Window {
        // Global runtime variables
    }
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
