
    create table incident (
        created datetime(6),
        jira_issue_last_modified datetime(6),
        modified datetime(6),
        pob_issue_last_modified datetime(6),
        id varchar(255) not null,
        jira_issue_key varchar(255),
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