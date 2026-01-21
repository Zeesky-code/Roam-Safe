#!/bin/bash
export DB_URL="jdbc:postgresql://ep-jolly-field-ahzb7l97-pooler.c-3.us-east-1.aws.neon.tech/neondb?sslmode=require"
export DB_USER="neondb_owner"
export DB_PASS="npg_Th3aWgPvMiH0"

echo "🌍 Connecting to Neon DB..."
./mvnw spring-boot:run
