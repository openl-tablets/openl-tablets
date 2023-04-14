if (Boolean.valueOf(System.getProperty("noPerf"))) {
    return true
}

def finished = System.currentTimeMillis()
def clock = (finished - (long) context.started)

File folder = basedir
for (def test: folder.getParentFile().listFiles()) {
    if (!test.isDirectory() || test.getName().startsWith("_")) {
        // Skip processing not folders (files, links and etc.)
        continue
    }
    println("Processing: " + test)
    def propsFile = new File(test, "invoker.properties")
    def props = new Properties()
    if (propsFile.exists()) {
        props.load (propsFile.newInputStream());
    }
    if (props.containsKey("openl.perf.mavenOpts")) {
        props.setProperty("invoker.mavenOpts", props.getProperty("openl.perf.mavenOpts"))
        props.remove("openl.perf.mavenOpts")
    }

    def poms = 0;
    test.eachFileRecurse (groovy.io.FileType.FILES) { file ->
        if (file.getName() == "pom.xml") {
            poms++;
        }
    }

    def timeout = poms * 5 + 10 // 5 seconds per file plus 10 seconds over
    def timeoutPlus = props.getProperty("openl.perf.timeoutPlus")
    if (timeoutPlus != null) {
        timeout += Integer.parseInt(timeoutPlus)
    }

    timeout *= (clock / 1500) // Adjust for performance of the server, 2000 is the base time

    timeout = Math.max(timeout.toInteger(), 10);

    props.setProperty("invoker.timeoutInSeconds", timeout.toString())

    var text = ("  Timeout: " + timeout + " for " + poms + " pom.xml files and clock=" + clock + "ms")
    println(text)
    props.store(propsFile.newOutputStream(), text)
}

return true
