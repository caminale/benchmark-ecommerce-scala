# benchmark-ecommerce-scala
Benchmark Cockroach/Spanner and Fauna in the future, on e-commerce scenarios
The goal of this project it's to bench/test different distributed & transactional databases.
For this moment we only test OLTP transactions, maybe in the future OLAP scenario will be implemented.
This api will simulate client charge on db, like 30k customers with differrent kind of actions 
as : open an order, read product, insert product into basket, validate order...

## Local deployment

### Prerequisites

You will need the following things properly installed on your computer.


#### mac OSX
##### prerequies for local deployment
* [sbt](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Mac.html)
`brew update & brew install sbt` 
* [Docker](https://docs.docker.com/docker-for-mac/install/) 
* [docker-compose](https://docs.docker.com/compose/install/) 

#### Linux
##### prerequies for local deployment
* [sbt](https://www.scala-sbt.org/0.13/docs/Installing-sbt-on-Linux.html) 
* [Docker](https://docs.docker.com/install/linux/docker-ce/ubuntu/) 
* [docker-compose](https://docs.docker.com/compose/install/) 



prerequies for gcp deployment
-
go [here](docs/gcp-configs.md) to config your account gcp

![alt text](public/images/global_stack_cockroach_local.png "global stack")

### On GCP 
 ---> sooon
 
## Launch db + api
### To launch db cockroach in local

To launch a cockroach Cluster & HAproxy loadbalancer with docker-compose :

```
cd cockroach_Deployment
docker-compose up
```
#### To launch api in local


##### After this commands lines, we have to configure [application.conf](conf/application.conf).
* Set url to 0.0.0.0:5432 correspond to the haproxy's ip 
* Set typeDB to 'cockroach'
* Start api enter this line into the root project's directory:
```
sbt run
``` 

##### Or you can run it in docker's container :

* pull docker image : 
    
    ``
    docker pull camelotte/scala-api:Vconcurrence 
    ``
    
    or 
    
    ``docker pull docker pull camelotte/scala-api:V80_20 
      ``
* docker run : 
``
docker run camelotte/scala-api:V80_20 
``

* Launch customer scenario benchmark : 
```
curl 0.0.0.0:9000/customer/scenario
```




## scala api e-commerce scenario logic :


more about scenario flow [here](docs/api-scenario-logic.md)

Project architecture 
-

* **app** : contains all the code of the bench
* **conf** : contains configuration db + api and conf about logs

to see more click [here](docs/archi-code.md)


sudo docker run -d -t -i -e DATABASE_URL='jdbc:postgresql://0.0.0.0:5432/octo?tcpKeepAlive=true?socketTimeout=0?sslmode=disable' \-e NBR_THREADS=100 \-e TYPE_DB='cockroach' \-e NBR_CUSTOMERS=200 \-e NBR_PRODUCTS=1000 \-e NBR_PRODUCTS_TO_ADD=1 \-e NBR_STOCK=1000 \-e TIMEOUT=80 \ --name scalaapibenchmark scala-api:Vconcurrence
sudo docker run -d -t -i -e DATABASE_URL='jdbc:postgresql://0.0.0.0:5432/octo?tcpKeepAlive=true?socketTimeout=0?sslmode=disable' \-e NBR_THREADS=100 \-e TYPE_DB='cockroach' \-e NBR_CUSTOMERS=200 \-e NBR_PRODUCTS=1000 \-e NBR_PRODUCTS_TO_ADD=1 \-e NBR_STOCK=1000 \-e TIMEOUT=80 --name scalaapibenchmark camelotte/scala-api:Vconcurrence
cockroach dump octo --insecure > backup.sql