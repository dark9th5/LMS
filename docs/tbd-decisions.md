# BA Items Requiring Customer Decision

The URD deliberately leaves the following items open. Defaults in this repository are development assumptions only and must not be treated as approved customer policy.

| ID | Topic | Temporary development assumption |
|---|---|---|
| TBD-01 | Scale | 500 registered, 100 concurrent, 20 concurrent exams |
| TBD-02 | Performance | p95 API under 800 ms for normal CRUD; page usable under 2.5 s on LAN |
| TBD-03 | Backup/DR | daily full + hourly WAL/archive plan; 30-day retention; final RPO/RTO pending |
| TBD-04 | Account | 12-character password, 5 failed attempts, 15-minute access token, MFA not enabled |
| TBD-05 | Import | preview first; valid rows may be committed, invalid rows returned separately |
| TBD-06 | Course version | published content edits create a new version; active classes remain on their assigned version |
| TBD-07 | Assignment | 200 MB default, resubmission allowed until deadline, late policy configurable |
| TBD-08 | Exam | autosave every 10 seconds; server time authoritative; 30-second submission grace period |
| TBD-09 | Grade | two-decimal storage, displayed to one decimal; all changes audited |
| TBD-10 | Certificate | internal verification code; final signature/QR template pending |
| TBD-11 | Notification | in-app enabled; SMTP optional; exponential retry |
| TBD-12 | Reports | CSV implemented first; official KPI catalogue and PDF layout pending |
| TBD-13 | License | development bypass available; production binding/grace policy pending |
| TBD-14 | Integration | adapter framework only until each target API/mapping is supplied |
| TBD-15 | Security | TLS, at-rest encryption, malware scan and retention require customer threat model |
| TBD-16 | Infrastructure | reference stack documented; final supported OS/runtime matrix pending |
