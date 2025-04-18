create table users
(
    id            integer generated by default as identity
        primary key,
    address       varchar(255),
    created_at    varchar(255),
    email         varchar(255),
    gender        integer,
    password      varchar(255),
    phone         varchar(255),
    profile_image varchar(255),
    updated_at    varchar(255),
    username      varchar(255)
);

alter table users
    owner to postgres;

