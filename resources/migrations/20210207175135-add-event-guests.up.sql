create table guests
(
    event varchar(22) references events (id),
    name  varchar(100) not null
);
