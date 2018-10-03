Scenario e-commerce process :
--

* What happens ?
    * Api will load a pool of customers and products from the database
    * It will associate actions to each customer
    * After that it will insert a row into the table : 'bench' to say "OK I'm ready"
    * When we saw this row added, we can change the boolean to true into table bench where id = 'starter'
    * When we change this boolean, the api run it scenario

* What does this scenario ?
    * Customers arrive asynchronously moreover each customer have a thinking time (random 0 upto 50ms)
    * Each action for a customer are executed synchronously
    * Each actions are sent to a "Manager request"
    * Manager request is a pool of threads, and each thread will unstack action and execute it
    to the database. this process is synchronous, each threads wait the db's response
