package common;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkerCommInterface extends Remote {
    /**
     * Instructs the worker to start solving the problem in the given range.
     * @param hash The MD5 hash to crack
     * @param start The starting number (inclusive)
     * @param end The ending number (inclusive)
     * @throws RemoteException
     */
    void solve(byte[] hash, long start, long end) throws RemoteException;

    /**
     * Stops the calculation on this worker.
     * @throws RemoteException
     */
    void stop() throws RemoteException;
}
