-- Ejecutar en SSMS con una cuenta que tenga permiso CREATE DATABASE.
-- No elimina ni modifica bases existentes.
USE master;
GO
IF DB_ID(N'CitasMedicasDemo') IS NULL
    EXEC(N'CREATE DATABASE [CitasMedicasDemo]');
GO
