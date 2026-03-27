alter table payments add column if not exists payer_name varchar(80);
alter table payments add column if not exists payer_cpf varchar(14);
