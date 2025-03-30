create table roles_permissions
(
    role_name        varchar(255) not null
        constraint fk6nw4jrj1tuu04j9rk7xwfssd6
            references roles,
    permissions_name varchar(255) not null
        constraint fk9u1xpvjxbdnkca024o6fyr7uu
            references permissions,
    primary key (role_name, permissions_name)
);

alter table roles_permissions
    owner to postgres;

