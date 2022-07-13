## Heroku applications
To deploy or redeploy these applications, run [Heroku App](https://github.com/openl-tablets/openl-tablets/actions/workflows/heroku.yml) action.
You can specify a branch, a tag or a sha-1 hash in the 'Ref to build' input field. If the value is not specified, then the HEAD of the branch will be used.

### These servers are for demoing:
* [DEMO](https://openl-demo.herokuapp.com)
* [WebStudio](https://openl-webstudio.herokuapp.com)
* [RuleServices](https://openl-ruleservices.herokuapp.com)
* [RuleServices (ALL)](https://openl-ruleservices-all.herokuapp.com)

Deploy on them the most stable branches.

### These servers are for development and testing:
* [DEV1](https://openl-dev1.herokuapp.com), [DEV2](https://openl-dev2.herokuapp.com), [DEV3](https://openl-dev3.herokuapp.com), [DEV4](https://openl-dev4.herokuapp.com),
* [QA1](https://openl-qa1.herokuapp.com), [QA2](https://openl-qa2.herokuapp.com), [QA3](https://openl-qa3.herokuapp.com), [QA4](https://openl-qa4.herokuapp.com),



## OpenL artifacts

To build Maven artifacts and docker images for the specified branch, you can use
[Deploy Maven artifacts](https://github.com/openl-tablets/openl-tablets/actions/workflows/deploy.yml) action.
This action uploads DEMO, WebStudio and RuleServices artifacts to the [GitHub packages](https://github.com/orgs/openl-tablets/packages?repo_name=openl-tablets).

