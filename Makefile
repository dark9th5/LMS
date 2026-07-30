SHELL := /bin/bash

.PHONY: setup up up-all down logs ps validate smoke test test-backend test-frontend backup restore clean

setup:
	./scripts/setup.sh

up:
	docker compose up -d --build

up-all:
	docker compose --profile extended --profile observability up -d --build

down:
	docker compose --profile extended --profile observability down

logs:
	docker compose logs -f --tail=200

ps:
	docker compose ps

validate:
	python3 scripts/validate-repository.py
	node scripts/check-typescript.js
	docker compose config --quiet

smoke:
	./scripts/smoke-test.sh

test: test-backend test-frontend

test-backend:
	cd backend && ./gradlew test --no-daemon

test-frontend:
	cd apps/web && npm install --no-audit --no-fund --ignore-scripts && npm run typecheck && npm run build

backup:
	./scripts/backup.sh

restore:
	@test -n "$(BACKUP)" || (echo "Usage: make restore BACKUP=backups/<folder>" && exit 1)
	./scripts/restore.sh "$(BACKUP)"

clean:
	docker compose --profile extended --profile observability down -v --remove-orphans
	rm -rf apps/web/.next apps/web/node_modules backend/.gradle backend/**/build
