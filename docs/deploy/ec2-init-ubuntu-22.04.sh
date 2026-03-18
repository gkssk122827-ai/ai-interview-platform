#!/usr/bin/env bash
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "Run this script as root: sudo bash $0"
  exit 1
fi

APP_USER="${SUDO_USER:-ubuntu}"
DOMAIN="${DOMAIN:-api.example.com}"
CLOUDFRONT_DOMAIN="${CLOUDFRONT_DOMAIN:-dxxxxxxxxxxxx.cloudfront.net}"
EMAIL="${EMAIL:-admin@example.com}"
ENABLE_CERTBOT="${ENABLE_CERTBOT:-false}"
NGINX_CONF_PATH="/etc/nginx/sites-available/aimentor.conf"

export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y software-properties-common ca-certificates curl gnupg lsb-release ufw nginx certbot python3-certbot-nginx unzip

# Docker Engine + Compose plugin
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
usermod -aG docker "$APP_USER"

# Java 21 (Temurin)
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
chmod a+r /etc/apt/keyrings/adoptium.gpg

echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb jammy main" | tee /etc/apt/sources.list.d/adoptium.list > /dev/null

apt-get update
apt-get install -y temurin-21-jdk

# Python 3.11
add-apt-repository -y ppa:deadsnakes/ppa
apt-get update
apt-get install -y python3.11 python3.11-venv python3.11-dev python3-pip
update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.11 2

# Docker network for app containers
if ! docker network inspect aimentor-net >/dev/null 2>&1; then
  docker network create aimentor-net
fi

# UFW firewall
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw deny 8080/tcp
ufw deny 8000/tcp
ufw --force enable

# Nginx reverse proxy
cat > "$NGINX_CONF_PATH" <<EOF
server {
    listen 80;
    server_name ${DOMAIN};

    client_max_body_size 25M;

    location = / {
        return 301 https://${CLOUDFRONT_DOMAIN}\$request_uri;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Connection "";
    }

    location /ai/ {
        allow 127.0.0.1;
        allow 10.0.0.0/8;
        allow 172.16.0.0/12;
        allow 192.168.0.0/16;
        deny all;

        proxy_pass http://127.0.0.1:8000/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Connection "";
    }
}
EOF

ln -sf "$NGINX_CONF_PATH" /etc/nginx/sites-enabled/aimentor.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl enable --now nginx
systemctl reload nginx

if [[ "$ENABLE_CERTBOT" == "true" ]]; then
  certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$EMAIL" --redirect
  systemctl enable certbot.timer
  systemctl start certbot.timer
fi

echo
echo "Initial setup completed."
echo "Docker network: aimentor-net"
echo "Nginx config: $NGINX_CONF_PATH"
echo "Domain: $DOMAIN"
echo "CloudFront redirect target: $CLOUDFRONT_DOMAIN"
echo "Re-login is required for docker group changes to apply to user $APP_USER."
