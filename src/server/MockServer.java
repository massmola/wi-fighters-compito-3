package server;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import client.ClientCommInterface;
import java.security.MessageDigest;
import java.util.Scanner;

public class MockServer extends UnicastRemoteObject implements ServerCommInterface {

    private ClientCommInterface registeredClient;

    // Config
    private static final int PORT = 1099;
    private static final String SERVICE_NAME = "Server";

    protected MockServer() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        try {
            // Start Registry
            try {
                LocateRegistry.createRegistry(PORT);
                System.out.println("Registry started on " + PORT);
            } catch (Exception e) {
                System.out.println("Registry already running.");
            }

            MockServer server = new MockServer();
            Naming.rebind("rmi://localhost:" + PORT + "/" + SERVICE_NAME, server);
            System.out.println("MockServer ready.");

            // Interaction loop
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nEnter command: publish <number> OR exit");

            while (scanner.hasNext()) {
                String cmd = scanner.next();
                if (cmd.equalsIgnoreCase("exit"))
                    break;

                if (cmd.equalsIgnoreCase("publish")) {
                    if (scanner.hasNext()) {
                        String number = scanner.next();
                        server.publish(number);
                    } else {
                        System.out.println("Missing number for publish command.");
                    }
                }
                System.out.println("\nEnter command: publish <number> OR exit");
            }
            System.out.println("MockServer exiting cleanly.");
            scanner.close();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register(String teamName, ClientCommInterface cc) throws Exception {
        this.registeredClient = cc;
        System.out.println("Team registered: " + teamName);
    }

    @Override
    public void submitSolution(String name, String sol) throws Exception {
        System.out.println("!!! SOLUTION SUBMITTED by " + name + ": " + sol + " !!!");
    }

    public void publish(String numberStr) {
        if (registeredClient == null) {
            System.out.println("No client registered.");
            return;
        }

        try {
            byte[] hash = getMD5(numberStr);
            // Problem size must be large enough to cover the number
            int problemSize = Integer.parseInt(numberStr) * 2; // Simple heuristic for test
            // Or just a large number
            if (problemSize < 1000000)
                problemSize = 1000000;

            System.out.println("Publishing problem for hash of " + numberStr);
            registeredClient.publishProblem(hash, problemSize);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getMD5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(input.getBytes());
    }
}
