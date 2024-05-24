    alter table if exists incident 
       add constraint uq_pob_issue_key unique (pob_issue_key);

    alter table if exists incident 
       add constraint uq_jira_issue_key unique (jira_issue_key);