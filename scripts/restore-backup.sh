#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  qapilot-server/scripts/restore-backup.sh <manifest.yaml> [--yes]

Example:
  qapilot-server/scripts/restore-backup.sh backup/manifests/260615-backup.yaml --yes

Notes:
  - This operation overwrites the target PostgreSQL database.
  - This operation removes and re-copies the configured MinIO prefixes.
EOF
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
  usage
  exit 1
fi

MANIFEST_PATH="$1"
AUTO_YES="${2:-}"

if [[ ! -f "$MANIFEST_PATH" ]]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

if [[ "$AUTO_YES" != "" && "$AUTO_YES" != "--yes" ]]; then
  usage
  exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MANIFEST_ABS="$REPO_ROOT/$MANIFEST_PATH"

yaml_get() {
  local expr="$1"
  ruby -r yaml -e '
    data = YAML.load_file(ARGV[0])
    value = eval(ARGV[1], binding, __FILE__, __LINE__)
    case value
    when nil
      exit 2
    when Array
      value.each { |item| puts item }
    else
      puts value
    end
  ' "$MANIFEST_ABS" "$expr"
}

RESTORE_MAPPINGS_RAW="$(
  ruby -r yaml -e '
    data = YAML.load_file(ARGV[0])
    (data.dig("minio", "restore_prefixes") || []).each do |entry|
      puts "#{entry.fetch("source")}|#{entry.fetch("target")}"
    end
  ' "$MANIFEST_ABS"
)"

NAME="$(yaml_get 'data["name"]')"
COMPOSE_FILE_REL="$(yaml_get 'data.dig("docker", "compose_file")')"
PG_CONTAINER="$(yaml_get 'data.dig("postgres", "container")')"
PG_USER="$(yaml_get 'data.dig("postgres", "user")')"
PG_MAINT_DB="$(yaml_get 'data.dig("postgres", "maintenance_db")')"
PG_DB="$(yaml_get 'data.dig("postgres", "database")')"
PG_SQL_REL="$(yaml_get 'data.dig("postgres", "sql_file")')"
MINIO_ENDPOINT="$(yaml_get 'data.dig("minio", "endpoint")')"
MINIO_ACCESS_KEY="$(yaml_get 'data.dig("minio", "access_key")')"
MINIO_SECRET_KEY="$(yaml_get 'data.dig("minio", "secret_key")')"
MINIO_BUCKET="$(yaml_get 'data.dig("minio", "bucket")')"
MINIO_BACKUP_ROOT_REL="$(yaml_get 'data.dig("minio", "backup_root")')"

COMPOSE_FILE="$REPO_ROOT/$COMPOSE_FILE_REL"
PG_SQL_FILE="$REPO_ROOT/$PG_SQL_REL"
MINIO_BACKUP_ROOT="$REPO_ROOT/$MINIO_BACKUP_ROOT_REL"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found: $COMPOSE_FILE_REL" >&2
  exit 1
fi

if [[ ! -f "$PG_SQL_FILE" ]]; then
  echo "SQL file not found: $PG_SQL_REL" >&2
  exit 1
fi

if [[ ! -d "$MINIO_BACKUP_ROOT" ]]; then
  echo "MinIO backup root not found: $MINIO_BACKUP_ROOT_REL" >&2
  exit 1
fi

if [[ -z "$RESTORE_MAPPINGS_RAW" ]]; then
  echo "No minio.restore_prefixes entries found in $MANIFEST_PATH" >&2
  exit 1
fi

echo "Restore manifest: $NAME"
echo "  PostgreSQL: $PG_SQL_REL -> $PG_DB"
echo "  MinIO bucket: $MINIO_BUCKET"
while IFS= read -r mapping; do
  [[ -z "$mapping" ]] && continue
  source_path="${mapping%%|*}"
  target_path="${mapping##*|}"
  echo "  MinIO prefix: $source_path -> $target_path"
done <<EOF
$RESTORE_MAPPINGS_RAW
EOF

if [[ "$AUTO_YES" != "--yes" ]]; then
  read -r -p "This will overwrite DB and MinIO data. Continue? [y/N] " reply
  if [[ ! "$reply" =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
  fi
fi

echo
echo "[1/4] Starting local infra"
docker compose -f "$COMPOSE_FILE" up -d postgres minio minio-bucket-init

echo
echo "[2/4] Restoring PostgreSQL"
docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_MAINT_DB" -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$PG_DB' AND pid <> pg_backend_pid();"
docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_MAINT_DB" -c "DROP DATABASE IF EXISTS $PG_DB;"
docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_MAINT_DB" -c "CREATE DATABASE $PG_DB OWNER $PG_USER;"
docker exec -i "$PG_CONTAINER" sh -c "psql -U '$PG_USER' -d '$PG_DB'" < "$PG_SQL_FILE"

echo
echo "[3/4] Restoring MinIO objects"
while IFS= read -r mapping; do
  [[ -z "$mapping" ]] && continue
  source_path="${mapping%%|*}"
  target_path="${mapping##*|}"
  if [[ ! -d "$MINIO_BACKUP_ROOT/$source_path" ]]; then
    echo "MinIO source directory not found: $MINIO_BACKUP_ROOT_REL/$source_path" >&2
    exit 1
  fi

  docker run --rm \
    -v "$MINIO_BACKUP_ROOT:/backup" \
    --network host \
    --entrypoint /bin/sh \
    minio/mc -c \
    "mc alias set local '$MINIO_ENDPOINT' '$MINIO_ACCESS_KEY' '$MINIO_SECRET_KEY' && \
     mc rm --recursive --force 'local/$MINIO_BUCKET/$target_path' && \
     mc cp --recursive '/backup/$source_path' 'local/$MINIO_BUCKET/$target_path'"
done <<EOF
$RESTORE_MAPPINGS_RAW
EOF

echo
echo "[4/4] Verifying restore"
docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -c "select count(*) as services_count from services;"
docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -c "select count(*) as domain_documents_count from domain_documents;"

echo
echo "Restore complete."
