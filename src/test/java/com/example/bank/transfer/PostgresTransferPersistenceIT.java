package com.example.bank.transfer;

import org.springframework.test.context.ActiveProfiles;

/** Repeat rollback and stale-balance protection on the production database engine. */
@ActiveProfiles(value = "postgres-test", inheritProfiles = false)
class PostgresTransferPersistenceIT extends TransferPersistenceTest {}
