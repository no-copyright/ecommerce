create table roles
(
    name        varchar(255) not null
        primary key,
    description varchar(255)
);

alter table roles
    owner to postgres;

