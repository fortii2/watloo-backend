# Watloo Backend

Watloo Backend is a Spring Boot service for a Waterloo-focused Telegram assistant. It combines course schedule management, restaurant reviews, utility commands, and AI-assisted command correction in a single bot, with REST endpoints for schedule clients and a small browser-based demo console.

## Frontend

The corresponding frontend project is available at [fortii2/watloo-frontend](https://github.com/fortii2/watloo-frontend).

## Features

- Register recurring courses for a University of Waterloo term through Telegram.
- Retrieve a user's daily or weekly course schedule through a REST API.
- Submit, rank, and search restaurant reviews and dish recommendations.
- Pick a random item from a list with a bot command.
- Correct mistyped bot commands with OpenAI before routing them.
- Synchronize term data from the University of Waterloo Open Data API every day.
- Persist users, schedules, terms, restaurants, and reviews in SQLite.
- Inspect service status, known commands, command correction, and upcoming terms from a browser.

## Tech Stack

- Java 17
- Spring Boot 3.4
- Spring Data JPA and SQLite
- Spring Cloud OpenFeign
- TelegramBots long polling
- OpenAI Chat Completions API
- Maven
- Docker and Docker Compose

## Prerequisites

For local development, install:

- JDK 17 or later
- Maven 3.8 or later
- A Telegram bot token from [BotFather](https://t.me/BotFather)
- A [University of Waterloo Open Data API key](https://openapi.data.uwaterloo.ca/api-docs/index.html)
- An [OpenAI API key](https://platform.openai.com/api-keys)

Docker can be used instead of installing Java and Maven locally.

## Configuration

The application uses the following environment variables:

| Variable | Purpose |
| --- | --- |
| `TELEGRAM_BOT_TOKEN` | Authenticates the Telegram bot. |
| `WATERLOO_OPEN_KEY` | Authenticates requests to Waterloo Open Data. |
| `OPENAI_API_KEY` | Enables AI-assisted command correction. |

Export the variables in your shell:

```bash
export TELEGRAM_BOT_TOKEN="your-telegram-bot-token"
export WATERLOO_OPEN_KEY="your-waterloo-open-data-key"
export OPENAI_API_KEY="your-openai-api-key"
```

The `dev` and `local` Spring profiles import a gitignored file at `src/main/resources/secrets.yml`. Create it with environment-variable references so that credentials remain outside source control:

```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
waterloo:
  open:
    key: ${WATERLOO_OPEN_KEY}
openai:
  api:
    key: ${OPENAI_API_KEY}
```

Do not commit API keys or bot tokens. The `secrets.yml` path is already ignored by Git.

## Run Locally

Clone the repository and create the SQLite data directory:

```bash
git clone https://github.com/fortii2/watloo-backend.git
cd watloo-backend
mkdir -p data
```

Configure the environment variables and `secrets.yml` described above, then start the application:

```bash
mvn spring-boot:run
```

The default `dev` profile starts the HTTP server and Telegram long polling. The SQLite database is created at `data/watloo.db`.

To run the HTTP API and demo console without Telegram polling, use the `local` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Once the server is running, open [http://localhost:8080](http://localhost:8080) to use the demo console.

## Run with Docker

Export the three required credentials, create the data directory, and start the published image:

```bash
mkdir -p data
docker compose up -d
```

Docker Compose uses `ghcr.io/fortii2/waterloo-backend:latest` by default and stores SQLite data in the local `data/` directory.

To build and run the current source instead:

```bash
docker build -t watloo-backend:local .
export WATLOO_IMAGE="watloo-backend:local"
docker compose up -d
```

Useful container commands:

```bash
docker compose logs -f watloo-bot
docker compose down
```

## Telegram Commands

| Command | Description |
| --- | --- |
| `/start` | Display the welcome message. |
| `/help` | Show command help. |
| `/pick <options...>` | Randomly select one whitespace-separated option. |
| `/add_course` | Start the guided course registration flow. |
| `/restaurant_reviews` | Submit a review or browse restaurant rankings and search results. |

Commands are checked before routing. Recognizable typos such as `/halp` can be corrected by OpenAI; if correction fails, the original input is routed unchanged.

## HTTP API

All examples assume the service is available at `http://localhost:8080`.

### Demo endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/demo/status` | Show service status and synchronized term information. |
| `GET` | `/api/demo/commands` | List known Telegram commands. |
| `GET` | `/api/demo/check?input=/halp` | Run a command through AI correction. |
| `GET` | `/api/demo/terms` | Return up to four upcoming terms. |

Example:

```bash
curl http://localhost:8080/api/demo/status
```

### Course schedule

`GET /api/courses` returns courses for a Telegram user. The user must already exist in the database, which normally happens after they send a message to the bot.

| Input | Required | Description |
| --- | --- | --- |
| `X-Telegram-User-Id` header | Yes | Telegram user ID associated with the schedule. |
| `view` query parameter | No | `week` (default) or `day`. |
| `date` query parameter | No | Anchor date in `YYYY-MM-DD` format; defaults to today. |

```bash
curl \
  -H "X-Telegram-User-Id: 123456789" \
  "http://localhost:8080/api/courses?view=week&date=2026-09-14"
```

Successful responses use the `America/Toronto` timezone and have this shape:

```json
{
  "view": "week",
  "timezone": "America/Toronto",
  "courses": [
    {
      "id": "c_1",
      "name": "ECE 650",
      "location": "E7 3343",
      "professor": "Example Professor",
      "dayOfWeek": 1,
      "date": "2026-09-14",
      "beginTime": "10:00",
      "endTime": "11:20"
    }
  ]
}
```

The endpoint returns `401` when the user header is missing or unknown, `400` for an invalid view or date, and `500` for an unexpected server error. The header identifies an existing Telegram user; it is not a cryptographic authentication mechanism.

## Data and Background Jobs

- Local profiles store data in `./data/watloo.db`.
- The production container stores data in `/app/data/watloo.db`, backed by the Compose volume mapping.
- Waterloo term data is synchronized on application startup and then every 24 hours.
- Course registration creates one schedule entry per week for the selected term.

## Testing

After configuring the required credentials, run the test suite with:

```bash
mvn test
```

Build the executable JAR with:

```bash
mvn clean package
```

## Deployment

Pushes to `main` trigger the GitHub Actions workflow in `.github/workflows/deploy.yml`. The workflow builds a Docker image, publishes it to GitHub Container Registry, and updates the configured EC2 deployment with Docker Compose.

The workflow reads the repository's `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `TELEGRAM_BOT_TOKEN`, and `WATERLOO_OPEN_KEY` GitHub Actions secrets. The production application also requires `OPENAI_API_KEY`; add that secret and pass it into the generated `.env` file before using this workflow for deployment. The current workflow does not write it automatically.

## Contributing

This repository follows a fork-and-pull-request workflow. Do not push directly to `main`.

1. Fork the repository and synchronize your fork with `main`.
2. Create a focused branch such as `feature-read-and-reply` or `fix-correct-reply-format`.
3. Implement and test the change locally.
4. Push the branch and open a pull request.
5. Wait for at least one team member to review and merge it.
