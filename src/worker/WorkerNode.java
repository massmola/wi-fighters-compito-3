package worker;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.math.BigInteger;

import common.MasterRepInterface;
import common.WorkerCommInterface;

public class WorkerNode extends UnicastRemoteObject implements WorkerCommInterface {

    private MasterRepInterface master;
    private static String masterHost = "localhost";
    private static final int MASTER_PORT = 1099;
    private static final String MASTER_SERVICE = "Master";

    private volatile boolean running = false;

    protected WorkerNode() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            masterHost = args[0];
        }
        try {
            String localIP = common.NetworkUtils.getLocalAddress();
            if (localIP != null) {
                System.setProperty("java.rmi.server.hostname", localIP);
                System.out.println("Set java.rmi.server.hostname to " + localIP);
            } else {
                System.err.println("Could not determine local IP, using default (likely localhost)");
            }

            WorkerNode worker = new WorkerNode();
            worker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() throws Exception {
        String url = "rmi://" + masterHost + ":" + MASTER_PORT + "/" + MASTER_SERVICE;
        System.out.println("Connecting to master at " + url);
        master = (MasterRepInterface) Naming.lookup(url);

        master.registerWorker(this);
        System.out.println("Registered with master.");
    }

    @Override
    public void stop() throws RemoteException {
        System.out.println("Stop signal received.");
        running = false;
    }

    @Override
    public void solve(byte[] hash, long start, long end) throws RemoteException {
        System.out.println("Starting search from " + start + " to " + end);

        new Thread(() -> {
            try {
                bruteForceMultiThreaded(hash, start, end);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bruteForceMultiThreaded(byte[] targetHash, long start, long end) {
        running = true;
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Using " + cores + " threads for computation.");

        long totalRange = end - start + 1;
        long chunkSize = totalRange / cores;

        Thread[] threads = new Thread[cores];
        for (int i = 0; i < cores; i++) {
            long tStart = start + (i * chunkSize);
            long tEnd = (i == cores - 1) ? end : (tStart + chunkSize - 1);

            threads[i] = new Thread(() -> {
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    for (long val = tStart; val <= tEnd && running; val++) {
                        String candidate = String.valueOf(val);
                        byte[] computedHash = md.digest(candidate.getBytes());

                        if (matches(computedHash, targetHash)) {
                            System.out.println("FOUND MATCH: " + candidate);
                            running = false; // Stop other local threads
                            master.submitInternalSolution(candidate);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }

        // Wait for threads (optional, but keep it clean)
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Finished (or stopped) range " + start + "-" + end);
    }

    private boolean matches(byte[] h1, byte[] h2) {
        if (h1.length != h2.length)
            return false;
        for (int i = 0; i < h1.length; i++) {
            if (h1[i] != h2[i])
                return false;
        }
        return true;
    }

    private String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%032x", bi);
    }
}
