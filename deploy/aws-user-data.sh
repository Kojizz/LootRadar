#!/bin/bash
# EC2 Ubuntu 24.04 user data. No credentials belong in this file.
set -euo pipefail
exec > >(tee -a /var/log/lootradar-deploy.log /dev/console) 2>&1
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl git openssl
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
cat > /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: noble
Components: stable
Architectures: amd64
Signed-By: /etc/apt/keyrings/docker.asc
EOF
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
if [ ! -f /swapfile ]; then
  fallocate -l 4G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
git clone https://github.com/Kojizz/LootRadar.git /opt/lootradar
cd /opt/lootradar
git checkout ddc4c73762ea137ca16a6c62569509f08d18ca1b
TOKEN=$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' http://169.254.169.254/latest/api/token)
PUBLIC_IP=$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4)
SITE_HOST="lootradar.${PUBLIC_IP//./-}.sslip.io"
umask 077
printf 'POSTGRES_PASSWORD=%s\nAPP_BASE_URL=https://%s\nSESSION_COOKIE_SECURE=true\nSITE_HOST=%s\n' "$(openssl rand -hex 32)" "$SITE_HOST" "$SITE_HOST" > .env
umask 022
cat > compose.aws.yaml <<'YAML'
services:
  db:
    restart: unless-stopped
    mem_limit: 384m
    logging:
      driver: json-file
      options: {max-size: 10m, max-file: '3'}
  app:
    mem_limit: 1024m
    environment:
      SERVER_FORWARD_HEADERS_STRATEGY: framework
    logging:
      driver: json-file
      options: {max-size: 10m, max-file: '3'}
  proxy:
    image: caddy:2-alpine
    restart: unless-stopped
    mem_limit: 128m
    ports:
      - '80:80'
      - '443:443'
    environment:
      SITE_HOST: ${SITE_HOST}
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy-data:/data
      - caddy-config:/config
    depends_on:
      - app
    logging:
      driver: json-file
      options: {max-size: 10m, max-file: '3'}
volumes:
  caddy-data:
  caddy-config:
YAML
cat > Caddyfile <<'CADDY'
{$SITE_HOST} {
  encode zstd gzip
  reverse_proxy app:8080
}
CADDY
docker compose -f compose.yaml -f compose.aws.yaml up --build -d
printf 'LootRadar containers started. Verify HTTPS health at https://%s/actuator/health\n' "$SITE_HOST"
