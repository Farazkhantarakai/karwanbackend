
alter table domains add column ssl_status varchar(50),
alter table domains add column target varchar(100),  -- cname.karwan.pk
alter table domains add column dns_configured boolean -- true or false for custom domains
alter table domains add column status varchar(50)
;