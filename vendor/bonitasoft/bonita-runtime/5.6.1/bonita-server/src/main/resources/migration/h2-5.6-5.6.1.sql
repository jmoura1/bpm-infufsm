alter table BN_OEI_ add LOCKED boolean; 
update BN_OEI_ set LOCKED = false;

alter table BN_IEI_ add LOCKED boolean;
update BN_IEI_ set LOCKED = false;