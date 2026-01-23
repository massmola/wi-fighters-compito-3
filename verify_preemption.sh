#!/usr/bin/env bash
# Rebuild
rm -rf bin
mkdir -p bin
javac -d bin src/common/*.java src/server/*.java src/client/*.java src/worker/*.java

# Kill any existing java processes
pkill -f "java.*MockServer"
pkill -f "java.*CrackerClient"
pkill -f "java.*WorkerNode"

sleep 1

# Start Server
echo "Starting MockServer..."
# Publish a HARD problem (no solution in range 0-2M for example, or just long time)
# Then publish an EASY problem
# "publish 99999999" (Unsolvable in short time)
# Sleep 5
# "publish 12345" (Solvable quickly)
(
    sleep 5; 
    echo "publish 99999999"; 
    sleep 5; 
    echo "publish 12345"; 
    sleep 10
) | java -cp bin server.MockServer > server.log 2>&1 &
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

# Wait 
sleep 25

echo "--- Server Log ---"
cat server.log
echo "--- Client Log ---"
cat client.log
echo "--- Worker Log (Tail) ---"
tail -n 20 worker.log

echo "Killing processes..."
kill $SERVER_PID $CLIENT_PID $WORKER_PID
