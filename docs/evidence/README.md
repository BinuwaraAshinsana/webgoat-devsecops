# docs/evidence/

Every screenshot, scan output and diff that the technical report cites lives here.

## Naming convention

Use `vN-<vuln>-<stage>.<ext>` so files sort together per vulnerability:

|       Stage        |         File         |                       What it shows                       |
|--------------------|----------------------|-----------------------------------------------------------|
| 1. Exploit works   | `v1-sqli-before.png` | The payload and the response, against **unmodified** code |
| 2. The fix         | `v1-sqli-diff.txt`   | Output of `git diff` for the fix                          |
| 3. Exploit blocked | `v1-sqli-after.png`  | The **exact same** payload, now failing                   |
| 4. SAST delta      | `v1-sqli-sast.txt`   | Finding count for that file, before and after             |

## SAST scans

|                     File                     |                 When                  |
|----------------------------------------------|---------------------------------------|
| `sast-baseline.sarif` / `sast-baseline.json` | Taken **before any fix is committed** |
| `sast-after.json`                            | Taken after all four fixes are merged |

A fix with no demonstrated exploit counts as basic secure-coding evidence only,
not as full vulnerability remediation — all four stages are required.
