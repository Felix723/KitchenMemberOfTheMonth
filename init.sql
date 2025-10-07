drop table if exists user;
drop table if exists product;
drop table if exists purchase;

create table user(
	username string primary key, 
	email string, 
	password string
);
insert into user values
 ('Felix', 'felix@itu.dk', 'test');

create table product(
	task_category string primary key,
	points int,
	description string
);
insert into product values
 ('high_value', 50, 'Take out Green Trash (Organic/Food Waste) & Clean the oven.'),
 ('medium_value', 30, 'Mopping the floor'),
 ('standard_value', 10, 'Take out Black Trash (General Waste), Take out Recycling Bin, Organising the Drying Rack & Brooming/Vacuuming the floor'),
 ('low_value', 5, 'Wiping the Countertops'),
 ('wild_card', 55, 'Example: Cleaning bottom of the shelves or organizing the cleaning stuff in the cabinet next to the fridges');

create table purchase(
	awardTime timestamp,
	points int,
	username string,
	primary key (awardTime, points, username)
);

insert into purchase values
 ('2025-10-11 9:55'::timestamp, 50, 'felix'),
 ('2025-10-11 9:56'::timestamp, 5, 'emma');