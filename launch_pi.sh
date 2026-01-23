#!/bin/bash

# Usage: ./launch_pi.sh <SERVER_IP>

if [ -z "$1" ]; then
    echo "Usage: ./launch_pi.sh <SERVER_IP>"
    echo "Example: ./launch_pi.sh 192.168.1.50"
    exit 1
fi

SERVER_IP=$1

# Create bin and compile
mkdir -p bin
echo "Compiling..."
javac -d bin -sourcepath src src/server/*.java src/client/*.java src/worker/*.java src/common/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "--- Starting Client (connecting to $SERVER_IP) ---"
java -cp bin client.CrackerClient $SERVER_IP > client.log 2>&1 &
CLIENT_PID=$!
echo "Client started with PID $CLIENT_PID. Logs: client.log"

sleep 5

echo "--- Starting Worker (connecting to localhost) ---"
java -cp bin worker.WorkerNode localhost > worker.log 2>&1 &
WORKER_PID=$!
echo "Worker started with PID $WORKER_PID. Logs: worker.log"

echo "---------------------------------------------------"
echo "System running. Press Ctrl+C to stop both processes."
echo "---------------------------------------------------"

# Cleanup function to kill processes on exit
cleanup() {
    echo "Stopping processes..."
    kill $CLIENT_PID $WORKER_PID 2>/dev/null
    exit
}

# Trap SIGINT (Ctrl+C)
trap cleanup SIGINT

# Keep script running to maintain the trap
wait
