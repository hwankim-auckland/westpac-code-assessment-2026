CREATE TABLE accounts
(
    id               VARCHAR(64)  NOT NULL,
    customer_id      UUID         NOT NULL,
    account_number   VARCHAR(20)  NOT NULL,
    customer_name    VARCHAR(120) NOT NULL,
    account_nickname VARCHAR(50),
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number)
);
