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
    // Hardcoded master address for now, usually passed as arg
    private static final String MASTER_HOST = "localhost";
    private static final int MASTER_PORT = 1099;
    private static final String MASTER_SERVICE = "Master";

    protected WorkerNode() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        try {
            WorkerNode worker = new WorkerNode();
            worker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() throws Exception {
        String url = "rmi://" + MASTER_HOST + ":" + MASTER_PORT + "/" + MASTER_SERVICE;
        System.out.println("Connecting to master at " + url);
        master = (MasterRepInterface) Naming.lookup(url);

        master.registerWorker(this);
        System.out.println("Registered with master.");
    }

    @Override
    public void solve(byte[] hash, long start, long end) throws RemoteException {
        System.out.println("Starting search from " + start + " to " + end);

        // Run in new thread to not block the RMI calling thread (though RMI might
        // handle this, better safe)
        // Wait, instructions say distributed load. If we start a thread, RMI returns
        // immediately.
        // But the master's loop calls `worker.solve` in a thread, so the Master doesn't
        // block.
        // If `solve` blocks here, the Master's thread for this worker blocks. That's
        // fine.
        // Actually, if we want to support cancellation or other things, a separate
        // thread is better.
        // Im keeping it simple: run in this thread. RMI call is sync.

        // EXCEPT: RMI timeouts? If the task takes 10 minutes, RMI might timeout.
        // So better to return immediately and run async.

        new Thread(() -> {
            try {
                bruteForce(hash, start, end);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bruteForce(byte[] targetHash, long start, long end) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String hexTarget = toHex(targetHash);
            System.out.println("Target: " + hexTarget);

            for (long i = start; i <= end; i++) {
                String candidate = String.valueOf(i);
                byte[] bytes = candidate.getBytes();
                byte[] computedHash = md.digest(bytes);

                // Compare
                // Optimization: Arrays.equals or manual
                // Converting to hex every time is slow. Compare bytes.
                boolean match = true;
                for (int j = 0; j < computedHash.length; j++) {
                    if (computedHash[j] != targetHash[j]) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    System.out.println("FOUND MATCH: " + candidate);
                    try {
                        master.submitInternalSolution(candidate);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return; // Stop searching
                }
            }
            System.out.println("Finished range " + start + "-" + end + " with no match.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%032x", bi);
    }
}
