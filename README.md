# ⚖ Courtroom Management System

A desktop application built with **Java 21, JavaFX and MySQL (JDBC)** that digitises the everyday work of a district court: filing cases, checking them, scheduling hearings, publishing the daily cause list and recording orders and judgments.

Built as the mini-project for **UCS553 – Enterprise Web Application**.

## Users

| Role | What they do |
|---|---|
| **Registrar** | Manages judges, lawyers and courtrooms, checks new filings, schedules hearings, publishes the cause list |
| **Judge** | Sees today's cause list, records orders and next dates, delivers judgments |
| **Lawyer** | Files cases with documents, tracks hearing dates, reads orders |

## Tech stack

| Part | Technology |
|---|---|
| Language | Java 21 (JDK) |
| User interface | JavaFX 21 (OpenJFX) with FXML and CSS |
| Database | MySQL 8 |
| Database access | JDBC (MySQL Connector/J) with the DAO pattern |
| Build tool | Maven |
| Logging | java.util.logging |

## Project structure

```
src/main/java/com/court/
 ├── Main.java          JavaFX Application (init / start / stop lifecycle)
 ├── Launcher.java      plain main class for running from an IDE
 ├── model/             User (abstract), Registrar, Judge, Lawyer, Courtroom, Role ...
 ├── dao/               GenericDao<T, ID> and DAO interfaces
 │   └── impl/          JDBC implementations
 ├── service/           business logic (AuthService ...)
 ├── controller/        JavaFX controllers for each screen
 ├── exception/         custom exceptions
 └── util/              DBConnection, AppConfig, PasswordUtil, SceneManager ...
src/main/resources/
 ├── view/              FXML screens
 ├── css/style.css      application theme
 ├── db.properties      database settings
 ├── app.properties     court name, hearing slots, folders
 └── logging.properties log configuration
database/
 ├── schema.sql         creates the database and tables
 └── seed.sql           sample users, courtrooms and holidays
```

## How to run

**Requirements:** JDK 21, Maven 3.9+, MySQL 8.

1. **Create the database**
   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p courtroom_db < database/seed.sql
   ```
2. **Set your MySQL password.** Edit `src/main/resources/db.properties`, or, to keep it out of Git, create a file called `db.local.properties` in the project folder:
   ```properties
   db.user=root
   db.password=your_password
   ```
3. **Run the app**
   ```bash
   mvn clean javafx:run
   ```
   In IntelliJ you can also open the project as a Maven project and run `com.court.Launcher`.

## Sample logins

| Role | Username | Password |
|---|---|---|
| Registrar | `registrar` | `admin123` |
| Judge | `judge1`, `judge2`, `judge3` | `judge123` |
| Lawyer | `lawyer1` … `lawyer4` | `lawyer123` |

## Logs

The app writes its activity log to `logs/court.log` (logins, errors and important actions).
