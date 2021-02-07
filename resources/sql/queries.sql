-- :name create-event! :! :n
-- :doc creates a new event record
INSERT INTO events
(id)
VALUES (:id)

-- :name get-event :? :1
-- :doc retrieves a event record given the id
SELECT * FROM events
WHERE id = :id

-- :name get-latest-event :? :1
-- :doc retrieves latest event record
SELECT * FROM events
ORDER BY created_at DESC
LIMIT 1
