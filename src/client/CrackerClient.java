package client;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import server.ServerCommInterface;
import common.MasterRepInterface;
import common.WorkerCommInterface;
import client.ClientCommInterface;

public class CrackerClient extends UnicastRemoteObject implements ClientCommInterface {

    private String teamName = "Wi-Fighters";
    private ServerCommInterface server;
    private List<WorkerCommInterface> workers = new ArrayList<>();
    private boolean solutionFound = false;
    
    // Chunking state
    private Queue<long[]> pendingTasks = new LinkedList<>();
    private byte[] currentProblemHash;

    // Config
    private static final int WORKER_PORT = 1099;
    private static String serverHost = "localhost";
    private static final String SERVER_SERVICE = "server";
    // Number of chunks to split the work into. 
    // Higher number = better load balancing but more RMI overhead.
    private static final int TOTAL_CHUNKS = 5; 

    protected CrackerClient() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            serverHost = args[0];
        }
        try {
            String localIP = common.NetworkUtils.getLocalAddress();
            if (localIP != null) {
                System.setProperty("java.rmi.server.hostname", localIP);
                System.out.println("Set java.rmi.server.hostname to " + localIP);
            } else {
                System.err.println("Could not determine local IP, using default");
            }

            CrackerClient client = new CrackerClient();
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() throws Exception {
        try {
            LocateRegistry.createRegistry(WORKER_PORT);
            System.out.println("Internal RMI Registry started on port " + WORKER_PORT);
        } catch (Exception e) {
            System.out.println("RMI Registry already running or failed: " + e.getMessage());
        }

        // Create and bind the separate handler for Workers
        WorkerHandler workerHandler = new WorkerHandler();
        Naming.rebind("rmi://localhost:" + WORKER_PORT + "/Master", workerHandler);
        System.out.println("MasterRepInterface bound (WorkerHandler).");

        String serverUrl = "rmi://" + serverHost + "/" + SERVER_SERVICE;
        System.out.println("Connecting to contest server at " + serverUrl);

        try {
            server = (ServerCommInterface) Naming.lookup(serverUrl);
        } catch (java.rmi.NotBoundException e) {
            System.err.println("Error: Service '" + SERVER_SERVICE + "' not bound at " + serverUrl);
            System.err.println("Available services at " + serverHost + ":");
            try {
                String[] list = Naming.list("rmi://" + serverHost + ":1099");
                for (String s : list) {
                    System.err.println(" - " + s);
                }
            } catch (Exception listEx) {
                System.err.println("Could not list services: " + listEx.getMessage());
            }
            throw e; // Re-throw to stop execution
        }

        server.register(teamName, this);
        System.out.println("Registered with server as " + teamName);
    }

    // --- Inner class to handle Workers ---
    private class WorkerHandler extends UnicastRemoteObject implements MasterRepInterface {
        protected WorkerHandler() throws RemoteException {
            super();
        }

        @Override
        public void registerWorker(WorkerCommInterface worker) throws RemoteException {
            synchronized (CrackerClient.this) {
                workers.add(worker);
                System.out.println("Worker registered. Total workers: " + workers.size());
            }
        }

        @Override
        public void submitInternalSolution(String solution) throws RemoteException {
            CrackerClient.this.submitInternalSolution(solution);
        }

        @Override
        public void taskCompleted(WorkerCommInterface worker) throws RemoteException {
            CrackerClient.this.assignNextTask(worker);
        }
    }

    // --- Logic ---

    public synchronized void submitInternalSolution(String solution) {
        if (solutionFound) return;
        solutionFound = true;

        System.out.println("Solution found by a worker: " + solution);
        stopAllWorkers();
        pendingTasks.clear(); // Clear separate tasks
        try {
            server.submitSolution(teamName, solution);
        } catch (Exception e) {
            System.err.println("Failed to submit solution to server: " + e.getMessage());
        }
    }

    private synchronized void assignNextTask(WorkerCommInterface worker) {
        if (solutionFound) return;
        
        long[] range = pendingTasks.poll();
        if (range == null) {
            // No more work
            System.out.println("No more tasks pending. Worker idle.");
            return;
        }

        long start = range[0];
        long end = range[1];
        
        System.out.println("Assigning range " + start + "-" + end + " to worker."); // Can include worker ID if available
        
        new Thread(() -> {
            try {
                worker.solve(currentProblemHash, start, end);
            } catch (RemoteException e) {
                System.err.println("Worker failed during solve. Removing.");
                synchronized (CrackerClient.this) {
                    workers.remove(worker);
                    // Re-queue the failed task!
                    pendingTasks.add(range); 
                }
            }
        }).start();
    }

    private void stopAllWorkers() {
        System.out.println("Stopping all workers...");
        for (WorkerCommInterface worker : new ArrayList<>(workers)) {
            new Thread(() -> {
                try {
                    worker.stop();
                } catch (RemoteException e) {
                    System.err.println("Failed to stop a worker (likely disconnected).");
                }
            }).start();
        }
    }

    @Override
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        System.out.println("Received problem. Max: " + problemsize);
        synchronized (this) {
            solutionFound = false;
            currentProblemHash = hash;
            pendingTasks.clear();

            if (workers.isEmpty()) {
                System.err.println("No workers available!");
                return;
            }

            // Chunk generation
            long totalRange = problemsize;
            // Use Math.max to prevent chunk size 0
            long chunkSize = Math.max(1, totalRange / TOTAL_CHUNKS);
            
            List<long[]> chunks = new ArrayList<>();
            for (int i = 0; i < TOTAL_CHUNKS; i++) {
                long start = i * chunkSize;
                // If last chunk, go to end (handle remainder)
                long end = (i == TOTAL_CHUNKS - 1) ? totalRange : (start + chunkSize - 1);
                
                if (start <= end) {
                    chunks.add(new long[]{start, end});
                }
            }

            // Randomize order
            Collections.shuffle(chunks);
            pendingTasks.addAll(chunks);
            System.out.println("Generated " + chunks.size() + " random chunks. Starting distribution...");

            // Initial assignment: give 1 chunk to each worker
            for (WorkerCommInterface worker : workers) {
                assignNextTask(worker);
            }
        }
    }
}
