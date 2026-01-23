#!/usr/bin/env bash
# Rebuild
rm -rf bin
mkdir -p bin
javac -d bin src/common/*.java src/server/*.java src/client/*.java src/worker/*.java

# Kill any existing java processes (be careful with this in prod, but ok for test env)
pkill -f "java.*MockServer"
pkill -f "java.*CrackerClient"
pkill -f "java.*WorkerNode"

sleep 1

# Start Server
echo "Starting MockServer..."
# Publish twice to trigger cache
(sleep 5; echo "publish 12345"; sleep 15; echo "publish 12345"; sleep 5) | java -cp bin server.MockServer > server.log 2>&1 &
SERVER_PID=$!
sleep 2

# Start Client (connecting to localhost server)
echo "Starting CrackerClient..."
java -cp bin client.CrackerClient localhost > client.log 2>&1 &
CLIENT_PID=$!
sleep 2

# Start Worker (connecting to localhost master)
echo "Starting WorkerNode..."
java -cp bin worker.WorkerNode localhost > worker.log 2>&1 &
WORKER_PID=$!
sleep 2

# Wait a bit
sleep 30

echo "--- Server Log ---"
cat server.log
echo "--- Client Log ---"
cat client.log
echo "--- Worker Log ---"
cat worker.log

echo "Killing processes..."
kill $SERVER_PID $CLIENT_PID $WORKER_PID
