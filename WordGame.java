import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;

public class WordGame implements Runnable {
	private int tries;
	private ArrayList <String> word_bank;
	public static Queue<WordGame> gameHandlers = new LinkedList<WordGame>();
	public static Queue<WordGame> gameLobby = new LinkedList<WordGame>();
	
	private String answer;
	private String guess;
	private String user_input;
	private BufferedReader buffered_reader;
	private BufferedWriter buffered_writer;
	private PrintWriter  pr;
	private Socket game_socket;
	private String client_username;
	private int client_ID;
	private static int score = 0;
	
	
	public WordGame(Socket game_socket)
	{
		try
		{
			this.game_socket = game_socket;
			this.buffered_writer = new BufferedWriter(new OutputStreamWriter(game_socket.getOutputStream()));
			this.buffered_reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(game_socket.getInputStream())));
			this.pr = new PrintWriter(new BufferedOutputStream(game_socket.getOutputStream()), true);
			this.word_bank = new ArrayList <String>();
			this.answer = "";
			this.guess = "";
			this.user_input = "";
			this.tries = 6;
			this.client_username = buffered_reader.readLine();
			this.client_ID = game_socket.getPort();
			gameLobby.add(this);
		}
		catch(IOException e)
		{
			closeEverything(game_socket, buffered_reader, buffered_writer);
		}
		
	}
	
	public void run()
	{
		
		while(game_socket.isConnected())
		{
			String messageFromClient;
			pr.println("=== Welcome to the Server ===");
			pr.println("Would you like to play a game?");
			try
	        {
				splitString();
				messageFromClient = user_input;
				if(messageFromClient.charAt(0) == 'Y' || messageFromClient.charAt(0) == 'y')
				{
					gameLobby.remove(this);
					gameHandlers.add(this);
					pr.println(this.client_username + " is playing the game");
					start_game();
					System.out.println("=== A GAME HAS BEGUN===");
				}
				else
				{
					pr.println("=== Welcome to the Game Lobby ===");
					broadcastMessage(client_username + " HAS JOINED THE LOBBY");
					while(game_socket.isConnected())
					{
						messageFromClient = buffered_reader.readLine();
						broadcastMessage(messageFromClient);
						if(gameLobby.size() == 1)
						{
							pr.println("Looks like you are alone :(");
						}
						
					}
				}
			}
			catch (IOException e)
			{
				closeEverything(game_socket, buffered_reader, buffered_writer);
				break;
			}
				
		}
	}
	
	public void start_game()
	{
		readFile();
		generateAnswer();
		answer = getAnswer();
		final String ANSI_RESET = "\u001B[0m";
		final String ANSI_GREEN = "\u001B[32m";
		final String ANSI_BLUE = "\u001B[34m";
		
		while(!isGameOver())
		{
				pr.println("Please guess a 5 letter word: ");
				pr.println(ANSI_BLUE + "BLUE " + ANSI_RESET + "means the letter is in the word, but on the incorrect spot");
				pr.println(ANSI_GREEN + "GREEN " + ANSI_RESET + "means the letter is in the word, and on the correct spot");
				pr.println("Normal color means the letter is not in the word");
				System.out.println(answer);
				StringBuilder b = new StringBuilder();
		
				do
				{
					splitString();
					
					while(user_input.length() != 5 || !user_input.matches("[a-zA-Z]+"))
					{
						pr.println("Please enter a valid word!");
						splitString();
					}
					
					guess = user_input;
					
					
						
					for(int index = 0; index < guess.length(); index++)
					{
						char c = guess.charAt(index);
						
							
						if (answer.charAt(index) == c)
						{
							b.append(ANSI_GREEN + c + ANSI_RESET);
						}
						else if (answer.contains(Character.toString(c)))
						{
							b.append(ANSI_BLUE + c + ANSI_RESET);
						}
						else
						{
							b.append(c);
						}	
					}
					pr.println(b.toString());
					b = new StringBuilder();
					if(didWin())
					{
						pr.println("Congratulations! You Won!");
						removeGameHandler();
						addPoints();
						break;
					}
					else
						decrement_tries();
				}while(!didWin() && tries > 0);	
				
				if (isGameOver())
				{
					lost();
					break;
				}
		}
	}
	
	public void broadcastMessage(String messageToSend)
	{
		for(WordGame client_waiting : gameLobby)
		{
			try 
			{
				if(!client_waiting.client_username.equals(client_username))
				{
					client_waiting.buffered_writer.write(messageToSend);
					client_waiting.buffered_writer.newLine();
					client_waiting.buffered_writer.flush();
					
				}
					
			}
			catch(IOException e)
			{
				closeEverything(game_socket, buffered_reader, buffered_writer);
			}
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
	
	public void removeGameHandler()
	{
		gameHandlers.remove(this);
		gameLobby.add(this);
		System.out.println(gameHandlers.size());
		System.out.println(gameLobby.size());
	}
	
	public void remove()
	{
		
		gameLobby.remove(this);
		System.out.println("REMOVED");
		
	}
	
	public void closeEverything(Socket game_socket, BufferedReader buffered_reader, BufferedWriter buffered_writer)
	{
		
		gameLobby.remove(this);
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
			if (game_socket != null)
				game_socket.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		broadcastMessage("SERVER: " + client_username + " has left the chat :(");
	}
	
	public void readFile()
	{
		try 
		{
			File words = new File("valid-wordle-words.txt");
			Scanner scan = new Scanner(words);
		    while (scan.hasNextLine()) 
		    {
		    	String data = scan.nextLine();
		        this.word_bank.add(data);
		    }
		      scan.close();
		} 
		catch (FileNotFoundException e) 
		{
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		}
	}
	
	
	public void generateAnswer()
	{
		this.answer = this.word_bank.remove(new Random().nextInt(word_bank.size())).toUpperCase();
	}
	
	public String getAnswer()
	{
		return answer;
	}
	
	public void showAnswer()
	{
		System.out.println(answer);
	}
	
	public void decrement_tries()
	{
		if( tries > 0)
		{
			tries--;
			pr.println("You have " + tries + " tries left");
		}
	}
	
	public boolean didWin() 
	{
	       return guess.equals(answer);
	}
	
	synchronized  void addPoints()
	{
		Semaphore sem = new Semaphore(1); 
		try
		{
			sem.acquire();
			switch(tries)
			{
			case 6:
				score += 10;
				break;
			case 5:
				score += 8;
				break;
			case 4:
				score += 6;
				break;
			case 3:
				score += 4;
				break;
			case 2:
				score += 2;
				break;
			case 1:
				score += 1;
				break;
			default:
				score += 0;
			}
		}
		catch (InterruptedException exc) 
		{ 
            System.out.println(exc); 
        } 
		sem.release();
		pr.println("The current score is: " + score);
	}
	
	
	/**public void addPoints()
	{
		switch(tries)
		{
		case 6:
			score += 10;
			break;
		case 5:
			score += 8;
			break;
		case 4:
			score += 6;
			break;
		case 3:
			score += 4;
			break;
		case 2:
			score += 2;
			break;
		case 1:
			score += 1;
			break;
		default:
			score += 0;
		}
		pr.println("The current score is: " + score);
	}*/
	 
	
	public void lost()
	{
		if (tries == 0)
		{
			pr.println("You Lost");
			pr.println("The Correct Answer was: " + getAnswer());
			removeGameHandler();
			addPoints();
			
		}
	}
	
	public boolean isGameOver () {
        return tries == 0 || didWin();
    }
	
	public boolean size()
	{
		return gameHandlers.size() > -1;
	}
}
