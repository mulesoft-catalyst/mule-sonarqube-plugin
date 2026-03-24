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

**Minimum SonarQube version**: **9.9 LTS** (this plugin is built against `sonar-plugin-api` 9.9).

**Highlights**:
- **DataWeave scanning**: `.dwl` files are indexed under the Mule language and scanned with DataWeave-specific rules (currently 2 rules: **Remove commented-out code** and **DataWeave file should not be too large**).
- **MUnit coverage in SonarQube**: imports MUnit coverage JSON reports so coverage is visible in SonarQube (see [MUnit coverage JSON report location](#munit-coverage-json-report-location)).

**Security**: XML parsing is hardened against XXE (external entities/DTDs are blocked). The optional namespace extension property `sonar.mule.namespace.properties` supports only local specs (`classpath:` / `file:` / filesystem path); `http(s)` is not allowed.

**Note**: SonarQube 9.9 LTS requires Java 17 to run the server.

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

![qualityprofiles](/img/1.1/quality-profiles.png)

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
- **rulesets**: a way to group rules by a category. Categories can be anything (e.g., application, configuration, api, batch).
  - You can filter which categories run during analysis with `-Dsonar.mule.ruleset.categories=flows,configuration,api`.
- **rules**: the rule itself, with these attributes:
  - **id**: a sequential number inside of the ruleset
  - **name**: meaningful name of rule
  - **description**: a more complete identification of the rule (supports HTML)
  - **severity**: the severity of the rule. Possible values are: `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`, `INFO`
  - **type**: the type of the issue that is going to be raised. Possible values are: `code_smell`, `bug`, `vulnerability`
  - **applies**: the scope of the rule. Possible values are:
    - `file`: evaluated per file
    - `application`: must evaluate true for at least one file in the project
    - `project`: validates project metadata (e.g., `sonar.projectName` / `sonar.projectKey`) using a regex
    - `node` (v1.1 rules only): selects violating nodes; an issue is created per node
  - **pluginVersion** (optional): rule format version:
    - `1.0` (default): JDOM2/Jaxen evaluation path
    - `1.1`: `javax.xml` (JAXP) XPath evaluation path (enables node-scope rules and accurate locations)
  - **locationHint** (optional): an XPath expression used to locate/anchor the issue when the rule fails (mainly used in file-scope rules)
  - **content**: XPath expression to be evaluated.

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

DataWeave rules are defined separately in `rules-dataweave.xml` and are evaluated by the DataWeave sensor on `.dwl` files (independent of Mule XML/XPath rules).

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

**Rule Store Example (v1.1 / `javax.xml` XPath with node-scope)**
```xml
<rulestore type="mule4">
  <ruleset category="configuration" pluginVersion="1.1">
    <!-- Node-scope rule: selects each violating node; SonarQube raises one issue per node -->
    <rule id="1"
          name="Hardcoded Salesforce connection values are not allowed"
          description="Detects hardcoded values in Salesforce connection attributes; values must be configurable placeholders like ${...}."
          severity="MAJOR"
          applies="node"
          type="code_smell"
          pluginVersion="1.1">
      //salesforce:cached-basic-connection//@*[f:isNotConfigurable(.)]
    </rule>

    <!-- File-scope rule: must evaluate true per file (uses JAXP boolean evaluation) -->
    <rule id="2"
          name="APIKit must be configured"
          description="Ensures the application uses APIKit by requiring at least one apikit:config element."
          severity="MAJOR"
          applies="file"
          type="code_smell"
          pluginVersion="1.1">
      count(//mule:mule/apikit:config) &gt; 0
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

#### MUnit coverage JSON report location

By default, the plugin loads the MUnit coverage JSON report from:

- **Mule 3**: `target/munit-reports/coverage-json/report.json`
- **Mule 4**: `target/site/munit/coverage/munit-coverage.json`

If your CI/test job generates the JSON report in a different location (for example, in a separate test application/module),
you can provide one or more explicit paths via:

- `-Dsonar.coverage.mulesoft.jsonReportPaths=/abs/path/to/report.json`
- `-Dsonar.coverage.mulesoft.jsonReportPaths=target/site/munit/coverage/munit-coverage.json,../test-app/target/.../munit-coverage.json`

Paths can be **absolute** or **relative to the module base directory** being analyzed by Sonar.

## Configuration
### Server
This plugin scans Mule configuration XML files by detecting Mule's core namespace (`http://www.mulesoft.org/schema/mule/core`) and by the configurable suffix list `sonar.mule.scan.file.suffixes` (default: `.xml`).

DataWeave source files (`.dwl`) are included in the analysis under the **Mule** language by default. The default language suffix list (`sonar.mule.file.suffixes`) is set to `.dwl` so these files show up in SonarQube and can carry issues.

The DataWeave sensor scans files by the configurable suffix list `sonar.mule.dataweave.file.suffixes` (default: `.dwl`).

![serverconfig](/img/1.1/dataweave.png)


You **do not need** to remove `.xml` from the built-in XML language settings. If you want to avoid XML analyzer rules running on Mule configuration files, prefer using exclusions on the XML analyzer or project exclusions instead of removing `.xml` globally.

![serverconfig](/img/1.1/server-conf-1.png)
#### Plugin
1. Plugin Generation
    - Download the module source code.
    - Open a terminal window and browse to module root folder.
    - Build the plugin running `mvn -DskipTests package`.
    - Copy the generated file `target/mule-validation-sonarqube-plugin-1.1.0-mule.jar` to `SONARQUBE_HOME/extensions/plugins/`.

2. Rules files
    - Rules are **embedded inside the plugin JAR** by default.
    - Optional override: to customize rules without rebuilding the plugin, place `rules-3.xml` / `rules-4.xml` (and optionally `rules-4-custom.xml`) in `*sonar-home*/extensions/plugins/` (the plugin will use those if present).
    - Rules support versioning via `pluginVersion`:
      - `pluginVersion="1.0"` (default): legacy JDOM2 evaluation
      - `pluginVersion="1.1"`: `javax.xml` XPath evaluation with **node-scope** rules (multiple issues per file + precise locations)
The jar file of the plugin has to be placed in the following folder <server-home>/extensions/plugins/

### Project
#### Quality Profile
By default, the mule 4 quality profile is going to be used. In case you are analyzing a mule 3 you need to change it, to do that, as an administrator, go to the project -> Administration -> Quality Profiles and change the profile for the Mule Language.

![quality-profiles-conf](/img/1.1/quality-profiles-conf.png)

#### Quality Gate
If you have created a custom Mule Quality Gate, to enforce it on a project, you will have to go to the project -> Administration -> Quality Gates and change the gate previously selected.

![quality-gate](/img/1.1/quality-gate.png)

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

![project-overview](/img/1.1/project-overview.png)

***Issues***

![project-issues](/img/1.1/project-issues.png)

***Measures***

![project-measures](/img/1.1/project-measures.png)

### Try it out

The easiest way to run SonarQube locally with this plugin is to use the official SonarQube Docker image
and mount the plugin JAR into the container.

#### Option A (recommended): Run SonarQube and mount the plugin JAR

1) Build the plugin JAR:

```cmd
mvn -DskipTests package
```

This produces:

```cmd
target/mule-validation-sonarqube-plugin-1.1.0-mule.jar
```

2) Start SonarQube with the plugin mounted into `extensions/plugins/`:

On **Windows (cmd.exe)**:

```cmd
docker rm -f sonarqube-mule 2>nul

docker run -d --name sonarqube-mule ^
  -p 9000:9000 ^
  -v "%cd%/target/mule-validation-sonarqube-plugin-1.1.0-mule.jar:/opt/sonarqube/extensions/plugins/mule-validation-sonarqube-plugin-1.1.0-mule.jar" ^
  sonarqube:latest
```

If you are on macOS/Linux, the same command uses `$(pwd)` instead of `%cd%` and does not need `^`:

```bash
docker rm -f sonarqube-mule 2>/dev/null || true

docker run -d --name sonarqube-mule \
  -p 9000:9000 \
  -v "$(pwd)/target/mule-validation-sonarqube-plugin-1.1.0-mule.jar:/opt/sonarqube/extensions/plugins/mule-validation-sonarqube-plugin-1.1.0-mule.jar" \
  sonarqube:latest
```

3) Watch startup logs and confirm the server is healthy:

```cmd
docker logs -f sonarqube-mule
```

Then open `http://localhost:9000/`.

#### Option B: Build a local Docker image (without committing a Dockerfile)

If you prefer an image that already contains the plugin, you can create a Dockerfile locally (not committed)
and build it.

1) Build the plugin JAR:

```cmd
mvn -DskipTests package
```

2) Create a temporary Dockerfile and build:

```bash
cat > Dockerfile.sonarqube-mule <<'EOF'
FROM sonarqube:latest
COPY target/mule-validation-sonarqube-plugin-1.1.0-mule.jar /opt/sonarqube/extensions/plugins/
EOF

docker build -t sonarqube-with-mule-plugin -f Dockerfile.sonarqube-mule .
```

3) Run the image:

```bash
docker rm -f sonarqube-mule 2>/dev/null || true
docker run -d --name sonarqube-mule -p 9000:9000 sonarqube-with-mule-plugin
```

#### Option C: Prebuilt Docker Image with mule-validation-sonaqube-plugin v1.1.0
```
docker run -d --name sonarqube-mule -p 9000:9000 pitaliyapankaj/mule-sonarqube-plugin:latest
```

*Disclaimer*
The docker image is based on the official SonarQube Image. For more information please visit https://hub.docker.com/_/sonarqube/


## Final Notes
Enjoy and provide feedback / contribute :)

