import {parser} from "./properties-files.grammar"
import {LRLanguage, LanguageSupport, indentNodeProp, foldNodeProp, foldInside, delimitedIndent} from "@codemirror/language"
import {styleTags, tags as t} from "@lezer/highlight"
import {tags} from "@lezer/highlight"
import {HighlightStyle, syntaxHighlighting} from "@codemirror/language"

export const propertiesFileLang = LRLanguage.define({
  parser: parser.configure({
    props: [
      styleTags({
        Key: t.variableName,
        Value: t.string,
        LineComment: t.lineComment,
      })
    ]
  }),
  languageData: {
    commentTokens: {line: ";"}
  }
})


const myHighlightStyle = HighlightStyle.define([
  {tag: tags.variableName, color: "red"},
  {tag: tags.lineComment, color: "#f5d", fontStyle: "italic"},
  {tag: tags.string, color: "blue"}
])


export function properties() {
  return [new LanguageSupport(propertiesFileLang), syntaxHighlighting(myHighlightStyle)]
}
