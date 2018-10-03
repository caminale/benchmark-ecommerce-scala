## Configure application :

Configuration file is : *application.conf* into conf directory.
At the eof there are a dbs object, and an api object. These 2 objects are the configurations of the 
db and configuration of the api. 

Database parameters :
----------
### Cockroach parameters :

cockroach url :
```
cockroach {
       url = "jdbc:postgresql://ip:port/dbname?tcpKeepAlive=true?socketTimeout=0?sslmode=disable"
     }
```
So you only have to change 2 parameters : **ip:port** + **dbname**
:warning: if you are in local with docker-compose deployment, your ip:port is 0.0.0.0:5432

### Spanner parameters :

Google Spanner : 

If you don't have a gcp project setup I redirect you to [this tuto](gcp-configs.md)
If you have already a project gcp setup with an account service key json with the rigths roles spanner 
you can continue : 

```
 spanner {
    projectID = "project-id-gcp"
    instanceID = "spanner-instance-id"
    databaseID = "database-name"
    maxSessions = 500 // one node spanner can have 10K sessions
    pathJsonKey = "service-account-key-json-gcp"
  }
````

The following parameters is applicated for all dbs :

```
  nbThreads = 200
  nbrTotalProductsInDB = 1600000
```
* **nbrThreads** : represents the size of a pool of threads who will manage connection and communication with database.

* **nbrTotalProductsInDB** : represents the total number of products into your db, we need this to randomize offset to get products
is only necessary if you launch multiple scenario in parallel
 
API :
-----

```
  typeDB = "cockroach"
  nbrCustomers = 1000
  nbrProducts = 8000
  nbrProductsToAdd = 1
  stockQuantity = 2000
  timeout = 20
```
* **typeDB** : is the name of the db to test for this moment we have only '**spaner**', '**cockroach**' and soon '**fauna**'
* **nbrCustomers** : represents the number of customers into the benchmark for one api, more there are customers more the bechnmark take time
* **nbrProducts** : represents the size of product that will be affect randomly to customers. Less the size is high more 
there is concurrency on product, beacause customers will do operations on same products
* **nbrProductsToAdd** : represents the number of products to put into customers' bascket
* **stockQuantity** : permit to put the stock of products to the same quantity each time you run the bench, to have idempotent run
* **timeout** : represents the thinking time for each customers, this parameters permit to mix customers' actions