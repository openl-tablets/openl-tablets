import org.openl.rules.ruleservice.core.interceptors.RulesType
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor

interface RuleService {

    @ServiceCallAfterInterceptor(ResponseAdapter.class)
    @RulesType("Pong")
    Object doPing(@RulesType("Ping") Object ping)

}
