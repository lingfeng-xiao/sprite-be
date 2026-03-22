# Incident Runbook

## Purpose

This runbook provides step-by-step procedures for handling incidents in the Digital Beings Java system.

## Severity Levels

| Severity | Description | Response Time | Examples |
|----------|-------------|---------------|----------|
| SEV-1 | Critical - Complete outage | Immediate | Neo4j unavailable, complete service down |
| SEV-2 | High - Major functionality impaired | < 1 hour | Health checks failing, data integrity issues |
| SEV-3 | Medium - Minor functionality affected | < 4 hours | Non-critical job failures, degraded performance |
| SEV-4 | Low - Cosmetic or minor issues | Next business day | Log errors, non-critical warnings |

## Incident Response Procedures

### SEV-1: Complete Outage

**Symptoms:**
- Application fails to start
- All health endpoints returning DOWN
- Neo4j connection failures

**Immediate Actions:**
1. Check Neo4j status: `ssh jd "docker ps | grep neo4j"`
2. Check application logs: `ssh jd "journalctl -u digital-beings -n 100"`
3. Verify remote verification node: `py -3.12 ops/remote/run_neo4j_smoke.py`

**Recovery Steps:**
```bash
# 1. Restart Neo4j container
ssh jd "docker restart neo4j"

# 2. Wait for Neo4j to be ready (check logs)
ssh jd "docker logs neo4j --tail 50"

# 3. Restart application
ssh jd "systemctl restart digital-beings"

# 4. Verify health endpoints
curl https://your-domain/actuator/health
```

**Post-Incident:**
- Document root cause in ITERATION-LOG.md
- Update monitoring alerts if gaps found
- Review and update this runbook if procedure failed

### SEV-2: Major Functionality Impaired

**Symptoms:**
- Health probe returning unhealthy
- Lease expiry job not running
- Graph consistency check failing

**Immediate Actions:**
1. Check application health: `curl https://your-domain/actuator/health`
2. Review job execution logs
3. Check graph consistency: `ssh jd "cd digital-beings-java && ./gradlew :jobs:test"`

**Recovery Steps:**
```bash
# 1. Check specific health indicator
curl https://your-domain/actuator/health/readiness

# 2. Review application logs
ssh jd "journalctl -u digital-beings --since '1 hour ago'"

# 3. Run graph consistency job manually
# (requires access to running application)

# 4. If Neo4j issue, restart container
ssh jd "docker restart neo4j"
```

### SEV-3: Minor Functionality Affected

**Symptoms:**
- Non-critical job failures
- Warning logs appearing
- Performance degradation

**Actions:**
1. Review logs for patterns: `ssh jd "grep WARNING /var/log/digital-beings.log"`
2. Check disk space: `ssh jd "df -h"`
3. Review recent deployments
4. Schedule maintenance window if needed

### SEV-4: Cosmetic Issues

**Actions:**
1. Document in issue tracker
2. Prioritize for next sprint
3. Monitor for escalation

## Common Recovery Commands

### Check Application Status
```bash
# Local health check
curl http://localhost:8080/actuator/health

# Remote health check
ssh jd "curl -s http://localhost:8080/actuator/health"
```

### Check Neo4j Status
```bash
# Container status
ssh jd "docker ps | grep neo4j"

# Neo4j logs
ssh jd "docker logs neo4j --tail 100"

# Test Neo4j connectivity
py -3.12 ops/remote/run_neo4j_smoke.py
```

### Restart Services
```bash
# Restart application
ssh jd "systemctl restart digital-beings"

# Restart Neo4j
ssh jd "docker restart neo4j"

# Full restart (Neo4j then app)
ssh jd "docker restart neo4j && sleep 30 && systemctl restart digital-beings"
```

### Backup Procedures

**Neo4j Backup:**
```bash
# Create backup
ssh jd "docker exec neo4j neo4j-admin backup --backup-dir=/backups --graph-name=graph.db"

# Verify backup
ssh jd "ls -la /backups/"
```

**Application State Backup:**
```bash
# Backup being store (if using JSON export)
# (requires running application with export capability)

# Backup configuration
ssh jd "cp -r /etc/digital-beings /backups/config-$(date +%Y%m%d)"
```

## Escalation Path

1. **Level 1**: On-call engineer (current sprint team)
2. **Level 2**: Tech lead / Architecture
3. **Level 3**: Infrastructure team (for Neo4j/hosting issues)

## Post-Incident Review

After each SEV-1 or SEV-2 incident:
1. Document timeline in ITERATION-LOG.md
2. Identify root cause
3. Create action items for prevention
4. Update this runbook if procedures need correction
5. Update monitoring/alerting as needed

## Contact Information

| Role | Contact |
|------|---------|
| Infrastructure | `ops@example.com` |
| On-call | See PagerDuty schedule |
| Architecture | `#architecture` Slack channel |
