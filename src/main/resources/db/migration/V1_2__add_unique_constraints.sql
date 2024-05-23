
    alter table if exists incident 
       add constraint incident_unique_pob_issue_key_constraint unique (pob_issue_key);

    alter table if exists incident 
       add constraint incident_unique_jira_issue_key_constraint unique (jira_issue_key);