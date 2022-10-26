import org.openl.rules.ruleservice.core.annotations.ApiErrors
import org.openl.generated.beans.MyError

@ApiErrors(value = [MyError.class, ErrorWrapper.class])
interface RuleService {

}
