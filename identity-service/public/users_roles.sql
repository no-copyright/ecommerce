create table users_roles
(
    user_id    integer      not null
        constraint fk2o0jvgh89lemvvo17cbqvdxaa
            references users,
    roles_name varchar(255) not null
        constraint fkmi9sfx618v14gm89cyw408hqu
            references roles,
    primary key (user_id, roles_name)
);

alter table users_roles
    owner to postgres;

