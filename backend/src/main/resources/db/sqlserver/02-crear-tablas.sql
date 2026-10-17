USE [CitasMedicasDemo];
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.usuarios', N'U') IS NULL
BEGIN
 CREATE TABLE dbo.usuarios (
  id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_usuarios PRIMARY KEY,
  username VARCHAR(50) NOT NULL CONSTRAINT uq_usuarios_username UNIQUE,
  password VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL CONSTRAINT uq_usuarios_email UNIQUE,
  rol VARCHAR(20) NOT NULL CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ROLE_USER','ROLE_ADMIN'))
 );
END;
IF OBJECT_ID(N'dbo.citas_medicas', N'U') IS NULL
BEGIN
 CREATE TABLE dbo.citas_medicas (
  id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_citas PRIMARY KEY,
  medico_nombre VARCHAR(100) NOT NULL,
  fecha DATE NOT NULL,
  hora TIME(0) NOT NULL,
  motivo VARCHAR(500) NOT NULL,
  estado VARCHAR(20) NOT NULL CONSTRAINT ck_citas_estado CHECK (estado IN ('PENDIENTE','CONFIRMADA','CANCELADA')),
  usuario_id BIGINT NOT NULL,
  CONSTRAINT fk_cita_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.usuarios(id),
  CONSTRAINT uk_medico_horario UNIQUE (medico_nombre,fecha,hora)
 );
 CREATE INDEX idx_citas_usuario ON dbo.citas_medicas(usuario_id);
END;
COMMIT;
GO
-- DataInitializer inserta los usuarios con BCrypt y dos citas al iniciar el backend.
