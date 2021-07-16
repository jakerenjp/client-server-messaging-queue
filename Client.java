// Java implementation for a client 
  
import java.io.*; 
import java.net.*; 
import java.util.Scanner;
  
// Client class 
public class Client  
{ 
    // initializations
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;

    public Client(String address, String port)
    {
        int pt = Integer.parseInt(port);
        try
        {
            socket = new Socket(address, pt);
            System.out.println("Connected to " + address + " on port " + port);
            input = new DataInputStream(System.in);
            out = new DataOutputStream(socket.getOutputStream());
            Scanner message = new Scanner(System.in);
            String inputString = "";

            while(true)
            {   
                System.out.print("Client> ");
                inputString = message.nextLine();
                out.writeUTF(inputString);
                out.flush();
                if(inputString.equals("exit"))
                    break;
                DataInputStream socketdis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                String servInput = socketdis.readLine();
                System.out.println(servInput);
            }   
        }
        catch(UnknownHostException u)
        {
            System.out.println(u);
        }
        catch(IOException i)
        {
            System.out.println(i);
        }

        //close connection
        try
        {
            input.close();
            out.close();
            socket.close();
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }
    public static void main(String[] args)// throws IOException  
    { 
        Client client = new Client(args[0], args[1]);
    } 
} 