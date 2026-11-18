CREATE DATABASE IF NOT EXISTS tums
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE tums;

CREATE TABLE IF NOT EXISTS app_setup_lock (
    id TINYINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT IGNORE INTO app_setup_lock (id) VALUES (1);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARBINARY(32) NOT NULL,
    password_salt VARBINARY(16) NOT NULL,
    password_iterations INT UNSIGNED NOT NULL,
    role VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY ux_users_username (username),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMINISTRATOR', 'LECTURER', 'FINANCE_OFFICER'))
);

CREATE TABLE IF NOT EXISTS students (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    admission_number VARCHAR(32) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    gender VARCHAR(24) NOT NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(254) NULL,
    programme VARCHAR(120) NOT NULL,
    department VARCHAR(120) NOT NULL,
    academic_level VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY ux_students_admission_number (admission_number),
    KEY ix_students_name (full_name),
    KEY ix_students_department (department),
    CONSTRAINT chk_students_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'))
);

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(24) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY ux_departments_code (code),
    UNIQUE KEY ux_departments_name (name)
);

CREATE TABLE IF NOT EXISTS programmes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(24) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY ux_programmes_code (code),
    KEY ix_programmes_department (department_id),
    CONSTRAINT fk_programmes_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    programme_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(24) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    credit_hours TINYINT UNSIGNED NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY ux_courses_code (code),
    KEY ix_courses_programme (programme_id),
    CONSTRAINT fk_courses_programme
        FOREIGN KEY (programme_id) REFERENCES programmes (id),
    CONSTRAINT chk_courses_credit_hours CHECK (credit_hours BETWEEN 1 AND 12)
);

CREATE TABLE IF NOT EXISTS university_staff (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    staff_number VARCHAR(32) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    qualification VARCHAR(150) NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(254) NULL,
    responsibility VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY ux_university_staff_number (staff_number),
    KEY ix_university_staff_name (full_name),
    KEY ix_university_staff_department (department_id),
    CONSTRAINT fk_university_staff_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
);
