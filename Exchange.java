import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
import java.lang.*;
import java.util.regex.*;

public class Exchange
{
	private Hashtable<String, MemoryQueue> queues = new Hashtable<String, MemoryQueue>();
	private String name;
	private String ip;
	private int port;
	private ArrayList<String> subscriptions = new ArrayList<String>();

	public Exchange(String name, String ip, int port)
	{
		this.name = name;
		this.ip = ip;
		this.port = port;
	}

	public void bindQueue(MemoryQueue queue)
	{
		queues.put(queue.getName().toString(), queue);
	}

	public void subscribe(String topic)
	{
		subscriptions.add(topic);
	}

	public Boolean isSubscribed (String topic)
	{
		return subscriptions.contains(topic);
	}

	public Hashtable<String, MemoryQueue> getHash()
	{
		return this.queues;
	}

	public String getName()
	{
		return this.name;
	}
}