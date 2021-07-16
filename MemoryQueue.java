import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
import java.lang.*;

public class MemoryQueue<String> implements Serializable
{
    private String name = null;
    private String IP = null;
    private int port;
    private int total;
    private Node first, last;

    private class Node{
        private String element;
        private Node next;
    }

    public MemoryQueue(String n, String IP, int port)
    {
        this.name = n;
        this.IP = IP;
        this.port = port;
        total = 0;
    } 

    public int list() 
    {
        return total;
    }

    public String get()
    {
        if (total == 0) 
            throw new java.util.NoSuchElementException();
        String message = first.element;
        first = first.next;
        --total;
        if (total == 0)
            last = null;
        return message;
    }

    public void put(String message)
    {
        Node current = last;
        last = new Node();
        last.element = message;
        total++;

        if (total == 1)
            first = last;
        else
            current.next = last;
    }

    public String getIP()
    {
        return this.IP;
    }

    public int getPort()
    {
        return this.port;
    }

    public String getName()
    {
        return this.name;
    }
}