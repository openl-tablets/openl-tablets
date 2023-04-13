import org.apache.cxf.jaxrs.utils.HttpUtils

class OpenLUtils {

    static String dateToString(Date date) {
        return HttpUtils.toHttpDate(date)
    }

}
