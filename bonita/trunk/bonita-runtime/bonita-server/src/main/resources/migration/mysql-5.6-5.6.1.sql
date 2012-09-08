alter table BN_OEI_ add LOCKED bit;
update BN_OEI_ set LOCKED = b'0';

alter table BN_IEI_ add LOCKED bit;
update BN_IEI_ set LOCKED = b'0';