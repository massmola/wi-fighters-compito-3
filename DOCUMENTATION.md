# Distributed MD5 Cracker Documentation

## Team
**Team Name**: Wi-Fighters
**Members**: Massimiliano, Gioele, Lorenzo

## A) Strategy for Problem Subdivision
The problem space (integers from 0 to `problemsize`) is subdivided using a simple static partitioning strategy.
1.  The Master Node (`CrackerClient`) determines the total number of connected Worker Nodes.
2.  The total range `[0, problemsize]` is divided into `N` equal chunks, where `N` is the number of workers.
3.  Each worker is assigned a chunk via the `solve(start, end)` RMI method.
4.  If the number of items is not perfectly divisible, the last worker takes the remainder.

This strategy ensures that the entire search space is covered whitout overlapping.

## B) Communication Implementation
The system uses **Java RMI** for all network communication between machines.

### 1. Client-Server Communication (External)
-   **Register**: The Client calls `server.register(teamName, clientStub)` to join the contest.
-   **Publish**: The Server calls `client.publishProblem(hash, size)` to trigger the computation.
-   **Submit**: The Client calls `server.submitSolution(teamName, solution)` when a match is found.

### 2. Master-Worker Communication (Internal)
-   **Register**: Workers connect to the Master's RMI Registry and call `registerWorker(workerStub)`.
-   **Task Assignment**: The Master asynchronously calls `worker.solve(hash, start, end)` on each worker.
-   **Result Submission**: When a worker finds the solution, it calls `master.submitInternalSolution(solution)`. The Master then forwards this to the main Server.
