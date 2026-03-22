# Resume Runbook

## Purpose

This file is the required re-entry point for any AI or human resuming work after an interruption.

## Resume Order

1. Read `docs/PROGRAM-STATUS.md`
2. Read `docs/REQUIREMENT-BACKLOG.md`
3. Read `docs/HANDOFF-CHECKLIST.md`
4. Read the active task in `docs/TASK-BOARD.md`
5. Read the latest entry in `docs/ITERATION-LOG.md`
6. Run the standard verification commands
7. Continue the single next recommended action from `docs/PROGRAM-STATUS.md`
8. If remote verification is relevant, read `docs/REMOTE-VERIFICATION-NODE.md` and run `py -3.12 ops/remote/probe_server.py`

## Standard Verification Commands

```powershell
./gradlew.bat build
./gradlew.bat :boot-app:test
./gradlew.bat :application:test
./gradlew.bat :testkit:test
./gradlew.bat :interfaces-rest:test
./gradlew.bat :interfaces-cli:test
py -3.12 ops/remote/probe_server.py
```

Run the Gradle commands serially, not in parallel, on this Windows machine.

## How To Identify Current Work

- `PROGRAM-STATUS.md` says which stage and task are active
- `REQUIREMENT-BACKLOG.md` says which long-lived requirement currently owns the work
- `TASK-BOARD.md` says what done means
- `ITERATION-LOG.md` says what actually happened most recently
- `TEST-STATUS.md` says whether the current baseline is verified

## How To Restore Local Services

- if Docker is available, start Neo4j with `docker compose up -d`
- if Docker is unavailable, do not fake green infrastructure; record the gap in `PROGRAM-STATUS.md` and `TEST-STATUS.md`
- if remote verification is needed, use `ops/remote/probe_server.py` and the local-only `.local/remote-verification.env` credential cache
- if the remote host is being used for Neo4j verification, check `docs/REMOTE-VERIFICATION-NODE.md` first and then use `ops/remote/start_neo4j.py` or `ops/remote/stop_neo4j.py`
- when validating the real persistence path, prefer the live remote Neo4j node over inventing local container success on this workstation

## Server Restart Procedure

When restarting the Digital Beings Java service on the remote server (`ssh jd`):

```bash
# 1. Check current status
curl -s http://localhost:8080/actuator/health

# 2. Stop the application
ssh jd "systemctl stop digital-beings"

# 3. Restart Neo4j (if needed)
ssh jd "docker restart neo4j"
ssh jd "sleep 10 && docker logs neo4j --tail 20"

# 4. Start the application
ssh jd "systemctl start digital-beings"

# 5. Wait for startup and verify health
ssh jd "sleep 15 && curl -s http://localhost:8080/actuator/health"
```

## Neo4j Backup Procedure

To create a backup of the Neo4j database:

```bash
# 1. Ensure Neo4j is running
ssh jd "docker ps | grep neo4j"

# 2. Create backup directory with timestamp
ssh jd "mkdir -p /backups/neo4j-$(date +%Y%m%d-%H%M%S)"

# 3. Run Neo4j backup
ssh jd "docker exec neo4j neo4j-admin backup --backup-dir=/backups --graph-name=graph.db"

# 4. Verify backup was created
ssh jd "ls -la /backups/"

# 5. Check backup integrity
ssh jd "docker exec neo4j neo4j-admin backup --backup-dir=/backups --check"
```

## Restore from Backup

```bash
# 1. Stop the application
ssh jd "systemctl stop digital-beings"

# 2. Stop Neo4j
ssh jd "docker stop neo4j"

# 3. Restore from backup (replace TIMESTAMP with actual backup directory)
ssh jd "docker run --rm -v neo4j_data:/data -v /backups/TIMESTAMP:/backup ubuntu cp -r /backup/* /data/"

# 4. Start Neo4j
ssh jd "docker start neo4j"

# 5. Verify Neo4j is running
ssh jd "docker ps | grep neo4j"

# 6. Start the application
ssh jd "systemctl start digital-beings"

# 7. Verify health
curl -s http://localhost:8080/actuator/health
```

## Application Log Locations

| Environment | Log Location |
|------------|--------------|
| Remote Server | `journalctl -u digital-beings -f` |
| Remote Server (file) | `/var/log/digital-beings/` |
| Remote Neo4j | `docker logs neo4j --tail 100 -f` |

## Incident Response

For detailed incident procedures, see `docs/INCIDENT-RUNBOOK.md`.

## How To Continue Safely

- create or update a requirement before changing implementation scope
- create or update a task only after the requirement entry exists and is actionable
- write an ADR before any non-trivial architecture change
- update the relevant contract docs before landing schema or API changes
- finish every work block by updating status, logs, and the handoff checklist

## Forbidden Actions

- do not rely on chat memory as the source of truth
- do not leave blockers or assumptions only in code comments
- do not start the next task while the handoff checklist is stale
- do not claim infrastructure verification if the machine lacks the required tools
- do not run overlapping Gradle build and test tasks against the same module outputs in parallel
- do not commit `.local/` or copy its contents into repo docs
