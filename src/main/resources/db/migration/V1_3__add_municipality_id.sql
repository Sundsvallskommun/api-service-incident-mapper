
alter table if exists incident
   add column municipality_id varchar(255) AFTER id;
   
create index ix_municipality_id 
   on incident (municipality_id);