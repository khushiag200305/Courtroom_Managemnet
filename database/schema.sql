-- =====================================================================
--  Courtroom Management System - MySQL schema
--  Run:  mysql -u root -p < database/schema.sql
--  Then: mysql -u root -p courtroom_db < database/seed.sql
-- =====================================================================

DROP DATABASE IF EXISTS courtroom_db;
CREATE DATABASE courtroom_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE courtroom_db;

-- ---------------------------------------------------------------------
-- Courtrooms (physical rooms where hearings happen)
-- ---------------------------------------------------------------------
CREATE TABLE courtrooms (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    room_no     VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------------
-- Users: one table for all 3 roles (Registrar, Judge, Lawyer)
-- Role-specific details live in the judges / lawyers tables.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           ENUM('REGISTRAR','JUDGE','LAWYER') NOT NULL,
    email          VARCHAR(100),
    phone          VARCHAR(15),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE judges (
    user_id        INT PRIMARY KEY,
    designation    VARCHAR(100) NOT NULL,
    courtroom_id   INT UNIQUE,
    FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
    FOREIGN KEY (courtroom_id) REFERENCES courtrooms(id) ON DELETE SET NULL
);

CREATE TABLE lawyers (
    user_id        INT PRIMARY KEY,
    bar_id         VARCHAR(30) NOT NULL UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Cases
-- ---------------------------------------------------------------------
CREATE TABLE cases (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    case_number        VARCHAR(30) UNIQUE,              -- assigned on registration, e.g. CS/101/2026
    title              VARCHAR(200) NOT NULL,           -- e.g. "Sharma vs. Verma"
    case_type          ENUM('CIVIL','CRIMINAL','FAMILY','PROPERTY','CONSUMER','OTHER') NOT NULL,
    description        TEXT,
    status             ENUM('FILED','DEFECTIVE','REGISTERED','PENDING','RESERVED','DISPOSED') NOT NULL DEFAULT 'FILED',
    urgent             BOOLEAN NOT NULL DEFAULT FALSE,
    filed_by           INT NOT NULL,                    -- lawyer who filed
    judge_id           INT,
    courtroom_id       INT,
    filing_date        DATE NOT NULL,
    registration_date  DATE,
    next_hearing_date  DATE,
    disposal_date      DATE,
    defect_reason      VARCHAR(500),
    judgment           TEXT,
    FOREIGN KEY (filed_by)     REFERENCES users(id),
    FOREIGN KEY (judge_id)     REFERENCES users(id),
    FOREIGN KEY (courtroom_id) REFERENCES courtrooms(id)
);

CREATE TABLE parties (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    case_id     INT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    party_type  ENUM('PETITIONER','RESPONDENT') NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(15),
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Hearings: the UNIQUE keys stop double-booking at database level too
-- ---------------------------------------------------------------------
CREATE TABLE hearings (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    case_id       INT NOT NULL,
    judge_id      INT NOT NULL,
    courtroom_id  INT NOT NULL,
    hearing_date  DATE NOT NULL,
    slot_time     TIME NOT NULL,
    status        ENUM('SCHEDULED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    FOREIGN KEY (case_id)      REFERENCES cases(id) ON DELETE CASCADE,
    FOREIGN KEY (judge_id)     REFERENCES users(id),
    FOREIGN KEY (courtroom_id) REFERENCES courtrooms(id)
);

-- ---------------------------------------------------------------------
-- Orders passed by the judge at each hearing
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    case_id     INT NOT NULL,
    hearing_id  INT,
    judge_id    INT NOT NULL,
    order_text  TEXT NOT NULL,
    outcome     ENUM('ADJOURNED','NEXT_HEARING','RESERVED','DISPOSED') NOT NULL,
    next_date   DATE,
    order_date  DATE NOT NULL,
    FOREIGN KEY (case_id)    REFERENCES cases(id) ON DELETE CASCADE,
    FOREIGN KEY (hearing_id) REFERENCES hearings(id) ON DELETE SET NULL,
    FOREIGN KEY (judge_id)   REFERENCES users(id)
);

-- ---------------------------------------------------------------------
-- Uploaded documents (files stored on disk, path stored here)
-- ---------------------------------------------------------------------
CREATE TABLE documents (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    case_id      INT NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    stored_path  VARCHAR(500) NOT NULL,
    file_size    BIGINT NOT NULL,
    uploaded_by  INT NOT NULL,
    uploaded_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id)     REFERENCES cases(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE TABLE holidays (
    holiday_date  DATE PRIMARY KEY,
    description   VARCHAR(100) NOT NULL
);

CREATE TABLE notifications (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    message     VARCHAR(500) NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE audit_log (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    action      VARCHAR(50)  NOT NULL,
    details     VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_cases_status       ON cases(status);
CREATE INDEX idx_cases_next_hearing ON cases(next_hearing_date);
CREATE INDEX idx_hearings_date      ON hearings(hearing_date);
