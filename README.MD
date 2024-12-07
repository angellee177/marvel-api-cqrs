# Marvel API Project

## Description

This project is a simple Marvel API application that allows users to fetch and store Marvel characters data. It includes features for querying Marvel characters, caching results, and storing data in a database.

## System Design

Please check [here](./docs/system-design.md) to see more details about the APIs system design.

## Project Setup

### step 1: Install dependencies
Before you can start the project, ensure all dependencies are installed:
```
./gradlew build
```

This command will download and set up all necessary dependencies for the project.

### step 2: setup applicaiton.conf
Create a `application.conf` file in the [resources directory](./src/main/resources/).
Check the [application.conf.example](./src/main/resources/application.conf.example) file here.


### Step 3: Running Migrations

The project uses Exposed for database migrations. To create the necessary tables, run the following command:
```
flyway -locations=filesystem:src/main/resources/db/migration -url=jdbc:postgresql://localhost:5433/your_db_name -user=your_user_name -password=your_db_password migrate
```
This will apply all pending migrations to your database, you can check the migration file at [here](./src/main/resources/db/migration)

## Running the Project

The following instructions cover the setup for local development:

### Local Development Setup
   To run the project locally, use the following commands:

#### Development Mode
```
./gradlew run
```

This command will start the application in development mode.

## Documentation
### Postman
**Steps to Import Postman Collection**:
1. Open Postman.
2. Click on "Import" in the top-left corner.
3. Select "File" and upload the [Marvel-api.postman_collection.json](Marvel-api.postman_collection.json) file from this repository to run the REST request.
4. After importing, you can start making API requests using the defined endpoints.

## Additional Notes
- **Endpoint:** this project are running REST and gRPC together, you can also make request to gRPC.
- **Environment Variables:** The [application.conf](./src/main/resources/application.conf.example) file is critical for configuring the application, especially for connecting to the PostgreSQL database and generating JWT tokens.
- **Database:** This project uses PostgreSQL for data storage. Make sure your database is set up correctly before running the migrations.
- **Testing:** Unit tests and integration tests are written using Kotlin and JUnit. Test coverage reports are available in `build/reports/jacoco/test/html/index.html`.