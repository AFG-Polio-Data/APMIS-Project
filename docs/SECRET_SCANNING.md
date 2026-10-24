# Secret Scanning

The repository is scanned for committed secrets with [gitleaks](https://github.com/gitleaks/gitleaks).
The check is currently **advisory**: it reports findings but never fails a pull request.

## What runs

The `Secret Scanning` workflow (`.github/workflows/gitleaks.yml`) runs on every pull request
and scans the commits the pull request adds (base commit..head commit) using the rules from
`.gitleaks.toml`, which extends the upstream default rule set.

The workflow can also be started manually from the Actions tab:

* default run – scans the latest commit of the selected branch;
* run with `full_history` enabled – scans the whole git history and provides a baseline for the
  false-positive rate.

## Where to find findings

1. The **job summary** of the `Secret Scanning` run lists every finding (rule, file, line, commit
   and the redacted match).
2. The `gitleaks-report` artifact keeps the full JSON report for 30 days.
3. The workflow logs contain the gitleaks console output.

## Reporting mode

The gitleaks step runs with `continue-on-error` and `--exit-code 0`, so a pull request is never
blocked by a finding. This stays in place until the false-positive rate has been measured; the
workflow can then be switched to required by removing `continue-on-error` and dropping
`--exit-code 0`.

## Triaging a finding

| Situation | Action |
| --- | --- |
| Real credential, token or private key | Rotate/revoke it immediately. If it is still reachable in the git history, remove it from the history as well (for example with `git filter-repo`). |
| Test fixture, placeholder or example value that cannot be used | Add a narrowly scoped entry to the allowlist in `.gitleaks.toml` (prefer `paths` or a precise `regexes` entry over disabling a rule). |
| Default demo credentials in `docs/SERVER_UPDATE.md` | Expected, not leaked secrets: these accounts are documented for demo/development systems only. They are still reported so that the decision stays visible; never use them on production systems. |

Every allowlist entry hides a finding from future reports, so it must be justified in the change
description.

## Measuring the false-positive rate

Before the check becomes required, record the outcome of each triage decision here so the
false-positive rate is known:

| Date | Scope | Findings | False positives | Notes |
| --- | --- | --- | --- | --- |
| _pending baseline run_ | | | | Run `Secret Scanning` with `full_history` enabled and record the result. |

False-positive rate = false positives / findings of the recorded run.
