# Requirement Backlog

## Purpose And Rules

- 杩欐槸 `digital-beings-java` 鐨勯暱鏈熼渶姹傚敮涓€涓婃父鏉冨▉鏂囨。銆?- 浠讳綍鏂板鏂瑰悜銆佽寖鍥村彉鍖栥€佽法闃舵浜嬮」锛屽繀椤诲厛钀藉埌杩欓噷锛屽啀鎷嗗埌 `docs/TASK-BOARD.md`銆?- `TASK-BOARD` 鍙壙鎺ュ凡鎷嗚В鍒版墽琛屽眰鐨?`JAVA-*` 浠诲姟锛屼笉鍐嶇洿鎺ュ彂鏄庢柊鐨勯暱鏈熻寖鍥淬€?- 鏈渶姹傛睜鍙洖濉湭瀹屾垚鍜屾湭鏉ラ渶姹傦紱宸插畬鎴愮殑 stage 0 鍜?stage 1 鍘嗗彶鑳屾櫙缁х画淇濈暀鍦?`CHANGE-JOURNAL` 鍜?`TASK-BOARD`銆?- 闇€姹備富閿浐瀹氫负 `REQ-*`锛涗竴涓?`REQ-*` 鍙互瀵瑰簲澶氫釜 `JAVA-*`锛屼絾涓€涓?`JAVA-*` 鍙兘褰掑睘涓€涓?`REQ-*`銆?
## Status Dictionary

| Status | Meaning |
| --- | --- |
| `candidate` | 鏂瑰悜琚瘑鍒紝浣嗗皻鏈繘鍏ヨ繎鏈熸壙璇?|
| `approved` | 鏂瑰悜宸茬‘璁わ紝绛夊緟鍓嶇疆鏉′欢鎴栨墽琛屾媶瑙?|
| `ready` | 渚濊禆銆侀獙鏀躲€佸奖鍝嶉潰宸茶ˉ榻愶紝鍙互鐩存帴鎷嗘墽琛屼换鍔?|
| `in_progress` | 姝ｅ湪琚綋鍓嶉樁娈典富鍔ㄦ帹杩?|
| `blocked` | 宸插惎鍔紝浣嗚鏄庣‘闃诲 |
| `done` | 闇€姹傞獙鏀跺凡缁忓畬鎴?|
| `deferred` | 鏆傛椂寤跺悗锛屼笉杩涘叆杩戞湡璁″垝 |

## Priority Dictionary

| Priority | Meaning |
| --- | --- |
| `P0` | 褰撳墠闃舵鍏冲彛锛屼笉瀹屾垚灏变笉鑳芥寮忔敹鍙?|
| `P1` | 杩戞湡蹇呴』杩涘叆瀹炵幇锛屽睘浜庝笅涓€闃舵涓荤嚎 |
| `P2` | 宸茬‘璁わ紝浣嗕緷璧栧墠缃樁娈靛畬鎴?|
| `P3` | 鏂瑰悜宸插畾锛屼絾鐭湡涓嶅惎鍔?|

## Requirement Type Dictionary

| Type | Meaning |
| --- | --- |
| `platform` | 鏍稿績骞冲彴搴曞骇銆佹寔涔呭寲銆佸涓昏繍琛岃兘鍔?|
| `domain` | 鏍稿績棰嗗煙妯″瀷銆佹湇鍔°€佷笟鍔¤鍒?|
| `interface` | REST銆丆LI銆佸绾︺€侀敊璇ā鍨?|
| `import` | 閬楃暀鏁版嵁瀵煎叆涓庤縼绉?|
| `governance` | 瀹℃煡銆佹姇褰便€佹不鐞嗛棴鐜€佺瓥鐣ユ墽琛?|
| `ops` | 杩愮淮銆佺伨澶囥€侀壌鏉冦€佺洃鎺с€佹仮澶?|

## Stage Roadmap Summary

| Stage Window | Primary Outcome | Requirement IDs | Exit Signal |
| --- | --- | --- | --- |
| Stage 2 | 瀹屾垚鐪熷疄鎸佷箙鍖栭摼璺笌杩佺Щ楠岃瘉闂幆 | `REQ-001`, `REQ-002`, `REQ-003` | `TEST-STATUS` 涓?stage 2 gate 鍙浆涓洪€氳繃 |
| Stage 3 | 鍏堟媶鍏变韩鎵╁睍缂濓紝鍐嶈ˉ榻愬墿浣欐牳蹇?bounded contexts 鐨勫簲鐢ㄦ湇鍔?| `REQ-013`, `REQ-010`, `REQ-011`, `REQ-012` | stage 3 鐑偣鏂囦欢琚В鑰︼紝application 灞傝鐩?relationships銆乬overnance銆乻napshot |
| Stage 4 | 瀹屾垚 V1 REST/CLI 璧勬簮涓庡绾︾粺涓€ | `REQ-020`, `REQ-021` | REST 涓?CLI 瀵归綈涓斿绾︾ǔ瀹?|
| Stage 5-6 | COMPLETED - Import & Governance | `REQ-030`, `REQ-040` | REQ-040 done, REQ-030 in progress | 瀹屾垚閬楃暀瀵煎叆涓庢不鐞嗛棴鐜?| `REQ-030`, `REQ-040` | 瀵煎叆鍙璁★紝娌荤悊閾捐矾鍙繍琛?|
| Stage 7 (Horizon A) | COMPLETED - Ops hardening | `REQ-070`~`REQ-074` | All tests green |
| Stage 8 (Horizon B) | 瀹屾垚瀹夸富閫傞厤涓庣敓浜у寲 | `REQ-050`, `REQ-060` | 鏂囦欢椹卞姩璺緞閫€鍑轰富閾撅紝鎭㈠婕旂粌閫氳繃 |

## Active Requirement Index

| ID | Title | Stage | Type | Priority | Status | Downstream Tasks |
| --- | --- | --- | --- | --- | --- | --- |
| `REQ-001` | 闇€姹傛睜娌荤悊灞傝惤鍦?| 2 | `platform` | `P0` | `done` | `JAVA-024` |
| `REQ-002` | 杩滅 Neo4j 鎸佷箙鍖栫儫闆鹃獙璇?| 2 | `platform` | `P0` | `done` | `JAVA-011`, `JAVA-016`, `JAVA-018` |
| `REQ-003` | Neo4j migration 涓庨泦鎴愰獙璇侀棴鐜?| 2 | `platform` | `P0` | `done` | `JAVA-003`, `JAVA-010` |
| `REQ-013` | Stage 3 鍏变韩鎵╁睍缂濇媶鍒?| 3 | `platform` | `P1` | `done` | `JAVA-025` |
| `REQ-010` | Relationship 涓?Host Contract 鏈嶅姟琛ュ叏 | 3 | `domain` | `P1` | `done` | `JAVA-026` |
| `REQ-011` | Owner Profile 涓?Managed Agent 鏈嶅姟琛ュ叏 | 3 | `governance` | `P1` | `done` | `JAVA-027` |
| `REQ-012` | Snapshot Continuity 鏈嶅姟琛ュ叏 | 3 | `domain` | `P1` | `done` | `JAVA-028` |
| `REQ-020` | REST/CLI V1 瀵归綈 | 4 | `interface` | `P1` | `done` | `JAVA-029`, `JAVA-030` |
| `REQ-021` | 缁熶竴閿欒妯″瀷涓庢帴鍙ｅ绾︽敹鍙?| 4 | `interface` | `P1` | `done` | `JAVA-031` |
| `REQ-030` | Legacy Importer | 5 | `import` | `P2` | `done` | `JAVA-032`, `JAVA-033` |
| `REQ-040` | Governance Loop Hardening | 6 | `governance` | `P2` | `done` | `JAVA-034`, `JAVA-035` |
| `REQ-050` | Host Adapters (superseded) | 7 | `platform` | `P3` | `deferred` | `JAVA-008` (superseded by REQ-080~REQ-086) |
| `REQ-060` | Portable Snapshot API | 9 | `migration` | `P0` | `candidate` | `JAVA-C2a`, `JAVA-C2b`, `JAVA-C3a`, `JAVA-C3b` |
| `REQ-061` | Migration Drill & Cutover | 9 | `migration` | `P0` | `candidate` | `JAVA-C1`, `JAVA-C4`, `JAVA-C5`, `JAVA-C6` |
| `REQ-062` | Python Legacy Cleanup | 10 | `migration` | `P0` | `candidate` | `JAVA-D1`, `JAVA-D2`, `JAVA-D3`, `JAVA-D4` |

## Requirement Details

### REQ-001 闇€姹傛睜娌荤悊灞傝惤鍦?
- `Title:` 闇€姹傛睜娌荤悊灞傝惤鍦?- `Stage Target:` 2
- `Type:` `platform`
- `Priority:` `P0`
- `Status:` `done`
- `Problem / Value:` 褰撳墠浠撳簱宸茬粡鏈夊己鎵ц杩借釜锛屼絾缂哄皯缁熶竴鐨勯暱鏈熼渶姹傚叆鍙ｏ紝瀵艰嚧闃舵鏂瑰悜銆佽兘鍔涚己鍙ｅ拰鏂囨。鍚屾鐐瑰垎鏁ｅ湪澶氫釜鏂囦欢涓€?- `In Scope:` 鍒涘缓 `docs/REQUIREMENT-BACKLOG.md`锛涚粰娲昏穬浠诲姟琛?`REQ-*` 褰掑睘锛涙妸 `PROGRAM-STATUS`銆乣TASK-BOARD`銆乣RESUME-RUNBOOK`銆乣API-CONTRACT`銆乣SCHEMA-GRAPH`銆乣MIGRATION-LEDGER` 鎺ュ埌闇€姹傛睜銆?- `Out of Scope:` 鐩存帴瀹炵幇 stage 2 smoke锛涙柊澧炰笟鍔℃湇鍔★紱鏂板瀵瑰鎺ュ彛銆?- `Dependencies:` none
- `Downstream Tasks:` `JAVA-024`
- `Affected Docs / Interfaces:` `docs/PROGRAM-STATUS.md`, `docs/TASK-BOARD.md`, `docs/RESUME-RUNBOOK.md`, `docs/API-CONTRACT.md`, `docs/SCHEMA-GRAPH.md`, `docs/MIGRATION-LEDGER.md`
- `Acceptance:` 鎵€鏈?`in_progress` 鍜?`planned` 鐨?`JAVA-*` 閮借兘杩芥函鍒?`REQ-*`锛涙仮澶嶉『搴忔樉寮忓寘鍚渶姹傛睜锛涢渶姹傛睜鎴愪负鍚庣画鑼冨洿鍙樻洿鐨勪笂娓稿叆鍙ｃ€?- `Verification:` 鏂囨。涓€鑷存€т汉宸ユ牳瀵癸紱妫€鏌?`PROGRAM-STATUS`銆乣TASK-BOARD`銆乣REQUIREMENT-BACKLOG` 涓夎€呬笉鍐茬獊銆?- `Risks / Notes:` 宸插畬鎴愮殑 stage 0 鍜?stage 1 涓嶅己鍒跺洖濉负 `REQ-*`锛屼繚鐣欏巻鍙蹭笂涓嬫枃鍗冲彲銆?
### REQ-002 杩滅 Neo4j 鎸佷箙鍖栫儫闆鹃獙璇?
- `Title:` 杩滅 Neo4j 鎸佷箙鍖栫儫闆鹃獙璇?- `Stage Target:` 2
- `Type:` `platform`
- `Priority:` `P0`
- `Status:` `done`
- `Problem / Value:` 杩滅 Neo4j 鑺傜偣宸茬粡鍙敤锛屼絾杩樻病鏈夎瘉鎹瘉鏄庡簲鐢ㄧ湡瀹炶蛋鐨勬槸 `neo4j` profile 鍜屾寔涔呭寲閾捐矾锛岃€屼笉鏄唴瀛樿矾寰勩€?- `In Scope:` 鍚姩 `neo4j` profile锛涜繛鎺ヨ繙绔?Neo4j锛涙墽琛岃嚦灏戜竴鏉?create being -> session -> lease -> review -> projection 鐨勭湡瀹炲啓搴撴祦绋嬶紱璁板綍杩愯鍛戒护涓庨獙璇佺粨鏋溿€?- `Out of Scope:` 瀹屾暣 migration 楠岃瘉锛涘叏閲忔帴鍙ｉ獙鏀讹紱瀵煎叆鍜屾不鐞嗛棴鐜€?- `Dependencies:` `REQ-001`, 宸插畬鎴愮殑 `JAVA-011` 涓?`JAVA-016`
- `Downstream Tasks:` `JAVA-011`, `JAVA-016`, `JAVA-018`
- `Affected Docs / Interfaces:` `docs/PROGRAM-STATUS.md`, `docs/TEST-STATUS.md`, `docs/REMOTE-VERIFICATION-NODE.md`, `boot-app` runtime profile docs
- `Acceptance:` 鑷冲皯瀹屾垚涓€娆＄湡瀹炴寔涔呭寲 smoke锛涚‘璁ゆ病鏈夊洖閫€鍒?`InMemoryBeingStore`锛涚粨鏋滃啓鍥炵姸鎬佹枃妗ｃ€?- `Verification:` `py -3.12 ops/remote/run_neo4j_smoke.py` 浜?2026-03-21 閫氳繃锛沗DigitalBeingsNeo4jSmokeIT` 鏂█ `BeingStore` 涓?`Neo4jBeingStore`锛屽苟瀹屾垚鐪熷疄鍐欏叆涓庤鍥炪€?- `Risks / Notes:` 鏈湴浠嶆棤 Docker锛屾墍浠ヨ闇€姹傜殑瀹瑰櫒绾ч獙璇佺洰鏍囦互杩滅鑺傜偣涓哄噯銆?
### REQ-003 Neo4j Migration 涓庨泦鎴愰獙璇侀棴鐜?
- `Title:` Neo4j migration 涓庨泦鎴愰獙璇侀棴鐜?- `Stage Target:` 2
- `Type:` `platform`
- `Priority:` `P0`
- `Status:` `done`
- `Problem / Value:` schema baseline銆丼DN 鏄犲皠鍜?`BeingStore` 閫傞厤宸插瓨鍦紝浣嗚繕缂哄湪鐪熷疄 Neo4j 鐩爣涓婄殑 migration 涓庨泦鎴愰獙璇侀棴鐜€?- `In Scope:` 缁存姢 migration 璧勪骇锛涘畬鎴愭寔涔呭寲鏄犲皠鏀跺彛锛涜鐪熷疄閫傞厤璺緞鍏峰闆嗘垚楠岃瘉鍏ュ彛锛涙妸楠岃瘉缁撴灉鎸傚洖 stage 2 gate銆?- `Out of Scope:` 鏂颁笟鍔′笂涓嬫枃鎵╁睍锛涘涓绘帴鍏ワ紱鐢熶骇鍖栬繍缁淬€?- `Dependencies:` `REQ-001`
- `Downstream Tasks:` `JAVA-003`, `JAVA-010`
- `Affected Docs / Interfaces:` `docs/SCHEMA-GRAPH.md`, `docs/TEST-STATUS.md`, `infra-neo4j`, `testkit`
- `Acceptance:` migration 鍙墽琛岋紱鐪熷疄閫傞厤璺緞鑳芥帴鍏ラ獙璇侊紱stage 2 gate 鐨勨€渕igration and persistence integration鈥濅笉鍐嶅彧鏄鍒掗」銆?- `Verification:` `./gradlew.bat build` 浜?2026-03-21 閫氳繃锛沗:infra-neo4j:test` 涓?`RemoteNeo4jMigrationSmokeTest` 涓?`RemoteNeo4jBeingStoreIntegrationTest` 鍧囧凡閫氳繃骞跺懡涓繙绔?Neo4j 鐩爣銆?- `Risks / Notes:` 璇ラ渶姹備笌 `REQ-002` 寮虹浉鍏筹紝浣?`REQ-002` 鏇村亸搴旂敤閾捐矾 smoke锛宍REQ-003` 鏇村亸搴曞骇楠岃瘉闂幆銆?
### REQ-013 Stage 3 鍏变韩鎵╁睍缂濇媶鍒?
- `Title:` Stage 3 鍏变韩鎵╁睍缂濇媶鍒?- `Stage Target:` 3
- `Type:` `platform`
- `Priority:` `P1`
- `Status:` `done`
- `Problem / Value:` 褰撳墠 stage 3 鐨勪笁鏉′富绾夸細鍏卞悓瑙︾ `Being.java`銆乣BeingStore.java`銆乣BeingNode.java`銆乣BeingNodeMapper.java` 绛夌儹鐐规枃浠躲€傚鏋滀笉鍏堟媶鍑虹ǔ瀹氭墿灞曠紳锛岀洿鎺ュ苟琛屾帹杩?`REQ-010`銆乣REQ-011`銆乣REQ-012` 鍙細鍒堕€犻珮鍐茬獊鍚堝苟鍜屽弽澶嶈繑宸ャ€?- `In Scope:` 璇嗗埆骞跺浐鍖?stage 3 鐨勫叡浜墿灞曠紳锛涙槑纭摢浜涘彉鏇村繀椤诲厛鍦ㄥ叡浜眰瀹屾垚锛涗负鍚庣画涓夋潯涓氬姟绾垮缓绔嬫洿灏忕殑鍐欏叆杈圭晫锛涙妸骞惰鏉′欢鍜屼换鍔′緷璧栧啓鍥?backlog銆乼ask board銆乸arallel execution plan銆?- `Out of Scope:` 鐩存帴浜や粯 relationships銆乬overnance銆乻napshot 鐨勫畬鏁存湇鍔″疄鐜帮紱鏂板 REST/CLI 璧勬簮锛涘鍏ユ垨娌荤悊 job銆?- `Dependencies:` `REQ-003`
- `Downstream Tasks:` `JAVA-025`
- `Affected Docs / Interfaces:` `docs/PARALLEL-EXECUTION-PLAN.md`, `docs/TASK-BOARD.md`, `docs/PROGRAM-STATUS.md`, `application`, `infra-neo4j`
- `Acceptance:` stage 3 鐨勫叡浜儹鐐逛笌鎵╁睍缂濊鏄惧紡瀹氫箟锛沗JAVA-019`銆乣JAVA-020`銆乣JAVA-021` 鐨勪緷璧栧拰鍚姩椤哄簭琚噸鏂版牎鍑嗭紱鍚庣画瀹炵幇绾跨▼鐭ラ亾鍝簺鏂囦欢浠嶅繀椤讳覆琛屻€?- `Verification:` `JAVA-025` 宸茶惤鍦帮紱`BeingNode`銆乣BeingNodeMapper` 涓庨樁娈?3 鎵╁睍鑺傜偣宸插畬鎴愭敹鍙ｏ紝`./gradlew.bat :domain-core:test :application:test :infra-neo4j:test` 涓?`py -3.12 ops/remote/run_neo4j_smoke.py` 浜?2026-03-21 閫氳繃銆?- `Risks / Notes:` 杩欐槸 stage 3 鐨勫叆鍙ｉ渶姹傦紝涓嶆槸闀挎湡涓氬姟鑳藉姏鏈韩锛涘畬鎴愬悗鎵嶈€冭檻閲嶆柊鎵撳紑澶氱嚎绋嬪疄鐜般€?
### REQ-010 Relationship 涓?Host Contract 鏈嶅姟琛ュ叏

- `Title:` Relationship 涓?Host Contract 鏈嶅姟琛ュ叏
- `Stage Target:` 3
- `Type:` `domain`
- `Priority:` `P1`
- `Status:` `done`
- `Problem / Value:` 褰撳墠 application 灞傚彧瑕嗙洊 being銆乴ease銆乺eview銆乸rojection锛宺elationships 鍜?host contracts 杩樻病鏈夌ǔ瀹氭湇鍔¤竟鐣屻€?- `In Scope:` 鍏崇郴鍥句笌 host contract 鐨?application services銆佸懡浠?鏌ヨ妯″瀷銆佸繀瑕佺殑璇诲彇瑙嗗浘銆?- `Out of Scope:` REST/CLI 瀵瑰鏆撮湶锛沴egacy 瀵煎叆锛涙不鐞?job銆?- `Dependencies:` `REQ-013`
- `Downstream Tasks:` `JAVA-026`
- `Affected Docs / Interfaces:` `docs/SCHEMA-GRAPH.md`, `docs/API-CONTRACT.md`
- `Acceptance:` application 灞傝兘鐙珛瀹屾垚 relationship 鍜?host contract 鐨勬牳蹇冩祦绋嬨€?- `Verification:` `RelationshipServiceTest` 涓?`HostContractServiceTest` 宸插姞鍏?`:application:test`锛屽苟浜?2026-03-21 閫氳繃銆?- `Risks / Notes:` 杩欐槸 stage 4 鎺ュ彛琛ュ叏鐨勫墠缃潯浠讹紝涔熸槸 stage 3 鐨?lead slice銆?
### REQ-011 Owner Profile 涓?Managed Agent 鏈嶅姟琛ュ叏

- `Title:` Owner Profile 涓?Managed Agent 鏈嶅姟琛ュ叏
- `Stage Target:` 3
- `Type:` `governance`
- `Priority:` `P1`
- `Status:` `done`
- `Problem / Value:` 娌荤悊涓婁笅鏂囧凡缁忔湁 schema 瑙勫垝锛屼絾 owner profile facts 鍜?managed agent specs 杩樻病鏈夊彲鎵ц鏈嶅姟灞傘€?- `In Scope:` owner profile fact 鏈嶅姟銆乵anaged agent spec 鏈嶅姟銆佹不鐞嗙浉鍏?DTO 涓庤鍙栨ā鍨嬨€?- `Out of Scope:` review cockpit 鍏ㄩ棴鐜紱瀹夸富浜嬩欢鎺ュ叆銆?- `Dependencies:` `REQ-013`
- `Downstream Tasks:` `JAVA-027`
- `Affected Docs / Interfaces:` `docs/SCHEMA-GRAPH.md`, `docs/API-CONTRACT.md`, `docs/MIGRATION-LEDGER.md`
- `Acceptance:` 娌荤悊涓婁笅鏂囩殑鏍稿績瀵硅薄鍙€氳繃 application 灞傜鐞嗐€?- `Verification:` `GovernanceServiceTest` 宸插姞鍏?`:application:test`锛屽苟浜?2026-03-21 閫氳繃銆?- `Risks / Notes:` 璇ラ渶姹傚皢鐩存帴褰卞搷 stage 4 鐨勬帴鍙ｈˉ榻愬拰 stage 5 鐨勫鍏ユ槧灏勩€?
### REQ-012 Snapshot Continuity 鏈嶅姟琛ュ叏

- `Title:` Snapshot Continuity 鏈嶅姟琛ュ叏
- `Stage Target:` 3
- `Type:` `domain`
- `Priority:` `P1`
- `Status:` `done`
- `Problem / Value:` snapshot 瑙勫垯瀛樺湪浜庨鍩熷眰锛屼絾娌℃湁瀹屾暣鐨?application service 鏉ユ壙鎺ュ垱寤恒€佽鍙栧拰鎭㈠淇濇姢娴佺▼銆?- `In Scope:` snapshot application services銆佽鍙栬鍥俱€佹仮澶嶄繚鎶ょ害鏉熻惤鍦般€?- `Out of Scope:` 鐢熶骇绾х伨澶囩紪鎺掞紱杩滅▼鎭㈠婕旂粌銆?- `Dependencies:` `REQ-013`
- `Downstream Tasks:` `JAVA-028`
- `Affected Docs / Interfaces:` `docs/SCHEMA-GRAPH.md`, `docs/API-CONTRACT.md`
- `Acceptance:` snapshot continuity 鐩稿叧娴佺▼鍙€氳繃 application 灞傛墽琛屽苟楠岃瘉绾︽潫銆?- `Verification:` `SnapshotServiceTest` 宸插姞鍏?`:application:test`锛屽苟浜?2026-03-21 閫氳繃锛屽寘鎷?`POST_RESTORE` 蹇収淇濇姢鐢ㄤ緥銆?- `Risks / Notes:` 杩欐槸鍚庣画 productionization 鐨勪笟鍔″墠鎻愶紝涓嶆槸鏈€缁堣繍缁村疄鐜版湰韬€?
### REQ-020 REST/CLI V1 瀵归綈

- `Title:` REST/CLI V1 瀵归綈
- `Stage Target:` 4
- `Type:` `interface`
- `Priority:` `P1`
- `Status:` `done`
- `Problem / Value:` 褰撳墠 REST 鍜?CLI 鍙湁灞€閮ㄨ祫婧愶紝涓旀湭鏉ヨ鍒掕祫婧愯繕鏈湪涓や晶瀵归綈銆?- `In Scope:` 涓?`relationships`銆乣host-contracts`銆乣owner-profile-facts`銆乣managed-agent-specs`銆乣snapshots` 琛ラ綈 REST 涓?CLI锛涚‘淇濅袱渚ц鐩栬寖鍥翠竴鑷淬€?- `Out of Scope:` 閿欒妯″瀷缁熶竴锛涚敓浜ч壌鏉冦€?- `Dependencies:` `REQ-010`, `REQ-011`, `REQ-012`
- `Downstream Tasks:` `JAVA-015`, `JAVA-017`, `JAVA-029`, `JAVA-030`
- `Affected Docs / Interfaces:` `docs/API-CONTRACT.md`, `interfaces-rest`, `interfaces-cli`
- `Acceptance:` V1 瑙勫垝璧勬簮鍦?REST 鍜?CLI 涓や晶鍧囧彲璁块棶锛屼笖瑕嗙洊鑼冨洿涓€鑷淬€?- `Verification:` REST/CLI parity tests or equivalent checklist completed. 2026-03-21: `JAVA-029` and `JAVA-030` passed with `./gradlew.bat :interfaces-rest:test :interfaces-cli:test`, and the full `./gradlew.bat build` remained green after Stage 4 closeout. - `Risks / Notes:` Stage 4 has moved to done; future interface expansion should happen through new requirements rather than reopening this V1 parity scope.
### REQ-021 缁熶竴閿欒妯″瀷涓庢帴鍙ｅ绾︽敹鍙?
- `Title:` 缁熶竴閿欒妯″瀷涓庢帴鍙ｅ绾︽敹鍙?- `Stage Target:` 4
- `Type:` `interface`
- `Priority:` `P1`
- `Status:` `done`
- `Problem / Value:` 褰撳墠 envelope 宸插畾锛屼絾閿欒鐮佹棌銆丆LI 杈撳嚭瑙勮寖鍜屾墿灞曟帴鍙ｅ绾﹁繕娌℃湁鏁翠綋鏀跺彛銆?- `In Scope:` 缁熶竴閿欒鐮佹棌銆佸搷搴?envelope 瑙勫垯銆丆LI 杈撳嚭濂戠害銆佺浉鍏虫枃妗ｅ悓姝ャ€?- `Out of Scope:` 涓氬姟鏈嶅姟琛ュ叏锛涙不鐞?job銆?- `Dependencies:` `REQ-020`
- `Downstream Tasks:` `JAVA-031`
- `Affected Docs / Interfaces:` `docs/API-CONTRACT.md`, `interfaces-rest`, `interfaces-cli`
- `Acceptance:` 閿欒妯″瀷鍜屽绾﹀湪 REST 涓?CLI 涓彲涓€鑷磋В閲婂拰缁存姢銆?- `Verification:` `JAVA-031` landed shared request-envelope helpers, request-family error-code routing, CLI `table/json` output, and passed `./gradlew.bat :interfaces-rest:test :interfaces-cli:test`, `./gradlew.bat build`, and `py -3.12 ops/remote/run_neo4j_smoke.py`. - `Risks / Notes:` This requirement is now done; later interface additions should conform to the normalized contract instead of redefining it.
### REQ-030 Legacy Importer

- `Title:` Legacy Importer
- `Stage Target:` 5
- `Type:` `import`
- `Priority:` `P2`
- `Status:` `done`
- `Problem / Value:` 褰撳墠 Java 宸ョ▼杩樻病鏈夋妸 Python 浠撳簱鏁版嵁瀵煎叆鍒板浘鏁版嵁搴撶殑姝ｅ紡鑳藉姏銆?- `In Scope:` 涓€娆℃€у鍏?beings銆乺elationships銆乤ccepted reviews銆乷wner profile銆乴ease 鍘嗗彶銆乻napshots锛沝ry-run锛涘紓甯告姤鍛婏紱璁℃暟鎶ュ憡銆?- `Out of Scope:` 鍙屽啓鍏煎锛涚户缁緷璧?Python 杩愯鏃躲€?- `Dependencies:` `REQ-010`, `REQ-011`, `REQ-012`
- `Downstream Tasks:` `JAVA-032`, `JAVA-033`
- `Affected Docs / Interfaces:` `docs/MIGRATION-LEDGER.md`, `legacy-importer`
- `Acceptance:` 瀵煎叆鍙噸澶嶆墽琛岋紱寮傚父鍜岀己澶遍」鑳借鎶ュ憡锛涘浘涓€鑷存€х粨鏋滃彲瀹¤銆?- `Verification:` 鏈€灏忔牱鏈€佸畬鏁存牱鏈€佽剰鏁版嵁鏍锋湰瀵煎叆楠岃瘉閫氳繃銆?- `Risks / Notes:` 璇ラ渶姹傚彧澶勭悊涓€娆℃€ц縼绉伙紝涓嶅紩鍏ュ弻鍐欍€?
### REQ-040 Governance Loop Hardening

- `Title:` Governance Loop Hardening
- `Stage Target:` 6
- `Type:` `governance`
- `Priority:` `P2`
- `Status:` `in_progress`
- `Problem / Value:` review銆乸rojection銆乷wner profile銆乻tale lease cleanup 杩樻湭褰㈡垚鍙璁＄殑娌荤悊闂幆銆?- `In Scope:` review cockpit 鍚庣銆乸rojection rebuild銆乷wner profile compile銆乬raph consistency job銆乻tale lease cleanup銆?- `Out of Scope:` 瀹夸富浜嬩欢鍗忚锛涚敓浜х伨澶囥€?- `Dependencies:` `REQ-020`, `REQ-021`, `REQ-030`
- `Downstream Tasks:` `JAVA-034`, `JAVA-035`
- `Affected Docs / Interfaces:` `docs/PROGRAM-STATUS.md`, `jobs`, `application`
- `Acceptance:` 娌荤悊閾捐矾鍙繍琛屻€佸彲杩借釜銆佸彲瀹¤銆?- `Verification:` governance flow 娴嬭瘯鍜?job 杩愯鎶ュ憡閫氳繃銆?- `Risks / Notes:` 璇ラ渶姹傛槸 stage 6 涓荤嚎锛屼笉搴斾笌瀹夸富鎺ュ叆娣峰仛涓€鍧椼€?
### REQ-050 Host Adapters

- `Title:` Host Adapters
- `Stage Target:` 7
- `Type:` `platform`
- `Priority:` `P3`
- `Status:` `candidate`
- `Problem / Value:` 鏈€缁堥渶瑕佷互鏄惧紡浜嬩欢 API 鏇挎崲褰撳墠 Python 涓栫晫閲岀殑鏂囦欢椹卞姩瀹夸富琛屼负銆?- `In Scope:` OpenClaw 涓?Codex 椋庢牸瀹夸富浜嬩欢鎺ュ叆銆乨rift reporting銆佸涓诲悎鍚屾牎楠屻€?- `Out of Scope:` 鐢熶骇鐩戞帶涓庣伨澶囥€?- `Dependencies:` `REQ-040`
- `Downstream Tasks:` `JAVA-008`
- `Affected Docs / Interfaces:` `docs/API-CONTRACT.md`, future host adapter modules
- `Acceptance:` 瀹夸富浜嬩欢璺緞鍙浛浠ｆ枃浠堕┍鍔ㄤ富閾俱€?- `Verification:` host adapter 娴嬭瘯鍜?drift 鎶ュ憡鍙繍琛屻€?- `Risks / Notes:` 褰撳墠闃舵鍙繚鐣欐柟鍚戯紝涓嶈繘鍏ヨ繎鏈熸墽琛屻€?
### REQ-060 Productionization

- `Title:` Productionization
- `Stage Target:` 8
- `Type:` `ops`
- `Priority:` `P3`
- `Status:` `candidate`
- `Problem / Value:` 椤圭洰灏氭湭鍏峰閴存潈銆佹仮澶嶃€佺伨澶囥€佺洃鎺у拰婕旂粌鑳藉姏銆?- `In Scope:` auth銆乥ackup銆乺estore銆乨isaster recovery銆乵onitoring銆佹仮澶嶆紨缁冦€?- `Out of Scope:` 鏂颁笟鍔¤兘鍔涙墿寮犮€?- `Dependencies:` `REQ-050`
- `Downstream Tasks:` `JAVA-009`
- `Affected Docs / Interfaces:` `docs/RESUME-RUNBOOK.md`, future ops modules
- `Acceptance:` 瀹屾垚鑷冲皯涓€娆℃仮澶嶆紨缁冨苟鍙璁°€?- `Verification:` recovery and resilience tests 閫氳繃銆?- `Risks / Notes:` 杩欐槸鐢熶骇鍖栭樁娈甸渶姹傦紝褰撳墠鍙繚鐣欏€欓€夋柟鍚戙€?

---

## Stage 7: Ops Hardening (Horizon A)

### REQ-070 Health & Readiness Probe Completion

- Title: Health & Readiness Probe Completion
- Stage Target: 7
- Type: platform
- Priority: P0
- Status: done
- Problem / Value: Kubernetes/load balancer needs confirmation of service availability, but current health check is incomplete.
- In Scope: Neo4jHealthIndicator, InstanceHealthIndicator, IntegrationHealthEndpoint
- Out of Scope: New service connection refactoring
- Dependencies: REQ-040
- Downstream Tasks: JAVA-A01
- Affected Docs / Interfaces: boot-app/src/main/resources/application.yml, docs/RESUME-RUNBOOK.md
- Acceptance: /actuator/health/readiness returns UP when Neo4j is reachable and schema is initialized
- Verification: curl /actuator/health/readiness returns UP when Neo4j is reachable

### REQ-071 LeaseExpiryJob & SessionCleanupJob

- Title: Lease Expiry Scanner + Session Cleanup Job
- Stage Target: 7
- Type: ops
- Priority: P0
- Status: done
- Problem / Value: Stale leases and sessions cannot auto-expire, requiring manual intervention from operators.
- In Scope: LeaseExpiryJob, StatusHeartbeatJob, SessionCleanupJob
- Out of Scope: New service triggers
- Dependencies: REQ-040
- Downstream Tasks: JAVA-A02
- Affected Docs / Interfaces: jobs/ module, application/LeaseService.java
- Acceptance: Stale lease/session gets marked automatically, with domain event record
- Verification: Job executes and lease/session is marked as stale, with audit record

### REQ-072 Restart / Backup / DR Runbook

- Title: Restart Backup DR Runbook Documentation
- Stage Target: 7
- Type: ops
- Priority: P1
- Status: done
- Problem / Value: No complete incident playbook exists; most procedures require self-researched troubleshooting.
- In Scope: Server restart procedure, Neo4j backup procedure, incident runbook
- Out of Scope: Remote DR auto-trigger
- Dependencies: REQ-070, REQ-071
- Downstream Tasks: JAVA-A03
- Affected Docs / Interfaces: docs/RESUME-RUNBOOK.md, docs/INCIDENT-RUNBOOK.md
- Acceptance: A newly attached AI can self-serve to understand service restart procedures
- Verification: AI can follow documented procedures without manual guidance

### REQ-073 Neo4j Migration Script Framework

- Title: Neo4j Migration Script Framework
- Stage Target: 7
- Type: platform
- Priority: P1
- Status: done
- Problem / Value: Database schema changes need proper version control and testing.
- In Scope: V001__initial_schema initialization, neo4j-migrations configuration
- Out of Scope: Remote migrations
- Dependencies: REQ-003
- Downstream Tasks: JAVA-A04
- Affected Docs / Interfaces: infra-neo4j/src/main/resources/db/migration/, docs/SCHEMA-GRAPH.md
- Acceptance: Schema changes are versioned, tests have entry point
- Verification: Fresh Neo4j starts with migration auto-applied

### REQ-074 Memory Store Production Guard

- Title: Memory Store Production Profile Guard
- Stage Target: 7
- Type: platform
- Priority: P1
- Status: done
- Problem / Value: Currently memory profile is just a Spring parameter, no hard stop for production switches.
- In Scope: Profile validation, warning log on startup
- Out of Scope: Strict profile enforcement
- Dependencies: none
- Downstream Tasks: JAVA-A05
- Affected Docs / Interfaces: boot-app/src/main/resources/application.yml, boot-app/.../MemoryBeingStoreConfiguration.java
- Acceptance: Production neo4j profile cannot start with InMemoryStore implementation
- Verification: Start boot app and no error occurs

---

## Stage 8: Host Adapter Integration (Horizon B)

### REQ-080 OpenClaw Session Auto-Registration

- Title: OpenClaw Session Auto-Registration to Java
- Stage Target: 8
- Type: platform
- Priority: P0
- Status: done
- Problem / Value: When OpenClaw starts a session, the script still manages its own session, without auto-registering session and lease handoff results.
- In Scope: POST /sessions self-registration, session close handover
- Out of Scope: Any Python script internals modification
- Dependencies: REQ-071
- Downstream Tasks: JAVA-B01
- Affected Docs / Interfaces: interfaces-rest/LeaseController.java, application/LeaseService.java
- Acceptance: OpenClaw session starts, self-registers successfully with lease handoff
- Verification: After startup, sessions list includes one entry, self-registration successful

### REQ-081 AuthorityLease Auto-Acquire/Release

- Title: AuthorityLease Auto-Acquire/Release on Session Lifecycle
- Stage Target: 8
- Type: platform
- Priority: P0
- Status: done
- Problem / Value: The lease lifecycle cannot self-coordinate, requires manual coordination to execute lease handoff.
- In Scope: Session register auto-acquires lease, same session close releases
- Out of Scope: Manual quick switch
- Dependencies: REQ-080
- Downstream Tasks: JAVA-B02
- Affected Docs / Interfaces: application/LeaseService.java, domain-core/AuthorityLease.java
- Acceptance: One session acquires lease, second session trying to acquire gets 409 Conflict
- Verification: Two competing acquires, second gets 409 Conflict

### REQ-082 Runtime Injection Context API

- Title: Runtime Injection Context API
- Stage Target: 8
- Type: interface
- Priority: P0
- Status: done
- Problem / Value: OpenClaw needs injection context; currently still needs to re-access the file to get the running role.
- In Scope: GET /beings/{id}/injection-context self-service
- Out of Scope: OpenClaw modifications; Python script internals modification
- Dependencies: REQ-080
- Downstream Tasks: JAVA-B03
- Affected Docs / Interfaces: interfaces-rest/BeingController.java, application/BeingService.java
- Acceptance: A Spring Boot starts, Java can self-assemble being as central service provider and resolve
- Verification: GET /beings/{id}/injection-context returns 200 and includes being.yaml and canonical state

### REQ-083 Embodiment Bundle Compilation from Java

- Title: Embodiment Bundle Compiled from Java
- Stage Target: 8
- Type: platform
- Priority: P0
- Status: done
- Problem / Value: When OpenClaw obtains a being session, it still has to go through files to get the script and execute handoff orders.
- In Scope: /injection-context includes canonical, service card
- Out of Scope: Any Python script internals modification
- Dependencies: REQ-081
- Downstream Tasks: JAVA-B04
- Affected Docs / Interfaces: application/InjectionContextService.java, interfaces-rest/BeingController.java
- Acceptance: Service card info and document header service card info are one
- Verification: Two competing acquires, Java script both succeed

### REQ-084 EvolutionSignal Closed Loop

- Title: EvolutionSignal Roundtrip: OpenClaw to Java Review Queue
- Stage Target: 8
- Type: platform
- Priority: P0
- Status: done
- Problem / Value: Python script active reflection still has traces, but Java backend still has a blind spot in the downstream review lane.
- In Scope: POST /reviews converts self-reflection into review item, IDENTITY lane purification
- Out of Scope: Any Python script internals modification
- Dependencies: REQ-082
- Downstream Tasks: JAVA-B05
- Affected Docs / Interfaces: interfaces-rest/ReviewController.java, application/ReviewService.java
- Acceptance: EvolutionSignal converts to Java review item; IDENTITY_CANDIDATE still has manual review gate
- Verification: After one evolution signal is sent, Java backend review request succeeds

### REQ-085 Python DigitalBeingsClient Delegation Layer (superseded)

- Title: (superseded — Python scripts are deleted, not delegated to)
- Stage Target: 8
- Type: platform
- Priority: P1
- Status: superseded
- Superseded By: REQ-062 (Python Legacy Cleanup — scripts are deleted entirely, not bridged)
- Reason: Java is the authoritative runtime. Python/digital-beings repository is migrated and archived, not bridged.
- Downstream Tasks: none

### REQ-086 Dual-Host Lease Coordination

- Title: Dual-Host (OpenClaw + Codex) Lease Coordination
- Stage Target: 8
- Type: platform
- Priority: P0
- Status: done
- Problem / Value: When two hosts share a being, Java should coordinate the lease, but there is currently no task to quickly switch the handoff.
- In Scope: Within window both sessions share being; lease host coordinates handoff; lease switch has manual entry
- Out of Scope: Any Python script internals modification
- Dependencies: REQ-081
- Downstream Tasks: JAVA-B07
- Affected Docs / Interfaces: application/LeaseService.java, domain-core/AuthorityLease.java
- Acceptance: Both hosts share being, lease host coordinates handoff, session and script are any consistent
- Verification: Both hosts share being, lease host coordinates handoff, session and script are any consistent

---

## Stage 9: Migration & Cutover (Horizon C)

### REQ-090 Portable Snapshot API

- Title: Portable Snapshot API — Export Being State as Self-Contained JSON
- Stage Target: 9
- Type: migration
- Priority: P0
- Status: candidate
- Problem / Value: There is no way to serialize a Being's full runtime state from Neo4j into a portable format that can be restored elsewhere. Python has file-copy snapshots; Java has no portable serialization.
- In Scope: POST /beings/{id}/snapshots/export — serializes full Being aggregate (identity, relationships, canonical projection, owner profile, sessions, leases, domain events) into a self-contained PortableSnapshot JSON. POST /beings/{id}/snapshots/import — restores a PortableSnapshot into the Neo4j store as a live Being.
- Out of Scope: Snapshot versioning/migration between schema versions; incremental sync
- Dependencies: REQ-083 (ServiceCard / Embodiment Bundle — export includes service card data)
- Downstream Tasks: JAVA-C2a, JAVA-C2b, JAVA-C3a, JAVA-C3b
- Affected Docs / Interfaces: application/snapshot/PortableSnapshotService.java, interfaces-rest/SnapshotController.java, domain-core/Being.java
- Acceptance: A being exported from one Neo4j instance can be imported into another and all domain invariants hold.
- Verification: Export → stop source → import to fresh Neo4j → all GET endpoints return identical data.

### REQ-091 Migration Drill & Identity Regression

- Title: End-to-End Migration Drill — Export, Switch, Verify, Rollback
- Stage Target: 9
- Type: migration
- Priority: P0
- Status: candidate
- Problem / Value: There is no automated end-to-end test that proves the full migration path works. Manual testing is error-prone and not repeatable.
- In Scope: Shell script (bash) that runs: export snapshot → verify snapshot JSON → import to staging Neo4j → run identity regression queries → report pass/fail. Includes rollback: re-import original snapshot and verify no drift.
- Out of Scope: High-risk production cutover; automated remediation
- Dependencies: REQ-090
- Downstream Tasks: JAVA-C4
- Affected Docs / Interfaces: docs/MIGRATION-DRILL.md, shell script under ops/
- Acceptance: `run_migration_drill.sh` exits 0 for both being guan-guan and xiao-yi.
- Verification: Drill script runs to completion against the live Python source data without errors.

### REQ-092 Gray Cutover: Low-Risk Scenarios to Java

- Title: Gray Cutover — First Low-Risk Production Cutover
- Stage Target: 9
- Type: migration
- Priority: P0
- Status: candidate
- Problem / Value: No plan exists for safely cutting over production traffic to Java without risking identity continuity.
- In Scope: Define low-risk cutover criteria (which being, which operations). Execute first cutover against a non-critical being. Verify all runtime paths (injection-context, service-card, session, lease, evolution-signal) work against the Java backend.
- Out of Scope: Full simultaneous cutover of all beings; high-risk operations
- Dependencies: REQ-091
- Downstream Tasks: JAVA-C1
- Affected Docs / Interfaces: docs/MIGRATION-LEDGER.md, OpenClaw adapter config
- Acceptance: Cutover being runs 100% on Java runtime with no regression in session/lease/injection-context.
- Verification: Cutover being's being.yaml remains unchanged in Python repo (read-only archive), all activity flows through Java.

---

## Stage 10: Python Legacy Cleanup (Horizon D)

### REQ-093 OpenClaw Runtime Script Deletion

- Title: Delete OpenClaw Runtime Management Scripts
- Stage Target: 10
- Type: migration
- Priority: P0
- Status: candidate
- Problem / Value: After successful cutover, operate_runtime_hub.py, operate_openclaw_host.py, operate_codex_host.py, dispatch_runtime_commands.py and similar scripts must be deleted. They can no longer manage session or lease state.
- In Scope: Delete all scripts that manage session, lease, injection-context, or being-runtime state. Replace with pure OpenClaw adapter that only executes instructions from Java REST API.
- Out of Scope: Deleting non-runtime scripts (bridge/assess_*, bridge/summarize_*, etc.)
- Dependencies: REQ-092
- Downstream Tasks: JAVA-D1
- Affected Docs / Interfaces: digital-beings/bridge/operate_runtime_hub.py, digital-beings/bridge/operate_*.py
- Acceptance: No script in digital-beings can manage session or lease state. All such scripts are deleted.
- Verification: `grep -r "session" bridge/*.py | grep -v "source" | wc -l` returns 0 for session/lease/being-runtime management.

### REQ-094 being_runtime.py and bridge_openclaw.py Deletion

- Title: Delete being_runtime.py and bridge_openclaw.py
- Stage Target: 10
- Type: migration
- Priority: P0
- Status: candidate
- Problem / Value: being_runtime.py (session management, bundle reading, yaml parsing) and bridge_openclaw.py (runtime state management) are the core Python runtime scripts. After cutover they must be deleted.
- In Scope: Delete both files and all functions that only exist to serve runtime state (load_runtime_bundle, load_injection_context, etc.). The digital-beings repo becomes a pure static configuration archive.
- Out of Scope: bridge/*.py scripts that are not runtime (assess_*, extract_*, sync_* non-runtime)
- Dependencies: REQ-093
- Downstream Tasks: JAVA-D2
- Affected Docs / Interfaces: digital-beings/bridge/being_runtime.py, digital-beings/bridge/bridge_openclaw.py
- Acceptance: being_runtime.py and bridge_openclaw.py do not exist.
- Verification: `ls bridge/being_runtime.py bridge/bridge_openclaw.py` returns "not found".

### REQ-095 digital-beings Repository Archive

- Title: Archive and Freeze digital-beings Python Repository
- Stage Target: 10
- Type: migration
- Priority: P0
- Status: candidate
- Problem / Value: After all runtime scripts are deleted, the digital-beings repository must be clearly marked as archived/read-only so no future work accidentally resumes Python runtime management.
- In Scope: Add ARCHIVED.md to repo root explaining it is read-only. Remove execute permissions on all .py files. Add git tag `migration-complete-YYYYMMDD`. Remove from cron jobs.
- Out of Scope: Actual repo deletion (git history preserved for audit)
- Dependencies: REQ-094
- Downstream Tasks: JAVA-D3
- Affected Docs / Interfaces: digital-beings/ARCHIVED.md
- Acceptance: Repo is frozen, cron jobs removed, no active automation touches it.
- Verification: No cron job references digital-beings scripts; ARCHIVED.md exists in repo root.

### REQ-096 Python Legacy Cleanup — Final Verification

- Title: Final Verification: Zero Python Runtime State Remains
- Stage Target: 10
- Type: migration
- Priority: P0
- Status: candidate
- Problem / Value: After cleanup, need a final automated verification that no Python process can influence Java runtime state.
- In Scope: Run verification queries against Neo4j to confirm all active leases belong to Java-managed sessions only. Verify all injection-context responses originate from Java. Confirm Python repo has no write access to Neo4j.
- Out of Scope: Non-runtime Python script audit
- Dependencies: REQ-095
- Downstream Tasks: JAVA-D4
- Affected Docs / Interfaces: Neo4j constraints, Java runtime logs
- Acceptance: All active leases have sessionId from Java-issued UUIDs (not Python). No injection-context returns data sourced from Python files.
- Verification: Automated check script exits 0.




## Deferred / Watch Pool

- 褰撳墠娌℃湁鏄惧紡 `deferred` 闇€姹傘€?- `REQ-050` 涓?`REQ-060` 浠嶄繚鐣欏湪娲昏穬绱㈠紩涓綔涓?`candidate`锛岀敤浜庢彁閱掍腑杩滄湡鏂瑰悜锛屼絾涓嶈繘鍏ヨ繎鏈熸帓鏈熴€?
## Maintenance Protocol

- 鏂版柟鍚戝厛寤洪渶姹傦紝鍐嶅缓浠诲姟銆?- 闇€姹備粠 `approved` 杩涘叆 `ready` 鍓嶏紝蹇呴』琛ラ綈楠屾敹銆佷緷璧栥€佸奖鍝嶆枃妗ｅ拰涓嬫父浠诲姟璁″垝銆?- 浠诲姟瀹屾垚涓嶈兘鐩存帴浠ｈ〃闃舵瀹屾垚锛屽繀椤诲厛鍥炲啓瀵瑰簲 `REQ-*` 鐨勯獙鏀跺拰楠岃瘉缁撴灉銆?- `PROGRAM-STATUS.md` 蹇呴』濮嬬粓鑳芥寚鍑哄綋鍓嶆鍦ㄦ帹杩涚殑 `REQ-*`銆?- `RESUME-RUNBOOK.md` 蹇呴』鎶?`REQUIREMENT-BACKLOG.md` 绾冲叆鎭㈠椤哄簭銆?- 濡傛灉 `API-CONTRACT.md`銆乣SCHEMA-GRAPH.md`銆乣MIGRATION-LEDGER.md` 鍙戠敓鍙樺寲锛屽繀椤昏兘杩芥函鍒板搴?`REQ-*`銆?- 浠讳綍 handoff 鍓嶉兘瑕佹鏌ュ綋鍓嶄换鍔℃槸鍚︿粛鐒舵寕鍦ㄦ湁鏁堢殑涓婃父闇€姹備笂銆?
