#!/bin/bash
set -euo pipefail

cd ..

# Sync files to Raspberry Pi 5, deleting remote files not present locally
rsync -av --delete --exclude 'web/node_modules' --exclude '.venv' --exclude 'backend/app/static' ./ rpi:/mnt/ssd/projects/live-location/

# SSH into Raspberry Pi 5, navigate to project folder, stop and rebuild containers
ssh rpi << 'EOF'
  cd /mnt/ssd/projects/live-location/infra/
  docker-compose down
  docker-compose build --no-cache
  docker-compose up -d
#  # Extract static files from running backend container
#  rm -rf ./static
#  docker cp live-location_backend_1:/app/app/static ./static
#
#  # Sync built frontend static files to EC2 backend
#  rsync -av --delete static/* ec2:~/price-monitor/backend/app/static/


  docker image prune -a -f
  docker container prune -f
EOF