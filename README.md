# People Search

web service for finding similar people faces in Scala.

### Introduction

Face recognition is one of the main problems of modern computer vision. There are many solutions developed for finding people from the pictures and authenticating them. Different governments and commercial companies use deep learning solutions, which in its core embed visual features of the person from an image into a vector of numbers.

This repository helps to build an API for such machine learning systems by providing database CRUD operations on the people face embeddings and clean endpoints for data manipulation.

Scala has been chosen as it induces pure functional style.

### Stack used

- [Doobie](https://tpolecat.github.io/doobie/) - connection with database
- [PostgreSQL](https://www.postgresql.org/) - database for storing embeddings
- [Http4s](https://http4s.org/) - manage requests and routes
- [Flyway](https://flywaydb.org/) - migrations
- [TypeSafe Config](https://github.com/lightbend/config) - reading config file
- [Circe](https://circe.github.io/circe/) - json encoding and decoding

### How to start

To start the project run following commands:

```bash
# copy repository
git clone https://github.com/QazyBi/People-Search
# enter the repository folder
cd people-search/
# create container with sbt and code
docker build . -t sbt
# start services: web-server and postgresql database
docker-compose up -d  # -d flag refers to detached mode, remove if not needed
```

### How to use

To get list of people in the database use next command:

```bash
curl [http://localhost:8080/people](http://localhost:8080/people)
```

To get information about specific person using he's id use:

```bash
curl http://localhost:8080/people/{id}  # specify id, for instance 1
```

To search top 5 similar people from the database to the person you specify

```bash
curl http://localhost:8080/search/{id}  # specify id of queried person, for instance 1

# one possible result
# [{"id":1203,"name":"Natalie Portman","embedding":[0.31,0.19,0.41,0.76,0.35, 0.3]},
#  {"id":562,"name":"Keira Knightley","embedding":[0.23,0.21,0.33,0.81,0.29, 0.14]},
#  {"id":1203,"name":"Britney Spears","embedding":[0.3,0.1,0.35,0.78,0.95, 0.04]},
#  {"id":1203,"name":"Kate Winslet","embedding":[0.12,0.23,0.45,0.70,0.89, 0.12]},
#  {"id":1203,"name":"Daisy Ridley","embedding":[0.45,0.16,0.24,0.84,0.91, 0.05]}]
```

To add new record of a person use following command:

```bash
curl -X POST --header "Content-Type: application/json" --data '{"name": "Martin Odersky", "embedding": [0.3, 0.1, 0.35, 0.85, 0.99, 0.01]}' http://localhost:8080/people
```

To update information about person in the database run following:

```bash
curl -X PUT --header "Content-Type: application/json" --data '{"name": "Martin Odersky", "embedding": [0.3, 0.1, 0.35, 0.85, 0.99]}' http://localhost:8080/people
```

To delete information about person in the database run following:

```bash
curl -X DELETE --header "Content-Type: application/json" http://localhost:8080/people/{id}  # specify id, for instance 1
```

### Project Structure

**resources:**

- `db/migration/V1__create_people.sql` - migrations that will be run when the project initializes
    
    ```sql
    CREATE TABLE people
    (
        id        serial       NOT NULL PRIMARY KEY,
        name     VARCHAR(100)  NOT NULL,
        embedding float8[]
    );
    ```
    

SQL code creates a table with id, name and embedding fields.

Embedding is a vector of floats representing person in some latent space.

- `application.conf` - configurations of the web server and of the connection to the database

**scala:**

**config** - ****module for reading configuration files(ServerConfig, DatabaseConfig)

**db** - module for database initialization through migrations and producing transactors

**model** - definition of the class mapping fields from PostgreSQL table

```scala
case class Person(
      id: Option[Long],  // corresponds to serial
      name: String,  // corresponds to VARCHAR
      embedding: List[Double]  // corresponds to SQL float8[]
)
case object PersonNotFoundError  // for handling cases when 
																// person not found in the table
```

**repository** -  ****definition of methods for interaction with database.

- getPeople - run sql query for selecting all people from the `people` table
- getPerson - run sql query for selecting person matching with given `Person` id
- createPerson - run sql query for creating a record in the table given `Person` class instance
- deletePerson - run sql query for deleting record in the table given `Person` id
- updatePerson - run sql query for updating record in the table given `Person` class instance

**service** - definition of routes and business logic(in further updates will be separated)

- [CosineSimilarity](https://en.wikipedia.org/wiki/Cosine_similarity) - object which defines useful methods for calculating cosine similarity between two lists of doubles.
    
    > Cosine similarity is a good measure of similarity of two vectors. Actively used when comparing two embeddings in machine learning.
    > 
- PeopleService - defines routes for http requests and data manipulation.

`HttpServer.scala` - initialization of the web server

`ServerApp.scala` - starting point of the project

### What is next?

- Adding image post requests
- Adding stream processing
