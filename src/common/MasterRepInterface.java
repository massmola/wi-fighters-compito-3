package common;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterRepInterface extends Remote {
    /**
     * Registers a worker with the master.
     * @param worker The worker stub
     * @throws RemoteException
     */
    void registerWorker(WorkerCommInterface worker) throws RemoteException;

    /**
     * Called by a worker when it finds a solution.
     * @param solution The numeric string solution
     * @throws RemoteException
     */
    void submitInternalSolution(String solution) throws RemoteException;

    /**
     * Called by a worker when it finishes a task range without finding a solution.
     * @param worker The worker stub
     * @throws RemoteException
     */
    void taskCompleted(WorkerCommInterface worker) throws RemoteException;
}
