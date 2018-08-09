### About Money Transfer API

Money Transfer API is a RESTFul service to transfer funds between accounts
It is developed with:
*   [Dropwizard] framework which includes: 
    * [Jersey]
    * [Hibernate ORM]
    * [Liquibase]
*   in-memory [HSQLDB]
*   [Apache ActiveMQ] for messaging between web server and order processing workers.
*   [JUnit]


It is possible to run application as standalone "fat-jar". Little efforts (bu)required to use
persistent storage and external message broker.

Money Transfer API contains three resource types: 
*   accounts - representation of customer account, but without contact or personal data
*   exchange rates - currency exchange rates
*   orders - notes about required actions under account.

#### Available services:

| HTTP METHOD        | PATH           |  USAGE |
| ------------- |-------------|:-----|
| GET     | /accounts/{id}  | get account by id |
| POST    | /accounts/{id} | create new account |
| GET     | /rates/{currency_from}/{currency_to} | get exchange rate |
| POST     | /rates | create exchange rate |
| PUT     | /rates | update or create exchange rate |
| GET     | /orders/{id} | get order by id |
| POST     | /orders | create order |

Simple transfer scenario:

**Create account A:**  
It is impossible to create account with non-zero balance. Balance should be updated via orders. Doesn't matter
what balance is in posted entity, resulting account will have zero balance.  
POST 127.0.0.1:8080/accounts
```json
    {"currency": "USD", "balance": 0.0}
```

**Create account B:**  
POST 127.0.0.1:8080/accounts
```json
    {"currency": "RUB", "balance": 0.0}
```
**Create exchange rates USD -> EUR, EUR -> RUB:**  
Reversed rates are not set automatically. So if set USD -> EUR, EUR -> USD won't be defined.  
POST 127.0.0.1:8080/rates
```json
    {"currencyCodeFrom": "EUR", "currencyCodeTo": "USD", "rate": 0.5}
```
```json
    {"currencyCodeFrom": "EUR", "currencyCodeTo": "RUB", "rate": 0.5}
```

**Increase balance of account A:**  
to make further transaction possible  
POST 127.0.0.1/orders
```json
    {
        "receiverAccount": "<A.ID>",
        "operationCurrency": "USD",
        "orderType": "INCOME",
        "amount": 10.0
    }
```

**Perform transaction between A and B:**  
POST 127.0.0.1/orders
```json
    {
        "senderAccount": "<A.ID>",
        "receiverAccount": "<B.ID>",
        "operationCurrency": "EUR",
        "orderType": "TRANSFER",
        "amount": 10.0
    }
```

**Check receiver and sender balances:**  
GET 127.0.0.1/accounts/<A.ID>
```json
    {
        "senderAccount": "<A.ID>",
        "receiverAccount": "<B.ID>",
        "operationCurrency": "EUR",
        "orderType": "INCOME",
        "amount": 5.0
    }
```

GET 127.0.0.1/accounts/<B.ID>
```json
    {
        "senderAccount": "<A.ID>",
        "receiverAccount": "<B.ID>",
        "operationCurrency": "EUR",
        "orderType": "INCOME",
        "amount": 5.0
    }
```


#### How to build and launch
You need JDK8, maven >= 3.2.5, available ports 8080 (http), 9001 (hsqldb) on your machine

Command:
```bash
mvn verify
```
will build project and launch integration tests.
As a result, a fat-jar artifact will be built: "money_transfer_api-1.0-SNAPSHOT.jar"
You can then launch it with command:
```bash
java -jar <path-to-jar-with-dependencies> <path-to-config>  
```
Application can be launched without config, embedded configuration is enough, but config file sample: config.yml contains convenient logging settings.

Limitations, assumptions and things to do:
*  For the sake of brevity DTO and entity classes are not divided. It is possible to make API more comfortable and separated from DB logic, but it is necessary to duplicate entities as DTO classes
*  The same reason why pool of workers is fixed size (size of cores count by default). 
*  There is no way to launch workers and web server in different JVM, but it requires few things to do, possibly will be the nearest change.
*  Currencies are hardcoded, while better solution is to use a separate table with a list of available currencies.
*  Possibly, using UUIDs is better than integer ids for entities while current implementation uses integers


[Dropwizard]: https://www.dropwizard.io/1.3.5/docs/
[Jersey]: https://jersey.github.io/
[Hibernate ORM]: http://hibernate.org/orm/
[Liquibase]: https://www.liquibase.org/
[HSQLDB]: http://hsqldb.org/
[Apache ActiveMQ]: https://activemq.apache.org/
[JUnit]: https://junit.org/