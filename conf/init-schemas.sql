/*DROP DATABASE octo CASCADE;
*/
SET TIME ZONE 'Europe/Paris';
CREATE DATABASE octo;
use octo;


BEGIN;

SAVEPOINT cockroach_restart;



-- 1. create the "main tables"

-- create table customer
CREATE TABLE customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name STRING NOT NULL,
    email STRING NOT NULL
); -- OK

-- create table orders (plural name to avoid special database keywords)
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customer(id) ON DELETE CASCADE,
    ordered_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP(),  -- TODO une seule date suffit? ou bien on met une date de création de la commande puis une date pour chaque changement de state?
    state STRING DEFAULT 'PROCESS'
); -- OK

-- create table product
CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name STRING NOT NULL,
    category STRING NOT NULL,
    brand STRING NOT NULL,
    description STRING NOT NULL,
    uri STRING,
    price NUMERIC DEFAULT 10.42,
    visible BOOL NOT NULL DEFAULT TRUE
); -- OK

-- create table stock
CREATE TABLE stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID REFERENCES product(id) ON DELETE CASCADE,
    stock INT NOT NULL DEFAULT 0,
    location STRING NOT NULL DEFAULT 'FRANCE',
    last_delivery TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP(),
    next_delivery TIMESTAMPTZ
); -- OK

-- create table manufacturer
CREATE TABLE manufacturer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name STRING NOT NULL,
    location STRING NOT NULL
); -- OK


-- 2. create the transition tables

-- create table order_product (similar to Basket/Card in semantics)
CREATE TABLE product_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID REFERENCES Product(id) ON DELETE CASCADE,
    order_id UUID REFERENCES Orders(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 0
); -- OK

-- create table manufacturer_stock (similar to supply history in semantics)
CREATE TABLE manufacturer_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manufacturer_id UUID REFERENCES Manufacturer(id) ON DELETE CASCADE,
    stock_id UUID REFERENCES Stock(id) ON DELETE CASCADE,
    quantity INT NOT NULL,
    refuel_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP()
); -- OK




-- 3. insert at least one customer

INSERT INTO customer (name, email) VALUES ('hello', 'hello@octo.com');

RELEASE SAVEPOINT cockroach_restart;

COMMIT;


CREATE VIEW available_products
    AS SELECT p.id, p.name, p.category, p.brand, p.description, p.uri, p.price, s.stock, 'France', '2018-08-20 10:04:43.334968+00:00'
    FROM product AS p
    JOIN stock AS s
    ON p.id = s.product_id ;

