alter table BN_PROC_DEF add MIGRATION_DATE_ NUMBER(19, 0);
update BN_PROC_DEF set MIGRATION_DATE_ = 0;

alter table BN_OEI_ add LOCKED number(1,0);
update BN_OEI_ set LOCKED = 0;

alter table BN_IEI_ add LOCKED number(1,0);
update BN_IEI_ set LOCKED = 0;