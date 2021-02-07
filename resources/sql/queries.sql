-- :name create-event! :! :n
-- :doc creates a new event record
insert into events
(id, datetime, dish, creator)
values (:id, :datetime, :dish, :creator)

-- :name get-event :? :1
-- :doc retrieves a event record given the id
select * from events
where id = :id

-- :name get-latest-event :? :1
-- :doc retrieves latest event record
select * from events
order by created_at desc
limit 1

-- :name add-guest! :! :n
-- :doc adds guest of event
insert into guests
(event, name)
values (:event-id, :name)

-- :name get-guests :? :*
-- :doc get guests of event
select name from guests
where event = :event-id
