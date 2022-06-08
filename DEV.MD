## Heroku applications
[![DEMO](https://img.shields.io/website?label=DEMO&url=https%3A%2F%2Fopenl-demo.herokuapp.com)](https://openl-demo.herokuapp.com)
[![WebStudio](https://img.shields.io/website?label=WebStudio&url=https%3A%2F%2Fopenl-webstudio.herokuapp.com)](https://openl-webstudio.herokuapp.com)
[![RuleServices](https://img.shields.io/website?label=RuleServices&url=https%3A%2F%2Fopenl-ruleservices.herokuapp.com)](https://openl-ruleservices.herokuapp.com)
[![RuleServices (ALL)](https://img.shields.io/website?label=RuleServices(ALL)&url=https%3A%2F%2Fopenl-ruleservices-all.herokuapp.com)](https://openl-ruleservices-all.herokuapp.com)

To deploy or redeploy these applications, run [Heroku App](https://github.com/openl-tablets/openl-tablets/actions/workflows/heroku.yml) action.
You can specify a branch, a tag or a sha-1 hash in the 'Ref to build' input field. If the value is not specified, then the HEAD of the branch will be used.

## OpenL artifacts

To build Maven artifacts and docker images for the specified branch, you can use
[Deploy Maven artifacts](https://github.com/openl-tablets/openl-tablets/actions/workflows/deploy.yml) action.
This action uploads DEMO, WebStudio and RuleServices artifacts to the [GitHub packages](https://github.com/orgs/openl-tablets/packages?repo_name=openl-tablets).

