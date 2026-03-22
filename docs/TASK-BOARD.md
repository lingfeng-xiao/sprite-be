# Task Board

## Task Governance

- `Requirement` is mandatory for any `in_progress`, `planned`, or post-backlog completed task.
- Historical stage 0 and stage 1 tasks created before `REQ-*` governance may omit the field.
- New execution scope must be created or updated in `docs/REQUIREMENT-BACKLOG.md` before a new `JAVA-*` task is added here.

## In Progress

### JAVA-C2a Stage 9 Portable Snapshot Export/Import API ✅ COMPLETED

- Requirement: `REQ-090`
- Stage: 9
- Status: completed (2026-03-22)
- Dependencies: `JAVA-B4` (completed)
- Note: Run on server via `ssh jd`
- Implementation Plan:
  1. ✅ Create `PortableSnapshot` value object (domain-core): captures full Being state
  2. ✅ Create `ExportSnapshotCommand` + `ImportSnapshotCommand` (application)
  3. ✅ Create `PortableSnapshotService` with `exportSnapshot()` + `importSnapshot()` (application, @Component)
  4. ✅ Add `POST /beings/{id}/export` + `POST /beings/{id}/import` to SnapshotController (path: `/snapshots/beings/{id}/export`, `/snapshots/beings/{id}/import`)
  5. ✅ Add `ReviewItem.fromPortableSnapshot()` factory method (domain-core)
  6. ✅ Add `AuthorityLease.fromPortableSnapshot()` factory method (domain-core)
  7. ✅ Add `RuntimeSession.fromPortableSnapshot()` factory method (domain-core)
  8. ✅ Add `BEING_IMPORTED` to `DomainEventType` enum
  9. ✅ Add `Being.reconstitute()` factory method (domain-core)
- Acceptance: Export → stop source → import → all GET endpoints return identical data
- Verification: All tests pass (`./gradlew test`)

### JAVA-C2b Stage 9 Portable Snapshot JSON Schema

- Requirement: `REQ-090`
- Stage: 9
- Status: pending (can start now - C2a export schema is stable)
- Dependencies: `JAVA-C2a` (export schema stable, can document now)
- Note: Define schema in docs/PORTABLE-SNAPSHOT-FORMAT.md

## Planned

### JAVA-C3a Stage 9 Portable Snapshot Import API

- Requirement: `REQ-090`
- Stage: 9
- Status: pending
- Dependencies: `JAVA-C2a` (export must be done first to know the full schema)
- Implementation Plan:
  1. `importSnapshot()` reconstructs Being aggregate from PortableSnapshot JSON
  2. Validates continuity_epoch increments correctly
  3. Resets all active leases to CLOSED (no active lease on import)
  4. Records DOMAIN_EVENT.IMPORTED domain event

### JAVA-C3b Stage 9 Import Consistency Verification

- Requirement: `REQ-090`
- Stage: 9
- Status: pending
- Dependencies: `JAVA-C3a`
- Implementation Plan:
  1. After import, verify identity regression: GET /beings/{id}/injection-context returns same canonicalProjection.version
  2. Verify relationships count matches
  3. Verify ownerProfileFacts preserved
  4. All verification checks run as automated test

### JAVA-C4 Stage 9 Full Migration Drill Script

- Requirement: `REQ-091`
- Stage: 9
- Status: pending
- Dependencies: `JAVA-C2a`, `JAVA-C3a`
- Outputs:
  - `ops/migration/run_migration_drill.sh`: export → verify JSON → import → run identity regression → report pass/fail → rollback option
  - `ops/migration/run_rollback_drill.sh`: re-import original snapshot and verify no drift

### JAVA-C1 Stage 9 Gray Cutover Strategy & First Cutover

- Requirement: `REQ-092`
- Stage: 9
- Status: pending
- Dependencies: `JAVA-C4`
- Implementation Plan:
  1. Document cutover criteria in docs/MIGRATION-LEDGER.md
  2. Select first cutover being (low-risk)
  3. Execute cutover: switch being to Java runtime
  4. Verify all runtime paths work: injection-context, service-card, session, lease, evolution-signal

### JAVA-C5 Stage 9 Rollback Procedure Validation

- Requirement: `REQ-091`
- Stage: 9
- Status: pending
- Dependencies: `JAVA-C4`
- Verification: Run rollback drill, confirm rollback script exits 0 and no Neo4j drift

### JAVA-C6 Stage 9 OpenClaw Demotion

- Requirement: `REQ-093`
- Stage: 9
- Status: pending
- Dependencies: `JAVA-C1`, `JAVA-C5`
- Outputs: Delete operate_runtime_hub.py, operate_openclaw_host.py, operate_codex_host.py; OpenClaw adapter only receives Java REST API instructions

### JAVA-D1 Stage 10 OpenClaw Runtime Script Deletion

- Requirement: `REQ-093`
- Stage: 10
- Status: pending
- Dependencies: `JAVA-C6`
- Outputs: All runtime management scripts deleted from digital-beings/bridge/

### JAVA-D2 Stage 10 being_runtime.py Deletion

- Requirement: `REQ-094`
- Stage: 10
- Status: pending
- Dependencies: `JAVA-D1`
- Outputs: being_runtime.py and bridge_openclaw.py deleted from digital-beings/bridge/

### JAVA-D3 Stage 10 Archive digital-beings Repository

- Requirement: `REQ-095`
- Stage: 10
- Status: pending
- Dependencies: `JAVA-D2`
- Outputs: ARCHIVED.md in repo root, git tag `migration-complete-YYYYMMDD`, cron jobs removed

### JAVA-D4 Stage 10 Final Verification

- Requirement: `REQ-096`
- Stage: 10
- Status: pending
- Dependencies: `JAVA-D3`
- Outputs: Automated verification script confirms zero Python runtime state influence

## Completed

### JAVA-B7 Stage 8 Dual-Host Lease Coordination

- Requirement: `REQ-086`
- Stage: 8
- Status: completed
- Dependencies: `JAVA-B4` (completed)
- Outputs:
  - Added `HandoffLeaseCommand` for lease migration
  - Added `handoffLease()` method to `LeaseService`
  - Added `handoffAuthorityLease()` to `Being` domain aggregate
  - Added `POST /leases/{leaseId}/handoff` endpoint in `LeaseController`
  - Atomic lease handoff: old lease released and new lease created in same transaction
  - No vacancy window during dual-host handoff
- Related Files:
  - `application/src/main/java/.../lease/HandoffLeaseCommand.java`
  - `application/src/main/java/.../lease/LeaseService.java`
  - `domain-core/src/main/java/.../being/Being.java`
  - `interfaces-rest/src/main/java/.../lease/LeaseController.java`
  - `interfaces-rest/src/main/java/.../lease/HandoffLeaseRequest.java`
- Related Tests: `./gradlew test` (full suite green)



### JAVA-B5 Stage 8 EvolutionSignal Closed Loop

- Requirement: `REQ-084`
- Stage: 8
- Status: completed
- Dependencies: `JAVA-B4`
- Outputs:
  - Added `SubmitEvolutionSignalCommand` and `submitEvolutionSignal()` method to `ReviewService`
  - Added `POST /beings/{beingId}/evolution-signal` to `ReviewController`
  - Evolution signal converts to IDENTITY lane review item and auto-submits
- Related Files:
  - `application/src/main/java/.../review/SubmitEvolutionSignalCommand.java`
  - `application/src/main/java/.../review/ReviewService.java`
  - `interfaces-rest/src/main/java/.../review/ReviewController.java`
  - `interfaces-rest/src/main/java/.../review/SubmitEvolutionSignalRequest.java`
- Related Tests: `./gradlew test` (full suite green)

### JAVA-B4 Stage 8 Embodiment Bundle Compilation

- Requirement: `REQ-083`
- Stage: 8
- Status: completed
- Dependencies: `JAVA-B3`
- Outputs:
  - Added `ServiceCardView` for structured service card representation
  - Added `getServiceCard()` to `InjectionContextService`
  - Added `GET /beings/{id}/injection-context/service-card` endpoint
- Related Files:
  - `application/src/main/java/.../being/ServiceCardView.java`
  - `application/src/main/java/.../being/InjectionContextService.java`
  - `interfaces-rest/src/main/java/.../being/BeingController.java`
- Related Tests: `./gradlew test` (full suite green)

### JAVA-B3 Stage 8 Runtime Injection Context API

- Requirement: `REQ-082`
- Stage: 8
- Status: completed
- Dependencies: `JAVA-B2`
- Outputs:
  - Added `InjectionContextView` and `InjectionContextService`
  - Added `GET /beings/{id}/injection-context` endpoint
  - Returns being identity, canonical projection, and active session/lease
- Related Files:
  - `application/src/main/java/.../being/InjectionContextView.java`
  - `application/src/main/java/.../being/InjectionContextService.java`
  - `interfaces-rest/src/main/java/.../being/BeingController.java`
- Related Tests: `./gradlew test` (full suite green)

### JAVA-B2 Stage 8 AuthorityLease Auto-Release on Session Close

- Requirement: `REQ-081`
- Stage: 8
- Status: completed
- Dependencies: `JAVA-B1`
- Outputs:
  - Modified `closeSession()` in `LeaseService` to auto-release associated lease
  - Added `closeSessionAutoReleasesLease` test to `LeaseServiceTest`
- Acceptance: Session close automatically releases associated lease
- Related Files:
  - `application/src/main/java/.../lease/LeaseService.java`
  - `application/src/test/java/.../lease/LeaseServiceTest.java`
- Related Tests: `./gradlew test` (full suite green)

### JAVA-B1 Stage 8 OpenClaw Session Auto-Registration

- Requirement: `REQ-080`
- Stage: 8
- Status: completed
- Dependencies: none
- Outputs:
  - Added `StartBeingSessionCommand` and `SessionWithLeaseView`
  - Added `startBeingSession()` to `LeaseService`
  - Added `POST /beings/{id}/sessions` endpoint in `BeingController`
  - Session and lease acquired in one atomic call
- Related Files:
  - `application/src/main/java/.../lease/StartBeingSessionCommand.java`
  - `application/src/main/java/.../lease/SessionWithLeaseView.java`
  - `application/src/main/java/.../lease/LeaseService.java`
  - `interfaces-rest/src/main/java/.../being/BeingController.java`
  - `interfaces-rest/src/main/java/.../being/StartBeingSessionRequest.java`
  - `interfaces-rest/src/test/java/.../being/BeingControllerTest.java`
- Related Tests: `./gradlew test` (full suite green)

### JAVA-033 Stage 5 Legacy Importer Full Replay And Reporting

- Requirement: `REQ-030`
- Stage: 5
- Status: completed
- Dependencies: `JAVA-032`
- Inputs:
  - dry-run validated importer mapping core
- Outputs:
  - full import replay with wired parsers
  - count report (in replay report)
  - anomaly report (in replay report)
  - graph consistency report (in replay report)
- Current Progress:
  - Created `LegacyImportReplayReport` class with import statistics, anomaly tracking, and graph consistency checks
  - Added `replay()` method to `LegacyImporter` that produces a full report
  - Created `BeingYamlParser` for parsing being.yaml files
  - Created `ReviewStateParser` for parsing review-state.json files
  - Created `SessionLeaseJsonParser` for parsing session/lease JSON files
  - Added `ParseTally` inner class and `parseFiles()` method to wire parsers into replay logic
  - Added `buildGraphConsistencyChecks()` to produce actual consistency checks from parse results
  - Added tests for replay functionality (4 tests passing)
- Acceptance:
  - full import produces counts, anomalies, and graph consistency output
- Related Files:
  - `legacy-importer/src/main/java/com/openclaw/digitalbeings/legacy/importer/LegacyImportReplayReport.java`
  - `legacy-importer/src/main/java/com/openclaw/digitalbeings/legacy/importer/LegacyImporter.java`
  - `legacy-importer/src/main/java/com/openclaw/digitalbeings/legacy/importer/BeingYamlParser.java`
  - `legacy-importer/src/main/java/com/openclaw/digitalbeings/legacy/importer/ReviewStateParser.java`
  - `legacy-importer/src/main/java/com/openclaw/digitalbeings/legacy/importer/SessionLeaseJsonParser.java`
  - `docs/MIGRATION-LEDGER.md`
- Related Tests:
  - `./gradlew :legacy-importer:test` (4 tests passing)
  - `./gradlew test` (full suite green)

### JAVA-035 Stage 6 Governance Jobs And Operational Reports

- Requirement: `REQ-040`
- Stage: 6
- Status: completed
- Dependencies: `JAVA-034`
- Inputs:
  - governance backend flows
  - imported or native graph data
- Outputs:
  - stale lease cleanup job (completed in JAVA-A2: LeaseExpiryJob, SessionCleanupJob)
  - graph consistency job (completed: GraphConsistencyJob with 5 tests)
  - operational run reports (completed: GovernanceReportJob with 4 tests)
- Current Progress:
  - Created `GraphConsistencyJob` that scans for graph integrity issues
  - Checks for: active sessions without leases, closed sessions, pending review items
  - Created `GovernanceReportJob` that generates daily operational reports
  - Added 9 unit tests total (5 for GraphConsistencyJob, 4 for GovernanceReportJob)
- Acceptance:
  - governance loop is end-to-end and auditable
- Related Files:
  - `jobs/src/main/java/com/openclaw/digitalbeings/jobs/GraphConsistencyJob.java`
  - `jobs/src/test/java/com/openclaw/digitalbeings/jobs/GraphConsistencyJobTest.java`
  - `jobs/src/main/java/com/openclaw/digitalbeings/jobs/GovernanceReportJob.java`
  - `jobs/src/test/java/com/openclaw/digitalbeings/jobs/GovernanceReportJobTest.java`
  - `docs/PROGRAM-STATUS.md`
- Related Tests:
  - `./gradlew :jobs:test` (19 job tests passing)

### JAVA-A3 Stage 7 Restart/Backup/DR Runbook

- Requirement: `REQ-072`
- Stage: 7
- Status: completed
- Dependencies: `JAVA-A1`, `JAVA-A2`
- Inputs:
  - completed health probe and lease cleanup
  - existing runbook templates
- Outputs:
  - Server restart procedure (added to RESUME-RUNBOOK.md)
  - Neo4j backup procedure (added to RESUME-RUNBOOK.md)
  - incident runbook (created INCIDENT-RUNBOOK.md)
- Acceptance:
  - a newly attached AI can self-serve to understand service restart procedures
- Related Files:
  - `docs/RESUME-RUNBOOK.md` (updated with restart/backup procedures)
  - `docs/INCIDENT-RUNBOOK.md` (new - incident response procedures)
- Related Tests:
  - documentation review

## Completed

### JAVA-A2 Stage 7 LeaseExpiryJob & SessionCleanupJob

- Requirement: `REQ-071`
- Stage: 7
- Status: completed
- Dependencies: none (ran in parallel with A4, A5)
- Inputs:
  - existing lease and session domain models
  - governance job scheduling framework
- Outputs:
  - LeaseExpiryJob (fixed bug: was filtering `activeAuthorityLease().filter(isExpired)` which could never match)
  - SessionCleanupJob
  - StatusHeartbeatJob (existing, @ConditionalOnProperty gated)
  - domain event records for stale transitions
  - LeaseExpiryJobTest and SessionCleanupJobTest (10 tests total)
- Acceptance:
  - stale lease/session gets marked automatically, with domain event record
- Bug Fixed:
  - Changed `being.activeAuthorityLease().filter(AuthorityLease::isExpired)` to `being.authorityLeases().stream().filter(AuthorityLease::isExpired)` in LeaseExpiryJob - the original logic could never match because a lease cannot be both ACTIVE and EXPIRED simultaneously
- Related Files:
  - `jobs/src/main/java/com/openclaw/digitalbeings/jobs/LeaseExpiryJob.java`
  - `jobs/src/main/java/com/openclaw/digitalbeings/jobs/SessionCleanupJob.java`
  - `jobs/src/test/java/com/openclaw/digitalbeings/jobs/LeaseExpiryJobTest.java`
  - `jobs/src/test/java/com/openclaw/digitalbeings/jobs/SessionCleanupJobTest.java`
  - `application/src/main/java/com/openclaw/digitalbeings/application/lease/LeaseService.java`
- Related Tests:
  - `./gradlew :jobs:test` (10 tests passing)

### JAVA-A1 Stage 7 Health & Readiness Probe Completion

- Requirement: `REQ-070`
- Stage: 7
- Status: completed
- Dependencies: none
- Inputs:
  - existing actuator health indicators
  - Neo4j connectivity
- Outputs:
  - Neo4jHealthIndicator (conditional on Driver bean)
  - InstanceHealthIndicator (JVM metrics)
  - SchemaInitHealthIndicator (tracks schema initialization state)
  - readiness probe configuration in application.yml
- Acceptance:
  - `/actuator/health/readiness` returns UP when Neo4j is reachable and schema is initialized
- Related Files:
  - `boot-app/src/main/java/com/openclaw/digitalbeings/boot/config/Neo4jHealthIndicator.java`
  - `boot-app/src/main/java/com/openclaw/digitalbeings/boot/config/InstanceHealthIndicator.java`
  - `boot-app/src/main/java/com/openclaw/digitalbeings/boot/config/SchemaInitHealthIndicator.java`
  - `boot-app/src/main/java/com/openclaw/digitalbeings/boot/config/SchemaInitializer.java`
  - `boot-app/src/main/resources/application.yml`
- Related Tests:
  - `./gradlew :boot-app:test`

### JAVA-A4 Stage 7 Neo4j Migration Script Framework

- Requirement: `REQ-073`
- Stage: 7
- Status: completed
- Dependencies: none
- Inputs:
  - existing migration assets
  - neo4j-migrations configuration
- Outputs:
  - V001__baseline_graph migration (CanonicalProjection constraint)
  - V002__canonical_projection_graph migration (all other constraints and indexes)
  - SchemaInitializer with @ConditionalOnBean(Driver.class) to prevent creation without Neo4j
- Acceptance:
  - schema changes are versioned, tests have entry point
- Related Files:
  - `infra-neo4j/src/main/resources/neo4j/migrations/V001__baseline_graph.cypher`
  - `infra-neo4j/src/main/resources/neo4j/migrations/V002__canonical_projection_graph.cypher`
  - `boot-app/src/main/java/com/openclaw/digitalbeings/boot/config/SchemaInitializer.java`
  - `docs/SCHEMA-GRAPH.md`
- Related Tests:
  - `:infra-neo4j:test`

### JAVA-A5 Stage 7 Memory Store Production Guard

- Requirement: `REQ-074`
- Stage: 7
- Status: completed
- Dependencies: none
- Inputs:
  - existing boot profile configuration
  - MemoryBeingStoreConfiguration
- Outputs:
  - profile validation in neo4j profile (Neo4jProfileValidation)
  - warning log on startup for memory profile
  - ConditionalOnBean(Driver.class) guards to prevent creation without Neo4j
- Acceptance:
  - production neo4j profile cannot start with InMemoryStore implementation
- Related Files:
  - `boot-app/src/main/java/com/openclaw/digitalbeings/boot/config/MemoryBeingStoreConfiguration.java`
  - `boot-app/src/main/java/com/openclaw/digitalbeings/boot/config/Neo4jProfileValidation.java`
  - `boot-app/src/main/resources/application.yml`
- Related Tests:
  - `:boot-app:test`

### JAVA-034 Stage 6 Governance Backend Slice
- Inputs:
  - existing boot profile configuration
  - MemoryBeingStoreConfiguration
- Outputs:
  - profile validation
  - warning log on startup for memory profile
- Acceptance:
  - production neo4j profile cannot start with InMemoryStore implementation
- Related Files:
  - `boot-app/src/main/resources/application.yml`
  - `boot-app/.../MemoryBeingStoreConfiguration.java`
- Related Tests:
  - `:boot-app:test`

## Completed

### JAVA-034 Stage 6 Governance Backend Slice

- Requirement: `REQ-040`
- Stage: 6
- Status: completed
- Dependencies: `JAVA-029`, `JAVA-030`, `JAVA-031`, `JAVA-032`
- Inputs:
  - V1 API and CLI
  - stage 5 importer dry-run contract
- Outputs:
  - review cockpit backend
  - projection rebuilds
  - owner profile compilation paths
- Acceptance:
  - governance backend is executable and auditable
- Related Files:
  - `application/`
  - `docs/PROGRAM-STATUS.md`
- Related Tests:
  - `./gradlew.bat :application:test`
  - `./gradlew.bat build`

### JAVA-032 Stage 5 Legacy Importer Dry-Run And Mapping Core

- Requirement: `REQ-030`
- Stage: 5
- Status: completed
- Dependencies: `JAVA-026`, `JAVA-027`, `JAVA-028`
- Inputs:
  - current Python repository data
- Outputs:
  - import mappings
  - dry-run pipeline
  - mapping validation output
- Acceptance:
  - importer can parse and report without mutating the target graph
- Related Files:
  - `legacy-importer/`
  - `docs/MIGRATION-LEDGER.md`
- Related Tests:
  - `./gradlew.bat :legacy-importer:test`
  - `./gradlew.bat build`

### JAVA-031 Stage 4 Contract Unification

- Requirement: `REQ-021`
- Stage: 4
- Status: completed
- Dependencies: `JAVA-029`, `JAVA-030`
- Inputs:
  - expanded REST and CLI surface
- Outputs:
  - unified error-code model
  - normalized response envelope and CLI output conventions
- Acceptance:
  - interface contracts are documented and consistent across REST and CLI
- Related Files:
  - `interfaces-rest/`
  - `interfaces-cli/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `./gradlew.bat :interfaces-rest:test`
  - `./gradlew.bat :interfaces-cli:test`
  - `./gradlew.bat build`
  - `py -3.12 ops/remote/run_neo4j_smoke.py`

### JAVA-029 Stage 4 Resource Family A REST And CLI

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: `JAVA-026`, `JAVA-028`
- Inputs:
  - completed stage 3 seams for relationships, host contracts, and snapshots
- Outputs:
  - REST and CLI resources for `relationships`, `host-contracts`, and `snapshots`
- Acceptance:
  - V1 resource family A is reachable through both REST and CLI
- Related Files:
  - `interfaces-rest/`
  - `interfaces-cli/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `./gradlew.bat :interfaces-rest:test :interfaces-cli:test`

### JAVA-030 Stage 4 Resource Family B REST And CLI

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: `JAVA-027`
- Inputs:
  - completed stage 3 seams for owner profile facts and managed agent specs
- Outputs:
  - REST and CLI resources for `owner-profile-facts` and `managed-agent-specs`
- Acceptance:
  - V1 resource family B is reachable through both REST and CLI
- Related Files:
  - `interfaces-rest/`
  - `interfaces-cli/`
  - `boot-app/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `./gradlew.bat :interfaces-rest:test :interfaces-cli:test`

### JAVA-028 Stage 3 Snapshot Continuity Service Slice

- Requirement: `REQ-012`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-025`
- Inputs:
  - stable snapshot domain rules
  - persistence baseline
- Outputs:
  - snapshot application services
  - create, read, and restore protection flows
- Acceptance:
  - snapshot continuity flows are executable through the application layer
- Related Files:
  - `application/`
  - `docs/SCHEMA-GRAPH.md`
- Related Tests:
  - `:application:test`

### JAVA-027 Stage 3 Owner Profile And Managed Agent Service Slice

- Requirement: `REQ-011`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-025`
- Inputs:
  - stable governance and persistence baseline
- Outputs:
  - owner profile fact services
  - managed agent specification services
- Acceptance:
  - governance services exist for owner profile facts and managed agent specs
- Related Files:
  - `application/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `:application:test`

### JAVA-026 Stage 3 Relationship And Host Contract Service Slice

- Requirement: `REQ-010`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-025`
- Inputs:
  - stable runtime and graph persistence baseline
- Outputs:
  - relationship and host contract application services
  - command and query DTOs for the relationship context
- Acceptance:
  - relationship and host contract flows can run without legacy scripts
- Related Files:
  - `application/`
  - `docs/SCHEMA-GRAPH.md`
- Related Tests:
  - `:application:test`

### JAVA-025 Stage 3 Shared Extension Seam Decomposition

- Requirement: `REQ-013`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-003`, `JAVA-010`
- Inputs:
  - stable post-stage-2 persistence baseline
  - current stage 3 hotspot map from `docs/PARALLEL-EXECUTION-PLAN.md`
- Outputs:
  - explicit stage 3 shared extension seam
  - recalibrated task dependencies for the three stage 3 service slices
  - documented serial vs parallel write boundaries for stage 3
- Acceptance:
  - stage 3 hotspot ownership is explicit
  - `JAVA-026`, `JAVA-027`, and `JAVA-028` no longer start from an undefined shared seam
- Related Files:
  - `application/`
  - `infra-neo4j/`
  - `docs/PARALLEL-EXECUTION-PLAN.md`
  - `docs/REQUIREMENT-BACKLOG.md`
- Related Tests:
  - `:domain-core:test`
  - `:application:test`
  - `:infra-neo4j:test`
  - `py -3.12 ops/remote/run_neo4j_smoke.py`

### JAVA-018 Stage 2 Remote Persistence Smoke

- Requirement: `REQ-002`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-010`, `JAVA-011`, `JAVA-016`
- Inputs:
  - live remote Neo4j verification node
  - `neo4j` boot profile
  - existing REST and application service paths
- Outputs:
  - reproducible persistence-backed smoke verification flow
  - documented remote runtime command sequence
- Acceptance:
  - a real app path runs against the remote Neo4j node without falling back to `InMemoryBeingStore`
  - verification result is recorded in `PROGRAM-STATUS.md` and `TEST-STATUS.md`
- Related Files:
  - `boot-app/`
  - `ops/remote/`
  - `docs/REMOTE-VERIFICATION-NODE.md`
- Related Tests:
  - `py -3.12 ops/remote/run_neo4j_smoke.py`

### JAVA-010 Stage 2 Application To Neo4j Adapter Integration

- Requirement: `REQ-003`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-003`
- Inputs:
  - `BeingStore` application port
  - `infra-neo4j` SDN entities and repository slice
- Outputs:
  - Neo4j-backed `BeingStore` adapter
  - mapping path between domain aggregate, application services, and persistence layer
- Acceptance:
  - application services can run on a real persistence adapter instead of only the in-memory store
  - integration tests are ready to target Neo4j through the real adapter path
- Related Files:
  - `application/`
  - `infra-neo4j/`
  - `testkit/`
- Related Tests:
  - `:infra-neo4j:test`
  - `:boot-app:test`

### JAVA-003 Stage 2 Neo4j Persistence And Events

- Requirement: `REQ-003`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-002`
- Inputs:
  - stable stage 1 aggregate and invariants
  - graph node and edge model
- Outputs:
  - Neo4j constraint catalog
  - migration baseline
  - persistence mappings
  - `DomainEvent` audit persistence path
- Acceptance:
  - baseline migration assets exist in the repo
  - persistence mappings compile cleanly
  - migration and persistence integration verification can run against a real Neo4j target
- Related Files:
  - `infra-neo4j/`
  - `docs/SCHEMA-GRAPH.md`
  - `docs/REQUIREMENT-BACKLOG.md`
- Related Tests:
  - `:infra-neo4j:test`

### JAVA-024 Stage 2 Requirement Backlog Governance

- Requirement: `REQ-001`
- Stage: 2
- Status: completed
- Dependencies: none
- Inputs:
  - current tracking docs
  - approved requirement governance plan
- Outputs:
  - `docs/REQUIREMENT-BACKLOG.md`
  - requirement links across status, task, and resume docs
  - requirement backlinks from contract and migration docs
- Acceptance:
  - all in-progress and planned tasks trace back to `REQ-*`
  - `PROGRAM-STATUS`, `TASK-BOARD`, and `REQUIREMENT-BACKLOG` do not conflict
- Related Files:
  - `docs/REQUIREMENT-BACKLOG.md`
  - `docs/PROGRAM-STATUS.md`
  - `docs/TASK-BOARD.md`
  - `docs/RESUME-RUNBOOK.md`
- Related Tests:
  - documentation consistency review

### JAVA-011 Remote Verification Node Enablement

- Requirement: `REQ-002`
- Stage: 2
- Status: completed
- Dependencies: none
- Inputs:
  - provided server `114.67.156.250`
  - local remote probe script
- Outputs:
  - remote Neo4j verification node or an explicit blocked-state record
- Acceptance:
  - remote account can run Docker workloads for Neo4j verification
  - Neo4j container can be started or the exact image/runtime blocker is documented with command evidence
- Related Files:
  - `ops/remote/`
  - `docs/REMOTE-VERIFICATION-NODE.md`
  - `docs/PARALLEL-EXECUTION-PLAN.md`
- Related Tests:
  - `py -3.12 ops/remote/probe_server.py`

### JAVA-015 Stage 4 CLI Delivery Slice

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: none
- Inputs:
  - `BeingService`
  - `LeaseService`
  - `ReviewService`
  - `interfaces-cli` Picocli baseline
- Outputs:
  - first functional CLI commands for currently implemented service flows
  - focused CLI tests
- Acceptance:
  - supported being, lease, and review flows can be invoked from Picocli commands
  - `:interfaces-cli:test` passes
- Related Files:
  - `interfaces-cli/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `:interfaces-cli:test`

### JAVA-016 Stage 2 Boot Profile Activation

- Requirement: `REQ-002`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-003`, `JAVA-010`
- Inputs:
  - `Neo4jBeingStore`
  - existing `boot-app` configuration
- Outputs:
  - profile-safe boot wiring for `memory` and `neo4j`
  - focused Spring tests for profile behavior
- Acceptance:
  - default `memory` boot path remains green
  - `neo4j` profile can resolve the real adapter when the infrastructure beans are present
- Related Files:
  - `boot-app/`
  - `docs/PROGRAM-STATUS.md`
- Related Tests:
  - `:boot-app:test`

### JAVA-017 Stage 4 REST Admin Slice

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: none
- Inputs:
  - current application service contracts
  - REST envelope design
- Outputs:
  - controllers and request DTOs for beings, sessions, leases, reviews, and canonical projection rebuilds
  - focused controller tests
- Acceptance:
  - `:interfaces-rest:test` passes
  - documented REST surface matches current implementation
- Related Files:
  - `interfaces-rest/src/main/java/com/openclaw/digitalbeings/interfaces/rest/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `:interfaces-rest:test`

### JAVA-001 Stage 0 Bootstrap Baseline

- Stage: 0
- Status: completed
- Dependencies: none
- Inputs:
  - approved architecture and documentation protocol
  - target project root `C:\Users\16343\.openclaw\digital-beings-java`
- Outputs:
  - Gradle multi-module project
  - Spring Boot startup baseline
  - Neo4j docker-compose baseline
  - full-trace documentation set
- Acceptance:
  - `gradlew.bat build` succeeds
  - app startup baseline exists through `:boot-app:test`
  - `docs/` set is complete
  - `RESUME-RUNBOOK.md` is usable
- Related Files:
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `boot-app/`
  - `docs/`
- Related Tests:
  - `:domain-core:test`
  - `:boot-app:test`
  - root `build`

### JAVA-002 Stage 1 Domain Model And Invariants

- Stage: 1
- Status: completed
- Dependencies: `JAVA-001`
- Inputs:
  - stage 0 baseline
  - fixed node and edge model
- Outputs:
  - bounded context packages
  - domain entities and value objects
  - invariants and state transitions
- Acceptance:
  - domain unit tests cover core invariants
- Related Files:
  - `domain-core/`
  - `docs/SCHEMA-GRAPH.md`
  - `docs/DECISIONS.md`
- Related Tests:
  - `:domain-core:test`

### JAVA-012 Parallel Infra Neo4j Persistence Slice

- Stage: 2
- Status: completed
- Dependencies: `JAVA-002`
- Inputs:
  - stage 1 aggregate model
- Outputs:
  - SDN node entities
  - repository interfaces
  - aggregate mapper
- Acceptance:
  - `:infra-neo4j:test` passes
- Related Files:
  - `infra-neo4j/src/main/java/com/openclaw/digitalbeings/infrastructure/neo4j/persistence/`
- Related Tests:
  - `:infra-neo4j:test`

### JAVA-013 Parallel Application Service Contracts

- Stage: 3 prep
- Status: completed
- Dependencies: `JAVA-002`
- Inputs:
  - stage 1 aggregate model
- Outputs:
  - `BeingStore` port
  - in-memory store
  - service contracts and DTOs
- Acceptance:
  - `:application:test` passes
- Related Files:
  - `application/src/main/java/com/openclaw/digitalbeings/application/`
- Related Tests:
  - `:application:test`

### JAVA-014 Parallel Testkit Neo4j Foundation

- Stage: 2 support
- Status: completed
- Dependencies: `JAVA-001`
- Inputs:
  - Testcontainers baseline
- Outputs:
  - reusable Neo4j container factory
  - connection details value object
  - container session wrapper
- Acceptance:
  - `:testkit:test` passes
- Related Files:
  - `testkit/src/main/java/com/openclaw/digitalbeings/testkit/`
- Related Tests:
  - `:testkit:test`

## Archived / Superseded

### JAVA-004 Stage 3 Application Services

- Status: superseded
- Replaced By:
  - `JAVA-026`
  - `JAVA-027`
  - `JAVA-028`
- Reason:
  - requirement backlog governance split the generic stage 3 umbrella into requirement-aligned tasks

### JAVA-005 Stage 4 REST And CLI V1

- Status: superseded
- Replaced By:
  - `JAVA-029`
  - `JAVA-030`
  - `JAVA-031`
- Reason:
  - requirement backlog governance split the generic stage 4 umbrella into requirement-aligned interface tasks

### JAVA-019 Stage 3 Relationship And Host Contract Services

- Status: superseded
- Replaced By:
  - `JAVA-026`
- Reason:
  - the implementation plan split the stage 3 relationship work into a dedicated post-seam service slice

### JAVA-020 Stage 3 Owner Profile And Managed Agent Services

- Status: superseded
- Replaced By:
  - `JAVA-027`
- Reason:
  - the implementation plan split the stage 3 governance work into a dedicated post-seam service slice

### JAVA-021 Stage 3 Snapshot Continuity Services

- Status: superseded
- Replaced By:
  - `JAVA-028`
- Reason:
  - the implementation plan split the stage 3 snapshot work into a dedicated post-seam service slice

### JAVA-022 Stage 4 REST And CLI Resource Completion

- Status: superseded
- Replaced By:
  - `JAVA-029`
  - `JAVA-030`
- Reason:
  - the implementation plan split stage 4 resource delivery into two resource families for safer parallel work

### JAVA-023 Stage 4 Contract Unification

- Status: superseded
- Replaced By:
  - `JAVA-031`
- Reason:
  - the implementation plan reissued the contract-normalization work under the new stage 4 split

### JAVA-006 Stage 5 Legacy Importer

- Status: superseded
- Replaced By:
  - `JAVA-032`
  - `JAVA-033`
- Reason:
  - the implementation plan split importer work into dry-run/mapping and full replay/reporting phases

### JAVA-007 Stage 6 Governance Loop Hardening

- Status: superseded
- Replaced By:
  - `JAVA-034`
  - `JAVA-035`
- Reason:
  - the implementation plan split governance work into backend capability and jobs/reporting phases
