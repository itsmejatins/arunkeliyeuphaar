alter table T_USER alter column LASTFMSESSIONTOKEN set default null;
update T_USER set LASTFMSESSIONTOKEN = null where LASTFMSESSIONTOKEN = '0';
alter table T_TRACK alter column FORMAT type varchar(50);
alter table T_TRACK add OWNERID varchar(50);
-- alter table T_TRACK drop column OWNERID;