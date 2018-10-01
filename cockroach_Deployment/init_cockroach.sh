#!/usr/bin/env bash
set -e

docker-compose exec roach1 ./cockroach sql -e 'CREATE DATABASE octo' --insecure
docker-compose exec roach1 ./cockroach sql -e 'CREATE TABLE octo.accounts (id VARCHAR(20) PRIMARY KEY,name TEXT,email TEXT,balance DECIMAL);' --insecure
docker-compose exec roach1 ./cockroach sql -e 'CREATE TABLE octo.feedback (id VARCHAR(20) PRIMARY KEY, numberTransaction INT);' --insecure
docker-compose exec roach1 ./cockroach sql -e 'CREATE DATABASE IF NOT EXISTS octo' --insecure
docker-compose exec roach1 ./cockroach sql -e 'CREATE TABLE IF NOT EXISTS octo.accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),name TEXT,email TEXT,balance DECIMAL);' --insecure
CREATE TABLE octo.users(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),name TEXT,email TEXT)
CREATE TABLE octo.products(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), stock_id: String, name: String, price: DECIMAL, category: TEXT, uri: TEXT, description: TEXT, brand: TEXT)
CREATE TABLE products(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name TEXT, price DECIMAL, category TEXT, uri TEXT, description TEXT, brand TEXT)

CREATE TABLE products(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name TEXT, price DECIMAL, category TEXT,  description TEXT, brand TEXT)
CREATE TABLE stocks(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), product_id TEXT, stock DECIMAL, zone TEXT)
CREATE TABLE orders(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id TEXT, date TEXT, state TEXT)
CREATE TABLE Product_Order(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), product_id TEXT, order_id TEXT, quantity DECIMAL)



# stock avec FK
CREATE TABLE stocks(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), product_id UUID REFERENCES products(id) ON DELETE CASCADE, stock DECIMAL, zone TEXT)
