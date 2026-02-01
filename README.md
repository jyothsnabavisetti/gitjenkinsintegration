# SeqDiag

Simple Spring Boot app that accepts a ZIP of a Java project and generates a sequence diagram (PlantUML + PNG).

## Run

mvn spring-boot:run

## API

POST /api/upload (multipart/form-data file=project.zip)
Returns JSON `{ plantuml: string, pngBase64: string }`

## Frontend UI

A minimal React + Vite frontend is included under `frontend/`.

Run it locally:

```bash
cd frontend
npm install
npm run dev
```

The dev server proxies `/api` to the backend at `http://localhost:8080`.

Build and run as a single bundled app:

```bash
# build backend + frontend and package into a single jar
mvn -DskipTests package

# run the fat jar
java -jar target/seqdiag-0.0.1-SNAPSHOT.jar
```

The Spring Boot app serves the frontend UI from `/` (static files copied into the JAR during the build).

Alternatively, build the frontend alone:

```bash
cd frontend
npm run build
```

Serve the `dist/` folder with any static server or integrate it into the backend's static resources.

## Notes

This is an MVP. It performs a best-effort static analysis using JavaParser and may not resolve dynamic dispatch or reflection calls.