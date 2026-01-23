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
(sleep 5; echo "publish 12345"; sleep 10) | java -cp bin server.MockServer > server.log 2>&1 &
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

# Send a command to the server to publish a problem by writing to its stdin?
# No, easier way: MockServer reads stdin. We can redirect input to it, but it's a background process.
# Just check registration for now.
# Attempt to publish a problem using a separate process is tricky because the server keeps state.
# WE can modify the script to pipe input.

# Wait a bit
sleep 5

echo "--- Server Log ---"
cat server.log
echo "--- Client Log ---"
cat client.log
echo "--- Worker Log ---"
cat worker.log

echo "Killing processes..."
kill $SERVER_PID $CLIENT_PID $WORKER_PID
