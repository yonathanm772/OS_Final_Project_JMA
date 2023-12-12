import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleHTTPserver 
{
	private ServerSocket serverSocket;
	private BufferedReader buffered_reader;
	private BufferedWriter buffered_writer;
	private String user_input;
	
	public SimpleHTTPserver(ServerSocket serverSocket)
	{
		this.serverSocket = serverSocket;
	}
	
	public void startServer()
	{
		try
		{
			while(!serverSocket.isClosed())
			{
				Socket socket = serverSocket.accept();
				System.out.println("A new client has connected!");
				WordGame game = new WordGame(socket);
				
				
				buffered_reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(socket.getInputStream())));
				PrintWriter  pr = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);
				
				pr.println("Would you like to join the game chat?");
				
				splitString();
				
				while(!user_input.matches("[a-zA-Z]+"))
				{
					pr.println("Please enter a valid answer!");
					splitString();
				}
				
				String messageFromClient = user_input;
				if(messageFromClient.charAt(0) == 'Y' || messageFromClient.charAt(0) == 'y')
				{
					Thread thread1 = new Thread(game);
					thread1.start();
				}
				else
				{
					socket.close();
					game.remove();
				}
				
	             
			}
		}
		catch (IOException e)
		{
			
		}
	}
	
	public void closeServerSocket()
	{
		try
		{
			if(serverSocket != null)
			{
				serverSocket.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public void splitString()
	{
		try
		{
			String temp = buffered_reader.readLine().toUpperCase();
			String [] parts = temp.split(" ");
			this.user_input = parts[1];
		}
		catch(IOException e )
		{
			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String [] args) throws IOException
	{
		ServerSocket serverSocket = new ServerSocket(1237);
		SimpleHTTPserver server = new SimpleHTTPserver(serverSocket);
		server.startServer();
	}
}

//need to display that someone is playing and stop other players from playing
// learn where to put the blocking method
// break out of the game once is over
