Table of Contents
=================

   * [Table of Contents](#table-of-contents)
   * [Mule SonarQube Plugin](#mule-sonarqube-plugin)
      * [SonarQube Concepts](#sonarqube-concepts)
         * [Issues](#issues)
         * [Quality Profiles](#quality-profiles)
         * [Measures](#Measures)
         * [Quality Gates](#quality-gates)
      * [Plugin Aspects](#plugin-aspects)
         * [Rules](#rules)
         * [Measures](#measures)
      * [Configuration](#Configuration)
         * [Server](#server)
            * [Plugin](#plugin)  
         * [Project](#project)
      * [Execution](#execution)
        * [Analyzing](#analyzing)
        * [Results](#results)
        * [Try it Out](#try-it-out)
      * [Final Notes](#final-notes)

# Mule SonarQube Plugin
A plugin has been created to validate Mule applications code (Configuration Files) using SonarQube. 
This plugin contains a set of rules and metrics that are going to used and calculated every time a project is being inspected.
For more information, about SonarQube please refer to: https://www.sonarqube.org/

This is an [UNLICENSED software, please review the considerations](UNLICENSE.md). If you need assistance for extending this, contact MuleSoft Professional Services

## SonarQube Concepts
### Issues
Is the way to identify and classify different aspects or exceptions of the code.
Currently, there are three type of issues:
- **Bug**
- **Vulnerability**
- **Code Smell**

And each issue could have different severities:
- **BLOCKER**
- **CRITICAL**
- **MAJOR**
- **MINOR**
- **INFO**

For more information, please refer to https://docs.sonarqube.org/latest/user-guide/issues/

### Quality Profiles
Quality profiles are collections of rules defined for each language that is going to be executed during the inspection of the code.
The plugin defines one Language, Mule, and two profiles, one for each version of mule. Each one of these profiles defines a set of rules that can be updated easily.

![qualityprofiles](/img/quality-profiles.png)

### Measures
Depending on the language, different metrics are being calculated. Issues are one type of metric, but you will have more information about the project, information such as, for example, Size, Complexity, and Coverage.

For more information, please refer to https://docs.sonarqube.org/latest/user-guide/metric-definitions/

### Quality Gates
With them, you could set up your quality control and enforce it on all your projects. You could say that you will not deploy an app with less than 60% of coverage or with more than 3 Code Smell. 
This quality control could be easily added to your CI/CD process to, for example, allow or not the deployment of your app.

For more information, please refer to https://docs.sonarqube.org/latest/user-guide/quality-gates/

## Plugin Aspects

### Rules
They are created based on a file, named rule store. In this rule store, you could define:
-**rulesets**: A way to group the rules by a category. The category itself could be anything, for example, categories like application, configuration, api, or batch could be used. When inspecting a particular project, you could specify which categories (rules) you want to inspect by running for example, mvn sonar:sonar -Dsonar.mule.ruleset.categories=flows,configuration,api
-**rules**: the rule itself, with these attributes:
- id: a sequential number inside of the ruleset
- name: meaningful name of rule
- description: a more complete identification of the rule. It supports HTML
- severity: the severity of the rule. Possible values are:
    - BLOCKER
    - CRITICAL
    - MAJOR
    - MINOR
    - INFO
- type: the type of the issue that is going to be raised. Possible values are:
    - code_smell
    - bug
    - vulnerability
- applies: is the scope of the rule, how is it going to be applied, if the rule is going to be validated against a single file or the entire project. For example, if you want to validate that your application is using APIKIT you need to validate the rule throw all the files of the project. Possible values are:
    - file
    - application
- content: xpath expresion to be evaluated.

Currently, there are two files (rule stores), one per each mule runtime version (3|4).
For example, the rule store (rules-4.xml) has three rulesets (categories):
- application: it encapsulates rules related to the application itself. Examples of these are: 
    - Validate APIKIT is being used. (1)
    - Validate APIKIT Exception strategy has been set. (2)
- flows: it encapsulates rules related to the flows itself. Examples of these are:
    - Validate configuration files don't have more than a specific number of flows in them. (1)
    - Validate flow names follow a naming convention. (3)
    - Validate encryption key is not being logged. (6)
- configuration: it encapsulates rules related to the configuration of the different global elements. Examples of these are:
    - Validate configuration properties are being used. (1)
    - Validate Secure Configuration doesn't use a hardcoded encryption key. (2)
    - Validate autodiscovery is being used. (3)
    - Validate dataweave transformations are being stored in external files. (5,6)
    - Validate HTTPS is being used. (7)
    - Validate HTTP Configuration has the corresponding port placeholders. (8)
    - Validate autodiscovery configuration is not being hardcoded. (9)

**Rule Store Example**
```xml
<rulestore type="mule4">
    <ruleset category="application">
        <rule id="1"
            name="Application should have used APIKit to auto-generate the implementation interface"
            description="Application should have used APIKit to auto-generate the implementation interface"
            severity="MAJOR" applies="application" type="code_smell">
            count(//mule:mule/apikit:config)>0
        </rule>
  </ruleset>
    <ruleset category="flows">
        <rule id="1"
            name="Configuration files should not have so many flows"
            description="Configuration files should not have so many flows"
            severity="MAJOR" type="code_smell">
            not(count(//mule:mule/mule:flow)>=10)
        </rule>
    </ruleset>
    <ruleset category="configuration">
        <rule id="1"
            name="Data Transformations should be stored in external DWL Files - Payload"
            description="Data Transformations should be stored in external DWL Files - Payload"
            severity="MINOR" type="code_smell">
            count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload)=0
            or
            matches(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload/@resource,'^.*dwl$')
        </rule>
        <rule id="2"
			name="Mule Credentials Vault should not use a hardcoded encryption key"
			description="Mule Credentials Vault should not use a hardcoded encryption key"
			severity="MAJOR" type="bug">
			count(//mule:mule/secure-properties:config)=0
			or
			is-configurable(//mule:mule/secure-properties:config/@key)
		</rule>
    </ruleset>
</rulestore>
```
### Measures
The plugin handles different types of metrics, such as:
- Number of Flows 
- Number of SubFlows
- Number of DW Transformations
- Complexity. It is calculated based on the number of flows in a single file.
    - Simple: Less or equal than seven
    - Medium: More than seven and less than fifteen
    - Complex: More than fifteen.
- Coverage. To be able to see it, you need to configure json format in Munit Report. https://docs.mulesoft.com/munit/2.2/coverage-maven-concept
- Number of MUnit Tests

## Configuration
### Server
As the plugin inspects xml files and SonarQube already comes with an XML plugin, you have to modify this behavior so only one plugin inspects xml files. For that reason, you have to remove the xml extension from it.
To do that you have to, as administrator, go to Administration-> Configuration->General Settings->XML and delete the .xml extension from it.

![serverconfig](/img/server-conf-1.png)
#### Plugin
1. Plugin Generation
    - Download the module source code.
    - Open a terminal window and browse to module root folder.
    - Build the mule plugin for Mule rules running `mvn clean package sonar-packaging:sonar-plugin -Dlanguage=mule`.
    - Copy the generated file, mule-validation-sonarqube-plugin-{version}-mule.jar to *sonar-home*/extensions/plugins

2. Copy rules [Mule 3 Rules](https://github.com/mulesoft-consulting/mule-sonarqube-plugin/blob/master/src/test/resources/rules-3.xml) or [Mule 4 Rules](https://github.com/mulesoft-consulting/mule-sonarqube-plugin/blob/master/src/test/resources/rules-4.xml) to *sonar-home*/extensions/plugins
The jar file of the plugin has to be placed in the following folder <server-home>/extensions/plugins/

### Project
#### Quality Profile
By default, the mule 4 quality profile is going to be used. In case you are analyzing a mule 3 you need to change it, to do that, as an administrator, go to the project -> Administration -> Quality Profiles and change the profile for the Mule Language.

![quality-profiles-conf](/img/quality-profiles-conf.png)

#### Quality Gate
If you have created a custom Mule Quality Gate, to enforce it on a project, you will have to go to the project -> Administration -> Quality Gates and change the gate previously selected.

![quality-gate](/img/quality-gate.png)

## Execution

### Analyzing
To analyze a project, you could run the maven command: ***mvn sonar:sonar***.
For that, you need to specify the server you are going to use and the directories you want to inspect when you are running the command like this:
```mvn
mvn sonar:sonar -Dsonar.host.url=http://mysonarqubeserver:9000 -Dsonar.sources=src/
```
And alternative to this, could be:
- For source files, add the property in the **pom.xml** of the project like this:
```xml
<sonar.sources>src/</sonar.sources> <!-- Directory from where scan Configuration Files -->
```
- And for the server configuration, set it up in the **settings.xml** like this: 
```xml
<profile>
       <id>sonar</id>
       <activation>
           <activeByDefault>true</activeByDefault>
       </activation>
       <properties>
           <!-- Optional URL to server. Default value is http://localhost:9000 -->
           <sonar.host.url>
             http://localhost:9000
           </sonar.host.url>
       </properties>
</profile>
```
For more information, please refer to: https://docs.sonarqube.org/latest/analysis/analysis-parameters/

### Results
Once you run the command, you will see the project and the information about it like this:

***Overview***

![project-overview](/img/project-overview.png)

***Issues***

![project-issues](/img/project-issues.png)

***Measures***

![project-measures](/img/project-measures.png)

### Try it out

```cmd
docker pull fperezpa/mulesonarqube:7.7.3
docker run -d --name sonarqube -p 9000:9000 -p 9092:9092 fperezpa/mulesonarqube:7.7.3
```
*Disclaimer*
The docker image is based on the official SonarQube Image, *sonarqube:7.7-community*. For more information please visit, https://hub.docker.com/_/sonarqube/


## Final Notes
Enjoy and provide feedback / contribute :)

