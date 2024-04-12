.PHONY: seed start stop sql

DB_HOST ?= localhost
DB_PORT ?= 25432
DB_USER ?= postgres
DB_PASSWORD ?= password
DB_NAME ?= transaction_demo

sql:
	PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -d $(DB_NAME) -U $(DB_USER)
