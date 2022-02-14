import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler

import java.lang.reflect.Method

class CsvFileInterceptor implements ServiceExtraMethodHandler<Object> {

    @Override
    Object invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        def source = (InputStream) Objects.requireNonNull(args[1])
        List<String> rows = new ArrayList<>()
        try (def scanner = new Scanner(source)) {
            while (scanner.hasNext()) {
                def line = scanner.nextLine()
                rows.add(line)
            }
        }
        def response = new Response()
        response.json = (Request) args[0]
        response.csv = rows.toArray(new String[0])
        return response
    }

}
