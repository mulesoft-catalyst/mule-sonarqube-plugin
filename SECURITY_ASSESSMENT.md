## Security Assessment – Mule SonarQube Plugin (v1.0.8)

This document summarizes a security and vulnerability assessment of the Mule SonarQube Plugin code in this repository, along with mitigations implemented.

Reference context: upstream issue tracker at [`mulesoft-catalyst/mule-sonarqube-plugin` issues](https://github.com/mulesoft-catalyst/mule-sonarqube-plugin/issues).

### Scope
- **Code reviewed**: plugin sensors, rules/profile bootstrapping, XML/XPath processing, coverage parsing, configuration properties handling.
- **Threat model**:
  - **Trusted**: SonarQube server administrators, plugin deployers.
  - **Potentially untrusted**: source code being analyzed (Mule XML files) in PR analysis, especially from external contributors/forks.
  - **Execution environments**:
    - SonarQube server (rule repository/profile definition at startup)
    - SonarScanner/CI runner (analysis sensors and file parsing)

### Summary verdict
- **Safe to use** in internal/trusted environments with restricted project configuration access and restricted network egress.
- **Primary historical blocker**: unsafe XML parsing defaults (XXE / external entity processing) when scanning untrusted XML.
- **Additional concern**: configuration-driven URL fetching for namespace extension could enable SSRF if misused.

## Attack surface & inputs

### Inputs consumed by the plugin
- **Mule XML configuration files** in the analyzed project (potentially attacker-controlled in PR analysis).
- **Rules XML**:
  - embedded defaults (classpath) and/or
  - optional override on the SonarQube server filesystem (`extensions/plugins/rules-3.xml`, `extensions/plugins/rules-4.xml`).
- **Namespace extension properties** via `sonar.mule.namespace.properties`.
- **MUnit coverage JSON** in the project build output folder (under `target/...`).

### Primary trust boundaries
- **Project source code → XML parser**: Mule XML content can contain DOCTYPE/entity directives.
- **Project configuration → external fetch**: `sonar.mule.namespace.properties` can point to an external spec (before restriction).
- **Rules overrides → plugin startup**: server filesystem rule overrides affect rule definitions.

## Findings

### F1 – XXE / external entity processing in XML parsing (High)
**Description**
- Multiple code paths parse XML using JDOM2 `SAXBuilder` with default settings. With defaults, DOCTYPE and external entities may be processed depending on underlying parser behavior.

**Impact**
- **SSRF**: parser may fetch external entities over the network.
- **Local file disclosure**: external entities can reference `file://...`.
- **DoS**: entity expansion (“billion laughs”) can exhaust CPU/memory.

**Likelihood**
- **High** when scanning untrusted XML (PRs from forks / external contributors).
- **Medium** in internal repos if contributors can commit crafted XML.

**Mitigation implemented in this repo**
- Centralized secure XML parsing configuration:
  - Disallow DOCTYPE declarations
  - Disable external general/parameter entities
  - Disable loading external DTDs
  - Disable entity expansion
- All XML parsing call sites were updated to use the secured builder.

### F2 – SSRF via `sonar.mule.namespace.properties` (Medium)
**Description**
- The namespace-extension feature loads a `.properties` spec and previously supported `http(s)` URL specs.

**Impact**
- If a project property can be influenced by an attacker (or misconfigured), analysis could make outbound requests to internal or external endpoints.

**Likelihood**
- Depends on who can edit project properties and whether CI permits overriding them.

**Mitigation implemented in this repo**
- Restricted `sonar.mule.namespace.properties` to **local-only sources**:
  - `classpath:...`
  - `file:...`
  - plain filesystem paths
- **HTTP(S) URLs are rejected** for namespace extension specs.

### F3 – Potential DoS via expensive XPath/regex evaluation (Medium)
**Description**
- Rules are XPath expressions and may contain regex `matches()`. Complex regex patterns and/or very large XML inputs can be costly.

**Impact**
- CI analysis slowdown or timeouts.

**Status**
- Not fully eliminated. Practical mitigations include:
  - limiting scan scope/suffixes (`sonar.mule.scan.file.suffixes`)
  - limiting max file size scanned (future enhancement)
  - caching compiled XPath expressions (future enhancement)

### F4 – Coverage JSON loaded fully into memory (Low/Medium)
**Description**
- Coverage JSON is currently read into a single `String` before parsing.

**Impact**
- Very large report files could cause memory pressure during analysis.

**Status**
- Acceptable for typical MUnit reports; could be improved by streaming parse and/or file size guardrails.

## Dependency posture (high level)
- Key dependencies (per `mvn dependency:tree`):
  - `com.fasterxml.jackson.core:jackson-databind:2.17.2`
  - `org.apache.logging.log4j:log4j-*:2.17.2`
  - `org.jdom:jdom2:2.0.6.1`
- No critical “known-bad” versions were identified among these pinned top-level dependencies in this repo; the principal security risk was the XML parser configuration (addressed by F1 mitigation).

## Operational recommendations
- **Run analysis in a restricted network environment** (deny egress by default) to limit blast radius.
- **Limit who can modify Sonar project properties**, especially those affecting parsing/paths.
- **Avoid scanning untrusted XML** unless you keep XXE protections enabled (now implemented).
- Keep **rules override files** on the SonarQube server restricted to administrators.

