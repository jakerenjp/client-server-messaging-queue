import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
import java.lang.*;
import java.util.regex.*;
import java.nio.file.*;

// Server class 
public class Server  
{ 
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;
    private String localIP = null;
    private int localPort;
    private MemoryQueue queue = null;
    private Hashtable<String, MemoryQueue> queueStore = new Hashtable<String, MemoryQueue>();
    private Exchange exchange = null;
    private Boolean exist = false;


    public Server(String type, String name)
    {
        //starts server and looks for connection
        try
        {
            InetAddress hostIP = InetAddress.getLocalHost();
            localIP = hostIP.getHostAddress();
            server = new ServerSocket(0);
            localPort = server.getLocalPort();

            if (type.equals("queue"))
            {
                try
                {
                    String prevMsg = "";
                    BufferedReader br = new BufferedReader(new FileReader("./" + name + ".log"));
                    queue = new MemoryQueue(name, localIP, localPort);
                    prevMsg = br.readLine();
                    System.out.println(prevMsg);
                    String[] arrInput = prevMsg.split("\\|", 0);
                    for (int i = 0; i < arrInput.length; i++)
                    {
                        queue.put(arrInput[i]);
                        System.out.println(arrInput[i]);
                    }
                    queueStore.put(name, queue);
                    exist = true;
                    System.out.println(name + " recovered at " + localIP + " on port " + localPort);
                }
                catch(FileNotFoundException f)
                {
                }
                if (!exist)
                {
                    queue = new MemoryQueue(name, localIP, localPort);
                    System.out.println(name + " created at " + localIP + " on port " + localPort);
                    queueStore.put(name, queue);
                }
            } 
            else if (type.equals("exchange"))
            {
                exchange = new Exchange(name, localIP, localPort);
                System.out.println(name + " created at " + localIP + " on port " + localPort);
            }
            else
            {
                System.out.println("Invalid input detected");
                return;
            }
            String serverInput = "";
            Scanner serverMsg = new Scanner(System.in);
            System.out.print("Server> ");
            serverInput = serverMsg.nextLine();
            String[] arrServInput = serverInput.split(" ", 0);
            /*if(arrServInput[0].equals("create") && arrServInput[1].equals("queue"))
            {
                String queueName = arrServInput[arrServInput.length - 1];
                if(queueStore.containsKey(queueName))
                {
                    out.writeUTF("Queue name taken\n\r");
                    out.flush();
                }
                else 
                {
                    Enumeration queues = queueStore.elements();
                    String enuIP = null;
                    int enuPort;
                    MemoryQueue currentEnum = null;
                    Boolean error = false;
                    while(queues.hasMoreElements())
                    {
                        currentEnum = (MemoryQueue)queues.nextElement();
                        enuIP = currentEnum.getIP().toString();
                        enuPort = currentEnum.getPort();
                        if (enuIP.equals(localIP) && enuPort == localPort)
                        {
                            out.writeUTF("Cannot create multiple queues on the same server and port\r\n");
                            error = true;
                            break;
                        }
                    }
                    if (!error)
                    {
                        MemoryQueue queue = new MemoryQueue(queueName, localIP, localPort);
                        queueStore.put(queueName, queue);
                    }
                }
            }*/
            while (arrServInput[0].equals("bind"))
            {
                String bindQName = arrServInput[1];
                String bindQIP = arrServInput[2];
                int bindPort = Integer.parseInt(arrServInput[3]);
                queueStore.put(bindQName, new MemoryQueue(bindQName, bindQIP, bindPort));
                //bind(bindQIP, bindPort);

                System.out.println(bindQName + " has been binded");

                System.out.print("Server> ");
                serverInput = serverMsg.nextLine();
                arrServInput = serverInput.split(" ", 0);
            }

            System.out.println("Waiting for client connection ...");

            socket = server.accept();
            System.out.println("Client accepted");

            input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
            Boolean socketOn = true;
            Thread clientThread = new ClientHandler(socket, input, out, queueStore, exchange);

            while(true)
            {
                if (!socketOn)
                {
                    server = new ServerSocket(0);
                    localPort = server.getLocalPort();
                    System.out.println("Server listening at " + localIP + " on port " + localPort);
                    System.out.println("Waiting for client connection ...");

                    socket = server.accept();
                    System.out.println("Client accepted");

                    input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    out = new DataOutputStream(socket.getOutputStream());
                    clientThread = new ClientHandler(socket, input, out, queueStore, exchange);
                    socketOn = true;
                }

                clientThread.start();
                socketOn = false;
                Runtime.getRuntime().addShutdownHook(
                new Thread("queue-kill-hook")
                {
                    @Override
                    public void run()
                    {
                        String queueContent = "";
                        while (queue.list() != 0)
                        {
                            queueContent = queueContent + queue.get() + "|";
                        }
                        try
                        {
                            File file = new File("./" + queue.getName().toString() + ".log");
                            BufferedWriter output = new BufferedWriter(new FileWriter(file));
                            output.write(queueContent);
                            output.close();
                            Process proc = Runtime.getRuntime().exec("chmod 666 ./" + queue.getName().toString() + ".log");
                        }
                        catch(IOException e)
                        {
                            System.out.println(e);
                        }
                    }   
                });
            }



            /*String line = "";
            Boolean socketOn = true;

            while(true)
            {
                if (!socketOn)
                {
                    server = new ServerSocket(0);
                    localPort = server.getLocalPort();
                    System.out.println("Server Initiated at " + localIP + " on port " + localPort);
                    System.out.println("Waiting for client connection ...");

                    socket = server.accept();
                    System.out.println("Client accepted");

                    input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    line = "";
                    out = new DataOutputStream(socket.getOutputStream());
                    socketOn = true;
                }
            }*/

        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    public void bind(String ip, int port)
    {
        try
        {
            Socket serverConnection = new Socket(ip, port);
            ObjectOutputStream bindSend = new ObjectOutputStream(serverConnection.getOutputStream());
            ObjectInputStream bindReceive = new ObjectInputStream(serverConnection.getInputStream());

            bindSend.writeObject(queue);

            MemoryQueue bindedQueue = (MemoryQueue) bindReceive.readObject();
            System.out.println(bindedQueue.getName().toString() + " binded");
            queueStore.put(bindedQueue.getName().toString(), bindedQueue);

            bindSend.close();
            bindReceive.close();

            ServerSocket serverConnectionSS = new ServerSocket(port);
            System.out.println("Binding server...");

            Socket pipe = serverConnectionSS.accept();

            ObjectInputStream serverInputStream = new ObjectInputStream(pipe.getInputStream());
            ObjectOutputStream serverOutputStream = new ObjectOutputStream(pipe.getOutputStream());

            MemoryQueue receivedQueue = (MemoryQueue) serverInputStream.readObject();
            queueStore.put(receivedQueue.getName().toString(), receivedQueue);

            serverOutputStream.writeObject(queue);

            serverInputStream.close();
            serverOutputStream.close();
        } 
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    public MemoryQueue getQueue()
    {
        return this.queue;
    }

    public static void main(String[] args)
    {
        if (args[0].equals("create"))
        {
            Server server = new Server(args[1], args[2]);
        }
        else
        {
            System.out.println("Invalid input detected");
        }

    }

} 

class ClientHandler extends Thread
{
    final DataInputStream input;
    final DataOutputStream out;
    final Socket socket;
    private Hashtable<String, MemoryQueue> queueStore;
    private Exchange exchange;

    public ClientHandler(Socket socket, DataInputStream input, DataOutputStream out, Hashtable<String, MemoryQueue> hash, Exchange exchange)
    {
        this.socket = socket;
        this.input = input;
        this.out = out;
        this.queueStore = hash;
        this.exchange = exchange;
    }

    @Override
    public void run()
    {
        String line = "";

        while(true)
        {
            try
            {
                line = input.readUTF();
                System.out.println(line);
                String[] arrLine = line.split(" ", 0);

                if (arrLine[0].equals("list"))
                {
                    String queueName = arrLine[arrLine.length - 1];
                    if (!queueStore.containsKey(queueName))
                    {
                        out.writeUTF("Queue does not exist\n\r");
                        out.flush();
                    }
                    else
                    {
                        int total = queueStore.get(queueName).list();
                        String msg = Integer.toString(total);
                        if (total == 1)
                        {
                            out.writeUTF(msg + " message\r\n");
                            out.flush();
                        }
                        else
                        {
                            out.writeUTF(msg + " messages\r\n");
                            out.flush();
                        }
                    }
                }
                else if (arrLine[0].equals("subscribe"))
                {
                    for (int i = 1; i < arrLine.length; i++)
                    {
                        exchange.subscribe(arrLine[i]);
                    }
                }
                else if (arrLine[0].equals("get"))
                {
                    String queueName = arrLine[arrLine.length - 1];
                    if (!queueStore.containsKey(queueName))
                    {
                        out.writeUTF("Queue does not exist\n\r");
                        out.flush();
                    }
                    else
                    {
                        MemoryQueue current = queueStore.get(queueName);
                        String msg = current.get().toString();
                        out.writeUTF(msg + "\n\r");
                        out.flush();
                    }                        
                }
                else if (arrLine[0].equals("publish"))
                {
                    Hashtable<String, MemoryQueue> exchangeHash = exchange.getHash();
                    String msg = line.substring(line.indexOf('\"'));
                    String newLine = line.substring(0, line.indexOf('\"') - 1);
                    String[] newArrLine = newLine.split(" ", 0);

                    if (arrLine[1].equals(exchange.getName().toString()))
                    {
                        for (int i = 2; i < newArrLine.length; i++)
                        {
                            if (exchange.isSubscribed(newArrLine[i]))
                            {
                                Enumeration queuesInExch = exchangeHash.elements();
                                MemoryQueue currentEnumQ = null;
                                while (queuesInExch.hasMoreElements())
                                {
                                    currentEnumQ = (MemoryQueue) queuesInExch.nextElement();
                                    currentEnumQ.put(msg);
                                }
                                out.writeUTF("Message received from subscription " + newArrLine[i] + "\r\n");
                            }
                            else
                            {
                                out.writeUTF("Not subscribed to " + newArrLine[i] + "\r\n");
                            }
                        }
                    }
                    else
                    {
                        out.writeUTF("Invalid exchange input\r\n");
                    }
                }
                else if (arrLine[0].equals("put"))
                {
                    String msg = line.substring(line.indexOf('\"'));
                    String newLine = line.substring(0, line.indexOf('\"') - 1);
                    String[] newArrLine = newLine.split(" ", 0);
                    if(arrLine[1].equals(exchange)) 
                    {
                        Hashtable<String, MemoryQueue> exchangeHash = exchange.getHash();
                        for (int i = 2; i < newArrLine.length; i++)
                        {
                            String queueName = newArrLine[i];
                            if (queueName.substring(queueName.length() - 1).equals("*"))
                            {
                                String matchName = queueName.substring(0, queueName.length() - 1) + ".*";
                                Enumeration queuesInExch = exchangeHash.elements();
                                MemoryQueue currentEnumQ = null;
                                String currentName = null;
                                while (queuesInExch.hasMoreElements())
                                {
                                    currentEnumQ = (MemoryQueue) queuesInExch.nextElement();
                                    currentName = currentEnumQ.getName().toString();
                                    Boolean match = Pattern.matches(matchName, currentName);
                                    if (match)
                                    {
                                        currentEnumQ.put(msg);
                                        out.writeUTF(currentName + " has received message\r\n");
                                        out.flush();
                                    }
                                }
                            }
                            else if (!exchangeHash.containsKey(queueName))
                            {
                                out.writeUTF(queueName + " does not exist\n\r");
                                out.flush();
                            }
                            else
                            {
                                exchangeHash.get(queueName).put(msg);
                                out.writeUTF(queueName + " has received message\r\n");
                                out.flush();
                            }
                        }                        
                    }
                    else
                    {
                    //if direct put into queue
                        for (int i = 1; i < newArrLine.length; i++)
                        {
                            String queueName = newArrLine[i];
                            if (queueName.substring(queueName.length() - 1).equals("*"))
                            {
                                String matchName = queueName.substring(0, queueName.length() - 1) + ".*";
                                Enumeration queuesInExch = queueStore.elements();
                                MemoryQueue currentEnumQ;
                                String currentName = null;
                                while (queuesInExch.hasMoreElements())
                                {
                                    currentEnumQ = (MemoryQueue) queuesInExch.nextElement();
                                    currentName = currentEnumQ.getName().toString();
                                    Boolean match = Pattern.matches(matchName, currentName);
                                    if (match)
                                    {
                                        currentEnumQ.put(msg);
                                        out.writeUTF(currentName + " has received message\r\n");
                                        out.flush();
                                    }
                                }
                            }
                            else if (!queueStore.containsKey(queueName))
                            {
                                out.writeUTF(queueName + " does not exist\n\r");
                                out.flush();
                            }
                            else
                            {
                                queueStore.get(queueName).put(msg);
                                out.writeUTF(queueName + " has received message\r\n");
                                out.flush();
                            }   
                        }
                    }   
                }
                else if (arrLine[0].equals("subscribe"))
                {

                }
                else if (arrLine[0].equals("exit"))
                {
                    //socketOn = false;
                    System.out.println("Closing Connection");
                    this.socket.close();      
                    this.input.close();
                    this.out.close();
                    break;
                }
                else
                {
                    out.writeUTF("Invalid request\r\n");
                    out.flush();
                }
            }
            catch(IOException i)
            {
                System.out.println(i);
            }
        }
    }
}
