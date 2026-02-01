# Seq Diagram Generator

[![CI](https://github.com/jyothsnabavisetti/gitjenkinsintegration/actions/workflows/ci.yml/badge.svg)](https://github.com/jyothsnabavisetti/gitjenkinsintegration/actions/workflows/ci.yml)

Simple Spring Boot app that accepts a ZIP of a Java project and generates a sequence diagram (PlantUML + PNG).

## Run

mvn spring-boot:run

### Quick commands

- Run backend (dev):

```bash
mvn spring-boot:run
```

- Build backend + frontend and run (requires Node on PATH):

```bash
# build and package (frontend included)
mvn -DskipTests -Dwith.frontend=true package
# run the produced jar
java -jar target/seq-diagram-generator-0.0.1-SNAPSHOT.jar
```

- Build with automatic Node download (if you don't have Node locally):

```bash
mvn -DskipTests -Dwith.frontend=true -Dinstall.node=true package
```

- Frontend dev server (separate terminal):

```bash
cd frontend
npm install
npm run dev
```

- Build frontend only:

```bash
cd frontend
npm install
npm run build
```

- Quick API test (uploads ZIP and saves PNG):

Included sample project: `examples/sample-project` (zip it and upload):

```bash
# create a zip from the included sample
cd examples/sample-project
zip -r ../../examples/sample-project.zip .

# upload and save PNG
curl -s -X POST -F "file=@examples/sample-project.zip" http://localhost:8080/api/upload \
  | jq -r '.pngBase64' | base64 --decode > diagram.png

# Or upload any project zip:
# curl -s -X POST -F "file=@/path/to/project.zip" http://localhost:8080/api/upload \
#   | jq -r '.pngBase64' | base64 --decode > diagram.png
```

- Run tests:

```bash
mvn test
# run a single test
mvn -Dtest=UploadControllerTest test
```

- Lint / static analysis:

```bash
mvn -DskipTests com.github.spotbugs:spotbugs-maven-plugin:4.7.3.0:check
mvn -DskipTests checkstyle:check
```

- Generate coverage report (JaCoCo):

```bash
mvn test jacoco:report
# report is at target/site/jacoco/index.html
```

- Docker (build & run):

```bash
mvn -DskipTests package
docker build -t seq-diagram-generator:local .
docker run -p 8080:8080 seq-diagram-generator:local
```

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
# build backend + frontend and package into a single jar (requires Node available on PATH)
mvn -DskipTests package

# run the fat jar
java -jar target/seq-diagram-generator-0.0.1-SNAPSHOT.jar
```

The Spring Boot app serves the frontend UI from `/` (static files copied into the JAR during the build).

CI runs unit + integration tests on every push/PR. Pushing a git tag matching `v*` triggers the workflow to build and publish a Docker image to GitHub Container Registry (`ghcr.io/<owner>/seq-diagram-generator`).

If you do NOT have Node installed locally, you can ask Maven to download and install Node during the build by adding `-Dinstall.node=true`. The build will try several recent Node LTS/patch versions automatically and use the first that downloads successfully:

```bash
mvn -DskipTests -Dinstall.node=true package
```

If the automatic download cannot find a valid Node binary in your network environment, either install Node locally and run `mvn -DskipTests package`, or build the frontend separately (`cd frontend && npm run build`).

Alternatively, build the frontend alone:

```bash
cd frontend
npm run build
```

Serve the `dist/` folder with any static server or integrate it into the backend's static resources.

## Notes

This is an MVP. It performs a best-effort static analysis using JavaParser and may not resolve dynamic dispatch or reflection calls.