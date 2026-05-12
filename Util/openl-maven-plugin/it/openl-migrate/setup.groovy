/*
 * Initialises a fresh local git repository in the copied IT project so the openl:migrate goal can create
 * real commits per migrator (one commit per phase). The verify.groovy reads `git log` and asserts that the
 * configured comment template (prefix + message + version) ends up in every commit subject.
 */
try {
    File folder = basedir

    def run = { String... args ->
        def proc = args.toList().execute(null, folder)
        proc.consumeProcessOutput(System.out, System.err)
        def exit = proc.waitFor()
        assert exit == 0 : "Command failed (exit ${exit}): ${args.join(' ')}"
    }

    run('git', 'init', '-b', 'main')
    run('git', 'config', 'user.name', 'openl-migrate-test')
    run('git', 'config', 'user.email', 'openl-migrate-test@example.com')
    run('git', 'config', 'commit.gpgsign', 'false')
    run('git', 'add', '.')
    run('git', 'commit', '-m', 'initial: imported IT fixture')

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
