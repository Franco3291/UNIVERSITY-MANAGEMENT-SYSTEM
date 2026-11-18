# Tharaka University Management System (TUMS)

TUMS is a Java 8-compatible desktop application built with Swing, JDBC and MySQL. The application currently provides database-backed sign-in, first-administrator setup, role-aware navigation, student record management, and academic catalogue management for departments, programmes, and courses. Staff, results, finance, and other operational modules are planned follow-on work and are identified in the dashboard as not yet implemented.

## Requirements

- JDK 8 or later
- Apache Maven 3.6 or later
- MySQL 8.0.16 or later

Open the project root in IntelliJ IDEA as a Maven project.

## Set up MySQL

1. Run [`database/schema.sql`](database/schema.sql) in MySQL Workbench or the MySQL command-line client. It creates the `tums` database, authentication, student, department, programme, and course tables. If you ran an earlier version of the script, run the updated script again; its table creation statements are safe to re-run.
2. Create a dedicated MySQL account for the application and grant it access to `tums`. Keep the account password out of source control. For a local-only development database, for example:

   ```sql
   CREATE USER 'tums_app'@'localhost' IDENTIFIED BY 'use-a-unique-local-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON tums.* TO 'tums_app'@'localhost';
   ```

3. Configure the application environment. Replace the example password with the one set for the database account:

   ```powershell
   $env:TUMS_DB_URL = "jdbc:mysql://localhost:3306/tums?serverTimezone=UTC&useSSL=false"
   $env:TUMS_DB_USER = "tums_app"
   $env:TUMS_DB_PASSWORD = "use-a-unique-local-password"
   ```

   `useSSL=false` is suitable only for a local development database. Configure TLS in the JDBC URL for a remote database.

## Create the first administrator

From a terminal in the project root, make sure the application classes and runtime dependency classpath are available:

```powershell
mvn compile dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"
$classpath = "target\classes;" + (Get-Content target\classpath.txt -Raw).Trim()
java -cp $classpath ke.co.tharaka.tums.Main --create-admin
```

The command prompts for a username and password without echoing the password. It can only create an administrator while the users table is empty. Choose a unique password of at least 12 characters. The password is stored as a salted PBKDF2-HMAC-SHA-256 hash, never as plain text.

## Run the application

```powershell
mvn compile exec:java -Dexec.mainClass=ke.co.tharaka.tums.Main
```

Alternatively, build the JAR with `mvn package`. The MySQL Connector/J dependency must be on the runtime classpath when launching the JAR.

Run the unit tests with `mvn test`.

## Implemented student records

The Student records screen provides case-insensitive search by admission number, name, email, programme, and department. It displays up to 500 active records per search and supports registering, updating, and archiving a record. Archiving hides the record from the active list without physically deleting it. Admission numbers are unique, required fields and contact details are validated, and JDBC operations use prepared statements off the Swing event thread.

- **Administrators:** search, register, update, and archive student records.
- **Lecturers:** search and view student records.

Student fields include admission number, full name, gender, phone, email, programme, department, and academic level. Programme, department, and academic level are stored as text until their management modules are implemented.

## Academic catalogue

The Academic catalogue screen manages departments, programmes, and courses as linked records. Search is available within each catalogue tab. Administrators may create, update, and archive entries; lecturers may view the catalogue. New programmes require an active department, and new courses require an active programme. Parent records with active children cannot be archived. Codes are unique, and course credit hours are validated from 1 to 12.

## Role navigation

- **Administrator:** student records, staff and courses, exams and results, fees and finance, and system administration.
- **Lecturer:** student records, staff and courses, and exams and results.
- **Finance officer:** fees and finance.

Navigation is filtered by role, and student and academic catalogue operations enforce their role permissions in the service layer. Exams and results, fees and finance, and system administration are still placeholders until their corresponding workflows are implemented. The dashboard window uses the normal Windows minimize, maximize/restore, and close controls.

## Project layout

- `ui`: Swing login and dashboard
- `service`: authentication and first-administrator setup
- `dao`: JDBC persistence
- `database`: MySQL schema
- `model`: user account, roles, and module access rules
- `service`: authentication, administrator setup, student, and academic catalogue business rules
- `security`: password hashing
- `config`: environment-based database configuration
