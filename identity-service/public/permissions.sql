create table permissions
(
    name        varchar(255) not null
        primary key,
    description varchar(255)
);

alter table permissions
    owner to postgres;

