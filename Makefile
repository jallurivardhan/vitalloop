# ============================================================
# VitalLoop — developer convenience targets
# Requires: Docker Compose v2+, Maven wrapper (or `mvn` on PATH)
# ============================================================

.PHONY: dev-up dev-down dev-logs build format clean help

## Start all infrastructure containers in the background
dev-up:
	docker compose up -d

## Stop and remove all infrastructure containers (keeps volumes)
dev-down:
	docker compose down

## Tail logs from all running infrastructure containers
dev-logs:
	docker compose logs -f

## Build the entire monorepo (compile + test + Spotless check)
build:
	mvn -B verify

## Apply Spotless code formatting across all modules
format:
	mvn spotless:apply

## Remove all Maven build artefacts
clean:
	mvn -B clean

## Print available targets
help:
	@grep -E '^## ' $(MAKEFILE_LIST) | sed 's/^## /  /'
