package client;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import server.ServerCommInterface;
import common.MasterRepInterface;
import common.WorkerCommInterface;

public class CrackerClient extends UnicastRemoteObject implements ClientCommInterface, MasterRepInterface {

    private String teamName = "Wi-Fighters";
    private ServerCommInterface server;
    private List<WorkerCommInterface> workers = new ArrayList<>();

    // Config
    private static final int WORKER_PORT = 1099; // Default RMI port for internal comms
    private static final String SERVER_HOST = "localhost"; // Default validation server host
    private static final String SERVER_SERVICE = "Server"; // Default service name

    protected CrackerClient() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        try {
            CrackerClient client = new CrackerClient();
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() throws Exception {
        // 1. Start Internal Registry for Workers
        try {
            LocateRegistry.createRegistry(WORKER_PORT);
            System.out.println("Internal RMI Registry started on port " + WORKER_PORT);
        } catch (Exception e) {
            System.out.println("RMI Registry already running or failed: " + e.getMessage());
        }

        Naming.rebind("rmi://localhost:" + WORKER_PORT + "/Master", this);
        System.out.println("MasterRepInterface bound.");

        // 2. Connect to Contest Server
        // Note: In a real scenario, we might want to wait for workers before
        // registering?
        // But instructions imply we register first.
        String serverUrl = "rmi://" + SERVER_HOST + "/" + SERVER_SERVICE;
        System.out.println("Connecting to server at " + serverUrl);
        server = (ServerCommInterface) Naming.lookup(serverUrl);

        server.register(teamName, this);
        System.out.println("Registered with server as " + teamName);
    }

    // --- MasterRepInterface Implementation ---

    @Override
    public synchronized void registerWorker(WorkerCommInterface worker) throws RemoteException {
        workers.add(worker);
        System.out.println("Worker registered. Total workers: " + workers.size());
    }

    @Override
    public void submitInternalSolution(String solution) throws RemoteException {
        System.out.println("Solution found by worker: " + solution);
        try {
            server.submitSolution(teamName, solution);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- ClientCommInterface Implementation ---

    @Override
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        System.out.println("Received problem. Max: " + problemsize);

        if (workers.isEmpty()) {
            System.err.println("No workers available to solve problem!");
            return;
        }

        long totalRange = problemsize;
        long chunkSize = totalRange / workers.size();

        // Simple equal distribution
        // For last worker, give the remainder

        for (int i = 0; i < workers.size(); i++) {
            long start = i * chunkSize;
            long end = (i == workers.size() - 1) ? totalRange : (start + chunkSize - 1);

            WorkerCommInterface worker = workers.get(i);

            // Call async to avoid blocking
            new Thread(() -> {
                try {
                    worker.solve(hash, start, end);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    // Handle worker failure? Remove from list?
                }
            }).start();
        }
    }
}
