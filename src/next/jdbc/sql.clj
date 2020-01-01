;; copyright (c) 2019-2020 Sean Corfield, all rights reserved

(ns next.jdbc.sql
  "Some utility functions that make common operations easier by
  providing some syntactic sugar over `execute!`/`execute-one!`.

  This is intended to provide a minimal level of parity with
  `clojure.java.jdbc` (`insert!`, `insert-multi!`, `query`, `find-by-keys`,
  `get-by-id`, `update!`, and `delete!`).

  For anything more complex, use a library like HoneySQL
  https://github.com/jkk/honeysql to generate SQL + parameters.

  The following options are supported:
  * `:table-fn` -- specify a function used to convert table names (strings)
      to SQL entity names -- see the `next.jdbc.quoted` namespace for the
      most common quoting strategy functions,
  * `:column-fn` -- specify a function used to convert column names (strings)
      to SQL entity names -- see the `next.jdbc.quoted` namespace for the
      most common quoting strategy functions.

  In addition, `find-by-keys` supports `:order-by` to add an `ORDER BY`
  clause to the generated SQL."
  (:require [next.jdbc :refer [execute! execute-one!]]
            [next.jdbc.sql.builder
             :refer [for-delete for-insert for-insert-multi
                     for-query for-update]]))

(set! *warn-on-reflection* true)

(defn insert!
  "Syntactic sugar over `execute-one!` to make inserting hash maps easier.

  Given a connectable object, a table name, and a data hash map, inserts the
  data as a single row in the database and attempts to return a map of generated
  keys."
  ([connectable table key-map]
   (insert! connectable table key-map {}))
  ([connectable table key-map opts]
   (execute-one! connectable
                 (for-insert table key-map opts)
                 (merge {:return-keys true} opts))))

(defn insert-multi!
  "Syntactic sugar over `execute!` to make inserting columns/rows easier.

  Given a connectable object, a table name, a sequence of column names, and
  a vector of rows of data (vectors of column values), inserts the data as
  multiple rows in the database and attempts to return a vector of maps of
  generated keys.

  Note: this expands to a single SQL statement with placeholders for every
  value being inserted -- for large sets of rows, this may exceed the limits
  on SQL string size and/or number of parameters for your JDBC driver or your
  database!"
  ([connectable table cols rows]
   (insert-multi! connectable table cols rows {}))
  ([connectable table cols rows opts]
   (if (seq rows)
     (execute! connectable
               (for-insert-multi table cols rows opts)
               (merge {:return-keys true} opts))
     [])))

(defn query
  "Syntactic sugar over `execute!` to provide a query alias.

  Given a connectable object, and a vector of SQL and its parameters,
  returns a vector of hash maps of rows that match."
  ([connectable sql-params]
   (query connectable sql-params {}))
  ([connectable sql-params opts]
   (execute! connectable sql-params opts)))

(defn find-by-keys
  "Syntactic sugar over `execute!` to make certain common queries easier.

  Given a connectable object, a table name, and either a hash map of
  columns and values to search on or a vector of a SQL where clause and
  parameters, returns a vector of hash maps of rows that match.

  If the `:order-by` option is present, add an `ORDER BY` clause. `:order-by`
  should be a vector of column names or pairs of column name / direction,
  which can be `:asc` or `:desc`."
  ([connectable table key-map]
   (find-by-keys connectable table key-map {}))
  ([connectable table key-map opts]
   (execute! connectable (for-query table key-map opts) opts)))

(defn get-by-id
  "Syntactic sugar over `execute-one!` to make certain common queries easier.

  Given a connectable object, a table name, and a primary key value, returns
  a hash map of the first row that matches.

  By default, the primary key is assumed to be `id` but that can be overridden
  in the five-argument call."
  ([connectable table pk]
   (get-by-id connectable table pk :id {}))
  ([connectable table pk opts]
   (get-by-id connectable table pk :id opts))
  ([connectable table pk pk-name opts]
   (execute-one! connectable (for-query table {pk-name pk} opts) opts)))

(defn update!
  "Syntactic sugar over `execute-one!` to make certain common updates easier.

  Given a connectable object, a table name, a hash map of columns and values
  to set, and either a hash map of columns and values to search on or a vector
  of a SQL where clause and parameters, perform an update on the table."
  ([connectable table key-map where-params]
   (update! connectable table key-map where-params {}))
  ([connectable table key-map where-params opts]
   (execute-one! connectable
                 (for-update table key-map where-params opts)
                 opts)))

(defn delete!
  "Syntactic sugar over `execute-one!` to make certain common deletes easier.

  Given a connectable object, a table name, and either a hash map of columns
  and values to search on or a vector of a SQL where clause and parameters,
  perform a delete on the table."
  ([connectable table where-params]
   (delete! connectable table where-params {}))
  ([connectable table where-params opts]
   (execute-one! connectable (for-delete table where-params opts) opts)))
