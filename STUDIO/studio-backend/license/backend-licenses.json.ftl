<#--
  The third-party libraries the war ships, in JSON, for the About dialog of studio-ui.

  The entries take the shape Vite writes the frontend libraries in, with the address of the license besides:
  {"name": "groupId:artifactId", "version": "...", "identifier": "SPDX expression", "text": "the LICENSE of the jar",
  "url": "license address", "notice": "the NOTICE of the jar"}.

  dependencyMap: entries of a library (a MavenProject) and the names of its licenses (a String array), each an SPDX
  identifier once the licenseMerges of the POM are applied.

  The build runs a copy of the template beside files/, where it unpacks the META-INF LICENSE and NOTICE of every jar by
  the path of the jar in a repository.
-->
<#-- The first of the files named that a library ships in META-INF, or an empty string. -->
<#function fileOf p files>
    <#local folder = "files/${p.groupId?replace('.', '/')}/${p.artifactId}/${p.version}/META-INF/"/>
    <#list files as name>
        <#local file = .get_optional_template(folder + name, {"parse": false})/>
        <#if file.exists>
            <#local text><@file.include/></#local>
            <#return text?trim/>
        </#if>
    </#list>
    <#return ""/>
</#function>
[
<#list dependencyMap as e>
<#assign p = e.getKey()/>
<#assign names = e.getValue()/>
<#assign text = fileOf(p, ["LICENSE", "LICENSE.txt", "LICENSE.md", "license.txt"])/>
<#assign notice = fileOf(p, ["NOTICE", "NOTICE.txt", "NOTICE.md", "notice.txt"])/>
    {
        "name": "${(p.groupId + ":" + p.artifactId)?json_string}",
        "version": "${p.version?json_string}"<#if names?has_content>,
        "identifier": "${names?join(" OR ")?json_string}"</#if><#if text?has_content>,
        "text": "${text?json_string}"</#if><#if p.licenses?has_content && p.licenses[0].url?has_content>,
        "url": "${p.licenses[0].url?trim?json_string}"</#if><#if notice?has_content>,
        "notice": "${notice?json_string}"</#if>
    }<#sep>,</#sep>
</#list>
]
