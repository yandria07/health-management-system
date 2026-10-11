#!/bin/bash
# ============================================================
# 健康管理系统 - 一键部署脚本
# 适用：Ubuntu 22.04 / 24.04 云服务器
# 用法：sudo bash deploy.sh
# ============================================================

set -e

# ========== 配置区（请根据实际情况修改） ==========
APP_NAME="health-server"
APP_PORT=8080
APP_DIR="/opt/health"
DB_NAME="student"
DB_USER="root"
DB_PASS="Cyybs@0402"            # 改成你的 MySQL 密码
DOMAIN="你的服务器IP或域名"        # 改成你的服务器IP或域名
GIT_REPO="https://github.com/yandria07/health-management-system.git"
# ==============================================================

echo "========================================"
echo "  健康管理系统 - 一键部署"
echo "========================================"

# 1. 安装基础依赖
echo "[1/8] 安装基础依赖..."
apt update -y
apt install -y openjdk-17-jdk mysql-server nginx git curl wget

# 2. 配置 MySQL
echo "[2/8] 配置 MySQL 数据库..."
systemctl start mysql
systemctl enable mysql
mysql -u root <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${DB_PASS}';
CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
FLUSH PRIVILEGES;
EOF

# 3. 创建数据库表
echo "[3/8] 创建数据库表..."
mysql -u root -p'${DB_PASS}' ${DB_NAME} <<'SQL'
CREATE TABLE IF NOT EXISTS `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `password` varchar(100) DEFAULT NULL,
  `gender` int DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `userType` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `health_data` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `record_date` date DEFAULT NULL,
  `weight` decimal(10,2) DEFAULT NULL,
  `height` decimal(10,2) DEFAULT NULL,
  `bmi` decimal(10,2) DEFAULT NULL,
  `systolic_pressure` int DEFAULT NULL,
  `diastolic_pressure` int DEFAULT NULL,
  `heart_rate` int DEFAULT NULL,
  `steps` int DEFAULT NULL,
  `sleep_hours` decimal(10,2) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `health_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `plan_name` varchar(100) DEFAULT NULL,
  `plan_type` varchar(50) DEFAULT NULL,
  `description` text,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `remind_time` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `admin` (`username`, `nickname`, `password`, `userType`) 
VALUES ('admin', '管理员', '123456', 1);
SQL

# 4. 安装 Maven（用于打包后端）
echo "[4/8] 安装 Maven..."
cd /tmp
if [ ! -f "apache-maven-3.9.6-bin.tar.gz" ]; then
  wget -q https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
fi
tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt
ln -sf /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn

# 5. 安装 Node.js（用于构建前端）
echo "[5/8] 安装 Node.js..."
if ! command -v node &> /dev/null; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt install -y nodejs
fi

# 6. 拉取代码并打包后端
echo "[6/8] 打包后端..."
mkdir -p ${APP_DIR}
cd ${APP_DIR}
if [ -d "health-management-system" ]; then
  cd health-management-system && git pull
else
  git clone ${GIT_REPO}
  cd health-management-system
fi

# 修改数据库密码
sed -i "s/password: Cyybs@0402/password: ${DB_PASS}/" backend/src/main/resources/application.yml

cd backend
mvn clean package -DskipTests -q
cp target/health-server-0.0.1-SNAPSHOT.jar ${APP_DIR}/health-server.jar

# 7. 配置 systemd 服务
echo "[7/8] 配置 systemd 服务..."
cat > /etc/systemd/system/health-server.service <<EOF
[Unit]
Description=Health Management Server
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=${APP_DIR}
ExecStart=/usr/bin/java -jar ${APP_DIR}/health-server.jar --server.port=${APP_PORT}
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable health-server
systemctl restart health-server

# 8. 构建前端并配置 Nginx
echo "[8/8] 构建前端并配置 Nginx..."
cd ${APP_DIR}/health-management-system/frontend
npm install --silent
npm run build

mkdir -p /var/www/health
cp -r dist/* /var/www/health/

cat > /etc/nginx/sites-available/health <<EOF
server {
    listen 80;
    server_name ${DOMAIN};

    location / {
        root /var/www/health;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:${APP_PORT}/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }
}
EOF

ln -sf /etc/nginx/sites-available/health /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl restart nginx
systemctl enable nginx

# 开放防火墙
ufw allow 80/tcp 2>/dev/null || true
ufw allow 443/tcp 2>/dev/null || true

echo ""
echo "========================================"
echo "  部署完成！"
echo "========================================"
echo "  系统首页:  http://${DOMAIN}"
echo "  API 文档:  http://${DOMAIN}/api/doc.html"
echo "  默认账号:  admin / 123456"
echo ""
echo "  后端日志:  journalctl -u health-server -f"
echo "  重启后端:  systemctl restart health-server"
echo "========================================"
