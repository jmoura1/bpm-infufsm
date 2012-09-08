alter table BN_PROC_DEF add MIGRATION_DATE_ int8;
update BN_PROC_DEF set MIGRATION_DATE_ = 0;

alter table BN_OEI_ add LOCKED bool;
update BN_OEI_ set LOCKED = false;

alter table BN_IEI_ add LOCKED bool;
update BN_IEI_ set LOCKED = false;
