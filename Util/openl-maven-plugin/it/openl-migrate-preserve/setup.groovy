/*
 * Normalises the descriptor line endings before openl:migrate runs.
 *
 * The migrators rewrite a descriptor only when its bytes differ from the marshalled model, and the
 * marshaller always ends lines with LF. A checkout with core.autocrlf=true (the Windows default) hands
 * over CRLF files, so every migrator would report a rewrite it did not actually make and the fixture
 * would no longer prove that a canonical descriptor is left untouched.
 */
try {
    File folder = basedir

    ['rules.xml', 'rules-deploy.xml'].each { name ->
        def file = new File(folder, name)
        def normalized = file.getText('UTF-8').replace('\r\n', '\n')
        file.write(normalized, 'UTF-8')
    }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
