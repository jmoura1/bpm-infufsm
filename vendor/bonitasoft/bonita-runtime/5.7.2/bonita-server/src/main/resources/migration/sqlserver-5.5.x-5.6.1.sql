alter table BN_PROC_DEF add MIGRATION_DATE_ NUMERIC(19, 0)
go
update BN_PROC_DEF set MIGRATION_DATE_ = 0
go

alter table BN_OEI_ add LOCKED tinyint null
go
update BN_OEI_ set LOCKED = 0
go

alter table BN_IEI_ add LOCKED tinyint null
go
update BN_IEI_ set LOCKED = 0
go