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
    private boolean solutionFound = false;

    // Config
    private static final int WORKER_PORT = 1099;
    private static String serverHost = "localhost";
    private static final String SERVER_SERVICE = "Server";

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

        Naming.rebind("rmi://localhost:" + WORKER_PORT + "/Master", this);
        System.out.println("MasterRepInterface bound.");

        String serverUrl = "rmi://" + serverHost + "/" + SERVER_SERVICE;
        System.out.println("Connecting to contest server at " + serverUrl);
        server = (ServerCommInterface) Naming.lookup(serverUrl);

        server.register(teamName, this);
        System.out.println("Registered with server as " + teamName);
    }

    @Override
    public synchronized void registerWorker(WorkerCommInterface worker) throws RemoteException {
        workers.add(worker);
        System.out.println("Worker registered. Total workers: " + workers.size());
    }

    @Override
    public synchronized void submitInternalSolution(String solution) throws RemoteException {
        if (solutionFound) return;
        solutionFound = true;

        System.out.println("Solution found by a worker: " + solution);
        stopAllWorkers();
        try {
            server.submitSolution(teamName, solution);
        } catch (Exception e) {
            System.err.println("Failed to submit solution to server: " + e.getMessage());
        }
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
        }

        synchronized (this) {
            if (workers.isEmpty()) {
                System.err.println("No workers available!");
                return;
            }

            long totalRange = problemsize;
            long chunkSize = totalRange / workers.size();

            for (int i = 0; i < workers.size(); i++) {
                long start = i * chunkSize;
                long end = (i == workers.size() - 1) ? totalRange : (start + chunkSize - 1);
                WorkerCommInterface worker = workers.get(i);

                new Thread(() -> {
                    try {
                        worker.solve(hash, start, end);
                    } catch (RemoteException e) {
                        System.err.println("Worker failed during solve. Removing. Error: " + e.getMessage());
                        e.printStackTrace();
                        synchronized (this) {
                            workers.remove(worker);
                        }
                    }
                }).start();
            }
        }
    }
}
