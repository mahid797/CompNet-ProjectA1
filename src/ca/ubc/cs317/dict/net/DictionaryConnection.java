package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

import static ca.ubc.cs317.dict.net.DictStringParser.splitAtoms;
import static ca.ubc.cs317.dict.net.Status.readStatus;

public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;
    private Socket socket;
    private BufferedReader socketInput;
    private PrintWriter socketOutput;

    /**
     * Establishes a new connection with a DICT server using an explicit host and
     * port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection
     *                                 can't be established, or the messages
     *                                 don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {

        try {
            this.socket = new Socket(host, port);
            this.socketOutput = new PrintWriter(socket.getOutputStream(), true);
            this.socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connection established; Host: " + host + " on port : " + port);

            Status a = readStatus(socketInput);
            if (a.getStatusCode() != 220) {
                throw new DictConnectionException();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new DictConnectionException();
        }

    }

    /**
     * Establishes a new connection with a DICT server using an explicit host, with
     * the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection
     *                                 can't be established, or the messages
     *                                 don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /**
     * Sends the final QUIT message and closes the connection with the server. This
     * function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the
     * connection.
     *
     */
    public synchronized void close() {
        //socketOutput.flush();
        socketOutput.println("QUIT");

        try {
            Status a = readStatus(socketInput);
            socket.close();
            socketInput.close();
            socketOutput.close();

        } catch (Exception ex) {
            //System.out.println("Error closing the streams");
            System.exit(1);
        }

    }

    /**
     * Requests and retrieves a map of database name to an equivalent database
     * object for all valid databases used in the server.
     *
     * @return A map linking database names to Database objects for all databases
     *         supported by the server, or an empty map
     *         if no databases are available.
     * @throws DictConnectionException If the connection was interrupted or the
     *                                 messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();

        socketOutput.println("SHOW DB");
        Status response = readStatus(socketInput);

        if (response.getStatusCode() == 110) {
            String nextLine = null;
            try {
                nextLine = socketInput.readLine();
            } catch (IOException e) {

            }

            while (!nextLine.equals(".")) {

                String[] a = splitAtoms(nextLine);
                Database newDB = new Database(a[0], a[1]);
                databaseMap.put(newDB.getName(), newDB);
                try {
                    nextLine = socketInput.readLine();
                } catch (IOException e) {}
            }
        }

        else if (response.getStatusCode() == 554) {
            // do nothing (not sure)
        }

        else {
            throw new DictConnectionException();
        }

        return databaseMap;
    }

    /**
     * Requests and retrieves a list of all valid matching strategies supported by
     * the server.
     *
     * @return A set of MatchingStrategy objects supported by the server, or an
     *         empty set if no strategies are supported.
     * @throws DictConnectionException If the connection was interrupted or the
     *                                 messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

        socketOutput.println("SHOW STRATEGIES");
        Status response = readStatus(socketInput);
        if (response.getStatusCode() == 111) {
            String nextLine = null;
            try {
                nextLine = socketInput.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                throw new DictConnectionException();
            }

            while (!nextLine.equals(".")) {
                String[] a = splitAtoms(nextLine);
                MatchingStrategy newMS = new MatchingStrategy(a[0], a[1]);
                set.add(newMS);
                try {
                    nextLine = socketInput.readLine();
                } catch (IOException e) {
                }
            }
        }

        else if (response.getStatusCode() == 555) {
            // do nothing (not sure)
        }

        else {
            throw new DictConnectionException();
        }

        return set;
    }

    /**
     * Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches
     *                 (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special
     *                 database may be specified,
     *                 indicating either that all regular databases should be used
     *                 (database name '*'), or that only
     *                 matches in the first database that has a match for the word
     *                 should be used (database '!').
     * @return A set of word matches returned by the server, or an empty set if no
     *         matches were found.
     * @throws DictConnectionException If the connection was interrupted, the
     *                                 messages don't match their expected
     *                                 value, or the database or strategy are
     *                                 invalid.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database)
            throws DictConnectionException {

        Set<String> set = new LinkedHashSet<>();

        String command = "MATCH " + database.getName() + " " + strategy.getName() + " " + "\"" + word + "\"";
        socketOutput.println(command);

        Status response = readStatus(socketInput);
        if (response.getStatusCode() == 152) {

            String nextLine = null;
            try {
                nextLine = socketInput.readLine();
            } catch (IOException e) {

            }
            while (!nextLine.equals(".")) {

                String[] a = splitAtoms(nextLine);
                set.add(a[1]);
                try {
                    nextLine = socketInput.readLine();
                } catch (IOException e) {}
            }
        }
        else if (response.getStatusCode() == 550) {
            throw new DictConnectionException();
        }
        else if (response.getStatusCode() == 551) {
            throw new DictConnectionException();
        }
        else if (response.getStatusCode() == 552) {
            // do nothing (not sure)
        }
        else {
            throw new DictConnectionException();
        }

        return set;
    }

    /**
     * Requests and retrieves all definitions for a specific word.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special
     *                 database may be specified,
     *                 indicating either that all regular databases should be used
     *                 (database name '*'), or that only
     *                 definitions in the first database that has a definition for
     *                 the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions
     *         returned by the server, or an empty
     *         collection if no definitions were returned.
     * @throws DictConnectionException If the connection was interrupted, the
     *                                 messages don't match their expected
     *                                 value, or the database is invalid.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database)
            throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();

        String command = "DEFINE " + database.getName() + " " + word;
        socketOutput.println(command);

        String nextLine = null;
        Status response = readStatus(socketInput);
        if (response.getStatusCode() == 150) {

            try {
                nextLine = socketInput.readLine();
                String[] a = splitAtoms(nextLine);
                String dbName = a[3];
            } catch (IOException e) {
                e.printStackTrace();
                throw new DictConnectionException();
            }
            while (!nextLine.equals(".")) {
                try {
                    nextLine = socketInput.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new DictConnectionException();
                }
            }
            try {
                nextLine = socketInput.readLine();
            } catch (IOException e) {
                e.printStackTrace();

            }

            //response = readStatus(socketInput);

            /*if (response.getStatusCode() == 250) {
                close();*/

        }

        else if (response.getStatusCode() == 151) {
                while (!nextLine.equals(".")) {
                    String[] a = splitAtoms(nextLine);
                    Definition newDef = new Definition(word, a[3]);
                    set.add(newDef);
                    try {
                        nextLine = socketInput.readLine();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }

        else if (response.getStatusCode() == 550) {
            throw new DictConnectionException();

        }

        else if (response.getStatusCode() == 552) {
            // do nothing (not sure)

        }
        else {
        }

        return set;
    }

}
