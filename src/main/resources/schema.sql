create table if not exists runtime_model_config (
    id bigint primary key,
    provider varchar(64) not null,
    model_name varchar(255) not null,
    api_key longtext,
    base_url varchar(2048),
    temperature_value double precision not null,
    max_tokens integer not null,
    updated_at timestamp not null
);

create table if not exists autonomy_policy (
    id bigint primary key,
    mode varchar(64) not null,
    paused boolean not null,
    allow_internal boolean not null,
    allow_readonly boolean not null,
    allow_mutating boolean not null,
    whitelist_json longtext,
    updated_at timestamp not null
);

create table if not exists life_journal_entries (
    id bigint auto_increment primary key,
    entry_type varchar(64) not null,
    title varchar(255) not null,
    detail text not null,
    payload_json longtext,
    created_at timestamp not null
);

create index idx_life_journal_entries_created_at
    on life_journal_entries (created_at desc);

create table if not exists life_command_executions (
    id bigint auto_increment primary key,
    command_id varchar(128) not null unique,
    command_type varchar(64) not null,
    content text not null,
    context_json longtext,
    source varchar(64) not null,
    summary text not null,
    detail text not null,
    success boolean not null,
    impact_json longtext,
    created_at timestamp not null
);

create index idx_life_command_executions_created_at
    on life_command_executions (created_at desc);

create table if not exists life_runtime_state (
    id bigint primary key,
    identity_json longtext not null,
    self_json longtext not null,
    relationship_json longtext not null,
    goals_json longtext not null,
    updated_at timestamp not null
);

create table if not exists action_executions (
    id varchar(36) primary key,
    task_id varchar(255) not null,
    execution_id varchar(255) not null,
    status varchar(64) not null,
    action_type varchar(255),
    target varchar(255),
    parameters longtext,
    request_payload longtext,
    response_payload longtext,
    error_message longtext,
    retry_count integer,
    idempotency_key varchar(255),
    cycle_id varchar(255),
    plan_id varchar(255),
    created_at timestamp null,
    started_at timestamp null,
    finished_at timestamp null,
    updated_at timestamp null
);

create index idx_action_task_id
    on action_executions (task_id);

create index idx_action_status
    on action_executions (status);

create index idx_action_idempotency_key
    on action_executions (idempotency_key);

create index idx_action_cycle_id
    on action_executions (cycle_id);
