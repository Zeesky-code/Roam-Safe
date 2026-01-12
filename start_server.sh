#!/bin/bash
export GOOGLE_SHEETS_URL='http://localhost:8080/test-webhook'

# Kill any existing process on 8080 or the previous java jar
pkill -f "java -jar target/roamSafe-1.0.0.jar" || true
lsof -ti:8080 | xargs kill -9 2>/dev/null

echo "Starting RoamSafe from JAR with H2 (Memory DB)..."
nohup java -jar target/roamSafe-1.0.0.jar \
  --spring.datasource.url=jdbc:h2:mem:testdb \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.datasource.username=sa \
  --spring.datasource.password= \
  --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
  > app.log 2>&1 &
echo $! > app.pid
echo "Server started with PID $(cat app.pid). Logs in app.log"
