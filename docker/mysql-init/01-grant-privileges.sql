-- The app connects as `churchos`, but tenant provisioning needs to run
-- `CREATE DATABASE tenant_<slug>` at runtime, so the user needs a global
-- grant rather than access scoped to churchos_control alone.
GRANT ALL PRIVILEGES ON *.* TO 'churchos'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
