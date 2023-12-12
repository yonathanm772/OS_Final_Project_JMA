import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class Client 
{
	private Socket socket;
	private BufferedReader buffered_reader;
	private BufferedWriter buffered_writer;
	private String username;
	
	public Client(Socket socket, String username)
	{
		try
		{
			this.socket = socket;
			this.buffered_writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			this.buffered_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.username = username;
		}
		catch(IOException e)
		{
			closeEverything(socket, buffered_reader, buffered_writer);
		}
	}
	
	public void sendMessage()
	{
		try
		{
			buffered_writer.write(username);
			buffered_writer.newLine();
			buffered_writer.flush();
			
			Scanner scan = new Scanner(System.in);
			while(socket.isConnected())
			{
				String messageToSend = scan.nextLine();
				buffered_writer.write(username + ": " + messageToSend);
				buffered_writer.newLine();
				buffered_writer.flush();
			}
		}
		catch(IOException e) 
		{
			closeEverything (socket, buffered_reader, buffered_writer);
		}
	}
	
	public void listenForMessage()
	{
		new Thread (new Runnable()
		{
			public void run()
			{
				String messageFromGroupChat;
				
				while(socket.isConnected())
				{
					try
					{
						messageFromGroupChat = buffered_reader.readLine();
						System.out.println(messageFromGroupChat);
					}
					catch (IOException e)
					{
						closeEverything(socket, buffered_reader, buffered_writer);
					}
				}
			}
		}).start();
	}
	
	
	public void closeEverything(Socket client_socket, BufferedReader buffered_reader, BufferedWriter buffered_writer)
	{
		try
		{
			if (buffered_reader != null)
			{
				buffered_reader.close();
			}
			if(buffered_writer != null)
			{
				buffered_writer.close();
			}
			if (client_socket != null)
				client_socket.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String [] args) throws IOException
	{
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter you username for the group chat: ");
		String username = scan.nextLine();
		Socket socket = new Socket("localhost", 1237);
		Client client = new Client(socket, username);
		
		client.listenForMessage();
		client.sendMessage();

		
	}
}
