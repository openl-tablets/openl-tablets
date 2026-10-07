<#--
  The third-party libraries the war ships, in JSON, for the About dialog of studio-ui.

  The entries take the shape Vite writes the frontend libraries in, with the address of the license instead of its text:
  {"name": "groupId:artifactId", "version": "...", "identifier": "license names", "url": "license address"}.

  dependencyMap: entries of a library (a MavenProject) and the names of its licenses (a String array).
-->
[
<#list dependencyMap as e>
<#assign p = e.getKey()/>
<#assign names = e.getValue()/>
    {
        "name": "${(p.groupId + ":" + p.artifactId)?json_string}",
        "version": "${p.version?json_string}"<#if names?has_content>,
        "identifier": "${names?join(" OR ")?json_string}"</#if><#if p.licenses?has_content
            && p.licenses[0].url?has_content>,
        "url": "${p.licenses[0].url?trim?json_string}"</#if>
    }<#sep>,</#sep>
</#list>
]
