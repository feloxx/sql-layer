SELECT * FROM customers LEFT JOIN orders ON customers.cid = orders.cid INNER JOIN items ON orders.oid = items.oid
