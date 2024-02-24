# Security Policy

## Supported Versions

The GoCD community only actively maintains and fixes security issues on top of the most recent released version.

Since breaking changes are rare, and generally sign-posted well in advance, we encourage users to stay on a recent or current version to allow for upgrade as easily as possible in the event of a security defect.

Having said this, wherever possible we will try and provide suggested mitigations or workarounds for older versions.

## Reporting a Vulnerability

Please report any issues to https://hackerone.com/gocd according to the listed policy.

## Baseline

This represents the oldest version which has **no known exploitable vulnerabilities**. Users are strongly recommended to be on at least this version; and preferably the latest version. 

| Baseline Version |
| ---------------- |
| `23.1.0`         |

Please note that this does *not* mean that there are zero potential vulnerabilities known from GoCD's dependencies
in this or subsequent versions. However where such vulnerabilities exist, none have been confirmed to be exploitable via GoCD
itself (without a prior non-GoCD breach).

## How do I know if I am using a release with known vulnerabilities?

In more recent years, an effort has been made to publish and request CVEs for responsibly disclosed & fixed issues to increase transparency and help users assess risk of running older versions.

While many are available as [GitHub Security Advisories](https://github.com/gocd/gocd/security/advisories), you can generally use the [NIST NVD database query tools](https://nvd.nist.gov/vuln/search?results_type=overview&query=cpe%3A2.3%3Aa%3Athoughtworks%3Agocd%3A22.3.0%3A*%3A*%3A*%3A*%3A*%3A*%3A*&search_type=all&form_type=Basic&isCpeNameSearch=true) to search for those affecting your specific version by replacing the version `22.3.0` with your own  and clicking "_Search_".

Note that this unlikely to be a complete listing of _all_ reported, responsibly disclosed and fixed issues. If there is a _publicly disclosed_ historical issue that is missing, please [raise an issue](https://github.com/gocd/gocd/issues/new) to let us know, and we will endeavour to document it properly.

## What about potential vulnerabilities from transitive dependencies?

The GoCD team make a concerted effort to keep dependencies up-to-date wherever possible, however GoCD does
still have some EOL dependencies with known vulnerabilities that GoCD is not vulnerable to, but which may create noise in scanner reports.

While this is a moving target the GoCD team maintain documented suppressions with commentary via:
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/) - Java & JavaScript dependencies
  - [current suppressions](https://github.com/gocd/gocd/blob/master/buildSrc/dependency-check-suppress.xml)
  - [build.gocd.org report off master](https://build.gocd.org/go/files/Security-Checks/latest/test/latest/dependency-check/dependency-check-report.html) (use _Guest_ login)
- [Bundler Audit](https://github.com/rubysec/bundler-audit) - Ruby/JRuby dependencies
  - [build.gocd.org report off master](https://build.gocd.org/go/files/Security-Checks/latest/test/latest/bundler-audit/cruise-output/console.log) 
- [Trivy](https://trivy.dev/) - built container images (OS and packaged dependencies), especially server
  - [current suppressions](https://github.com/gocd/gocd/blob/master/buildSrc/.trivyignore)
  - [build.gocd.org Security-Checks-Containers pipeline](https://build.gocd.org/) (use _Guest_ login)
