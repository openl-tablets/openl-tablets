import org.openl.generated.beans.MyError
import org.openl.rules.ruleservice.core.annotations.ApiErrors

@ApiErrors(value = [MyError.class, ErrorWrapper.class])
interface RuleService {

}
