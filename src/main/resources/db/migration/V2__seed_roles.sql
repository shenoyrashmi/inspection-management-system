INSERT INTO roles (name)
VALUES
    ('ADMIN'),
    ('MANAGER'),
    ('INSPECTOR')
ON CONFLICT (name) DO NOTHING;
