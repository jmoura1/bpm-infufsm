alter table BN_PROC_DEF add MIGRATION_DATE_ bigint;
update BN_PROC_DEF set MIGRATION_DATE_ = 0;

alter table BN_OEI_ add LOCKED boolean; 
update BN_OEI_ set LOCKED = false;

alter table BN_IEI_ add LOCKED boolean;
update BN_IEI_ set LOCKED = false;