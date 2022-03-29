
# Security Policy

## Supported Versions

We release patches for security vulnerabilities. Which versions are eligible
receiving such patches depend on the CVSS v3.0 Rating:

| CVSS v3.0 | Supported Versions                        |
| --------- | ----------------------------------------- |
| 9.0-10.0  | Releases within the previous three months |
| 4.0-8.9   | Most recent release                       |

## Reporting a Vulnerability

Please report (suspected) security vulnerabilities to
**[oss-security@momentive.ai](mailto:oss-security@momentive.ai)**. You will receive a response from us within 48 hours.

Please include at minimum the following details:
- the repository where the vulnerability has been detected
- a brief description of the vulnerability
- optionally the type of vulnerability and any related [OWASP category]
- non-destructive exploitation details

If the issue is confirmed, we will release a patch as soon
as possible depending on complexity. Momentive's target vulnerability remediation SLA is:

| CVSS v3.0 | Remediation Target |
| --------- | ------------------ |
| 7.0-10.0  | 30 days  			 |
| 4.0-6.9   | 60 days		     |
| 0.1-3.9   | 90 days		     |

## Scope
The following are **not** in scope:
- volumetric vulnerabilities, for example overwhelming a service with a high volume of requests
- reports indicating that our services do not fully align with “best practice”, for example missing security headers

## Bug bounty
Unfortunately, Momentive BugBounty program does not include OSS software in scope unless the vulnerability can be demonstrated on SurveyMonkey products in Production. SurveyMonkey will make efforts to show appreciation to people who take the time and effort to disclose vulnerabilities responsibly.


