CREATE TABLE people
(
    id        serial       NOT NULL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    embedding float8[]
);
