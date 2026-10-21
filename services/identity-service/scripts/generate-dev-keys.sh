#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p .local/keys
if [ -e .local/keys/private.pem ] || [ -e .local/keys/public.pem ]; then
    echo "Keys already exist; refusing to overwrite them." >&2
    exit 1
fi
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out .local/keys/private.pem
openssl pkey -in .local/keys/private.pem -pubout -out .local/keys/public.pem
chmod 644 .local/keys/public.pem
printf 'Development keys created. For Compose set APP_UID=%s and APP_GID=%s in .env.\n' "$(id -u)" "$(id -g)"
