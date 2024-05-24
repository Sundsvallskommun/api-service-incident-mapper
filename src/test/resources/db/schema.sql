
    create table incident (
        id varchar(255) not null,
        created datetime(6),
        jira_issue_key varchar(255),
        last_synchronized_jira datetime(6),
        last_synchronized_pob datetime(6),
        modified datetime(6),
        pob_issue_key varchar(255),
        status varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create index ix_pob_issue_key 
       on incident (pob_issue_key);

    create index ix_jira_issue_key 
       on incident (jira_issue_key);

    create index ix_status 
       on incident (status);

    alter table if exists incident 
       add constraint uq_pob_issue_key unique (pob_issue_key);

    alter table if exists incident 
       add constraint uq_jira_issue_key unique (jira_issue_key);