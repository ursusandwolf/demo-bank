-- Local development only. Register both users through the API first.
INSERT INTO accounts (id, account_number, owner_id, balance, currency, status, created_at, version)
SELECT gen_random_uuid(), 'ALICE-USD', id, 1000.00, 'USD', 'ACTIVE', CURRENT_TIMESTAMP, 0
FROM users WHERE email = 'alice@example.com'
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO accounts (id, account_number, owner_id, balance, currency, status, created_at, version)
SELECT gen_random_uuid(), 'BOB-USD', id, 500.00, 'USD', 'ACTIVE', CURRENT_TIMESTAMP, 0
FROM users WHERE email = 'bob@example.com'
ON CONFLICT (account_number) DO NOTHING;
