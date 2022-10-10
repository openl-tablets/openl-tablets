import {basicSetup} from "codemirror"
import {EditorView, showPanel} from "@codemirror/view"
import {StateEffect, StateField, Compartment, Extension} from "@codemirror/state"
import {xml} from "@codemirror/lang-xml"
import {properties} from "./text-editor-languages/properties-file-plugin"

const dirtyState = StateEffect.define<boolean>()
const highlightScheme = StateEffect.define<string>()
const languageConf = new Compartment

enum HighlightScheme {
    NONE,
    XML,
    PROPERTIES
}

function toolbarState(config) {
    return StateField.define<boolean>({
        create: () => false,
        update(value, transaction) {
            for (let e of transaction.effects) {
                if (e.is(dirtyState)) {
                    value = e.value
                }
            }
            return value
        },
        provide: field => showPanel.from(field, value => createToolbar(config, value))
    })
}

function statusBarState(initialScheme) {
    return StateField.define<string>({
        create: () => initialScheme,
        update(value, transaction) {
            for (let e of transaction.effects) if (e.is(highlightScheme)) value = e.value
            return value
        },
        provide: field => showPanel.from(field, value => createStatusBar(initialScheme))
    })
}

function createStatusBar(initialScheme: HighlightScheme) {
    return view => {
        let dom = document.createElement("div")
        dom.innerHTML = `<div class="text_editor_status_bar">
            <span>Highlight scheme:</span>
            <select class="selectHighlight">
                <option ${initialScheme == HighlightScheme.NONE ? "selected='true'" : ""}>None</option>
                <option ${initialScheme == HighlightScheme.XML ? "selected='true'" : ""}>Xml</option>
                <option ${initialScheme == HighlightScheme.PROPERTIES ? "selected='true'" : ""}>Properties</option>
            </select>
         </div> `
        dom.querySelector(".selectHighlight").addEventListener("change", (ev: Event) => {
            let select = ev.currentTarget as HTMLSelectElement
            if (select.selectedOptions && select.selectedOptions.length > 0) {
                let selectedOption = select.selectedOptions.item(0)
                let highlightExtension = getHighlightPlugin(selectedOption.index)
                view.dispatch({
                    effects: languageConf.reconfigure(highlightExtension)
                })
            }
        })
        return {dom}
    }
}

function getHighlightPlugin(scheme: HighlightScheme) {
    switch (scheme) {
        case HighlightScheme.XML: return xml()
        case HighlightScheme.PROPERTIES: return properties()
        default: return []
    }
}

function createToolbar(config, value) {
    return view => {
        let dom = document.createElement("div")
        dom.innerHTML = `<div class="te_toolbar">
            <img src="${config.contextPath}/faces/tableEditor/img/Save.gif" title="Save" class="save_button te_toolbar_item ${value ? '' : 'te_toolbar_item_disabled'}"/>
         </div> `
        dom.querySelector(".save_button").addEventListener("click", () => {
            let documentIterator = view.state.doc.iter()
            let content = "";
            while (!documentIterator.done) {
                content += documentIterator.value
                documentIterator = documentIterator.next()
            }
            return window.fetch(config.contextPath + "/web/text-editor/file", {
                method: "POST",
                // headers: new Headers({'content-type': 'application/octet-stream'}),
                body: content,
            }).then((response => {
                if (response.ok) {
                    view.dispatch({effects: dirtyState.of(false)});
                    if (config.onAfterSave) {
                        config.onAfterSave(response)
                    }
                } else {
                    throw new Error(response.statusText)
                }
            })).catch(error => {
                if (config.onError) {
                    config.onError(error);
                }
            })
        })
        return {top: true, dom}
    }
}

const textEditorTheme = EditorView.baseTheme({
    ".text_editor_status_bar": {
        padding: "5px 10px",
    }
})

let dirtyStateListener = EditorView.updateListener.of(update => {
    if (update.docChanged) {
        update.view.dispatch({
            effects: dirtyState.of(true)
        })
    }
})

function tooltip(config): Extension {
    return [toolbarState(config), dirtyStateListener];
}

export function initCodeMirror(element: Element, fileExtension: string, config) {
    let initialScheme = HighlightScheme.NONE
    switch (fileExtension) {
        case "xml":
            initialScheme = HighlightScheme.XML;
            break;
        case "properties":
            initialScheme = HighlightScheme.PROPERTIES
            break;
    }
    return window.fetch(`${config.contextPath}/web/text-editor/file`)
        .then((response: Response) => {
            if (response.ok) {
                return response.text()
            } else {
                throw new Error(response.statusText)
            }
        }).then((text) =>
            new EditorView({
                doc: text,
                extensions: [basicSetup, languageConf.of(getHighlightPlugin(initialScheme)), tooltip(config),
                    statusBarState(initialScheme), textEditorTheme],
                parent: element
            })
        ).catch((error: TypeError) => {
            if (config.onError) {
                config.onError(error);
            }
        });
}
