import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This is a Client class used to transfer files and send messages
 * to other users, while connected to a mutual tracker.
 */
public class Client extends UnicastRemoteObject implements ClientRMI{

	private Socket clientSocket; 	//The socket used to connect to the tracker
	private String hostname; 		//hostname of the tracker, passed in as a command line argument
	private int port; 				//port # the tracker is broadcasting on
	private int id; 					//ID number assigned by the tracker
	private int rmiPort;				//Port number to broadcast RMI
	private DataOutputStream outToServer; 	//Used to connect to tracker
	private BufferedReader inFromServer;	//Used to read data from tracker
	private Scanner in;							//User input
	private boolean done;						//Flagged when the user decides to quit
	private ArrayList<String> myFiles;		//ArrayList of filenames added by user
	
	
//	private ByteArrayOutputStream arrayOutput;
	private InputStream currentFileReader;		//Reads bytes from a file to send to another client
	private boolean transferComplete;			//Boolean to mark when transfer is complete

	/**
	 * Constructor for the Client class.
	 *
	 * @param server - IP address of the tracker
	 * @param port - Port number tracker is broadcasting on
	 */
	public Client(String server,String port) throws RemoteException{
		in = new Scanner(System.in);
		clientSocket = null;
		hostname = server;
		this.port=-1;
		this.rmiPort=8092;
		id=-1;
		myFiles = new ArrayList<String>();
		currentFileReader = null;
		
		try{
			this.port = Integer.parseInt(port);
		}
		catch(Exception e){
			System.out.println("Err - Invalid Port");
			System.exit(0);
		}
	}
	/**
	 * @param args
	 * 	args[0] - Tracker IP address
	 * 	args[1] - Tracker port #
	 *
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if(args.length != 2)
		{
			System.out.println("Usage: java Client [TRACKER_IP] [TRACKER PORT]");
			return;
		}
		Client client = new Client(args[0],args[1]);
		
		client.initialize();
		int userChoice;
		client.done=false;
		while(!client.done){
			userChoice = client.menu();
			switch(userChoice){
			case 1:
				client.addFile();
				break;
			case 2:
				client.getFileList();
				break;
			case 3:
				client.fileRequest();
				break;
			case 4:
				client.getUserList();
				break;
			case 5:
				client.sendMessage();
				break;
			case 0:
				client.disconnect();
				break;
			default:
				System.out.println("Invalid option");
				break;
			}
		}
	}
	/*
	 * Prints the menu and gets the users's input
	 */
	private int menu(){
		int result = -1;
		
		System.out.println(
				"\nSelect an option: \n"+
				"1. Add a file to share \n"+
				"2. Request the list of files available \n"+
				"3. Download a file \n"+
				"4. Request the list of users online \n"+
				"5. Send a message to a user\n"+
				"0. Exit\n"
				);
		try{
			result = in.nextInt();
		}
		catch(Exception e){
			result=-1;
			in.nextLine();
		}
		return result;
	}
	/*
	 * Initializes the TCP connection, gets the clients ID and registers the RMI 
	 */
	private void initialize() throws IOException{
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("NEW_CLIENT" + '\n');
		id = Integer.parseInt(inFromServer.readLine());
		System.out.println("Assigned ID number "+id);
		clientSocket.close();
		try
		{
			//The RMI name to be bound is simply the word 'Cleint' with the ID appended at the end.
			//This makes it easy to connect to other clients without needing to mess with IP addresses.
			Naming.rebind("//localhost:"+rmiPort+"/Client"+id,this);
			System.err.println("Client ready");
		}
		catch(Exception e)
		{
			System.err.println("Error binding RMI: ");
			e.printStackTrace();
		}
	}
	/*
	 * Adds a file to the tracker
	 */
	private void addFile() throws IOException{
		System.out.println("Enter the name of the file to be added: ");
		String fName = in.next();
		File temp = new File(fName);
		if(!temp.exists())
		{
			System.out.println("\n==============");
			System.out.println("File not found");
			System.out.println("==============");
			return;
		}
		
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("FILE_ADD" + '\n');
		
		outToServer.writeBytes(id + "\n");
		outToServer.writeBytes(fName + '\n');
		outToServer.writeBytes(temp.length() + "\n");
		String servermsg = inFromServer.readLine();
		System.out.println("\n=============================");
		System.out.println(servermsg);
		System.out.println("=============================");
		clientSocket.close();
		if(!servermsg.equals("Server already has that file"))
			myFiles.add(fName);
		
	}
	/*
	 * Requests a list of available files from the tracker
	 */
	private void getFileList() throws IOException{
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("FILE_LIST" + '\n');
		int count = Integer.parseInt(inFromServer.readLine());
		System.out.println("==========================");
		System.out.println("Files Available on server:");
		System.out.println("==========================");
		for(int i=0;i<count;i++){
			System.out.println(inFromServer.readLine());
		}
		System.out.println("==========================");
		clientSocket.close();
		
	}
	/*
	 * Gets a list of connected users from the tracker
	 */
	private void getUserList() throws IOException{
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("USER_LIST" + '\n');
		int count = Integer.parseInt(inFromServer.readLine());
		String fromServer;
		System.out.println("=======================");
		System.out.println("Users currently online:");
		System.out.println("=======================");
		System.out.format("%2s%17s%18s%18s%13s","ID","IP ADDRESS","UPLOAD","DOWNLOAD","RATIO");
		System.out.println();
		System.out.format("%2s%17s%18s%18s%13s","--","----------","------","--------","-----");
		System.out.println();
		int tempID;
		for(int i=0;i<count;i++){
			fromServer=inFromServer.readLine();
			try{
				tempID=Integer.parseInt(fromServer.substring(0, 2));
			}
			catch(NumberFormatException nfe){
				tempID=Integer.parseInt(fromServer.substring(1, 2));
			}
			if(tempID==this.id)
				System.out.println(fromServer+" (you)");
			else
				System.out.println(fromServer);
		}
		System.out.println("=======================");
		clientSocket.close();
	}
	/*
	 * Tells the tracker that this client is going offline
	 */
	private void disconnect() throws IOException{
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("DISCONNECT" + '\n');
		outToServer.writeBytes(id+"\n");
		clientSocket.close();
		done=true;
		
		try
		{
			Naming.unbind("//localhost:"+rmiPort+"/Client"+id);
			System.err.println("RMI Unbinded");
		}
		catch(Exception e)
		{
			System.err.println("Error unbinding RMI");
//			e.printStackTrace();
		}
		System.exit(0);
	}
	/*
	 * Tells the tracker what file we want, and the tracker will respond with where that file is located.
	 * The program will then use RMI to obtain the file directly from that client.
	 */
	private void fileRequest() throws IOException{
		this.getFileList();
		System.out.println("Enter the name of the file to request: ");
		String fName = in.next();
		if(this.myFiles.contains(fName)){
			System.out.println();
			System.out.println("==========================");
			System.out.println("You are hosting that file.");
			System.out.println("==========================");
			return;
		}
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("FILE_REQUEST" + '\n');
		outToServer.writeBytes(id+"\n");
		outToServer.writeBytes(fName+"\n");
		
		String hostID = inFromServer.readLine();
		if(hostID.equals("Error")){
			System.out.println("\n====================");
			System.out.println("Error recieving file");
			System.out.println("====================");
			clientSocket.close();
			return;
		}
		
		clientSocket.close();
		OutputStream newFile = null;
		ClientRMI fileHost=null;
		try
		{
			//Getting the remote object
			fileHost = (ClientRMI) Naming.lookup("//localhost:"+rmiPort+"/Client"+hostID);
		}
		catch(Exception e)
		{
			System.err.println("Error locating recipient");
			e.printStackTrace();
		}
		System.out.println("What would you like the file to be saved as?");
		String outFileName = in.next();
		System.out.println("Writing to file "+outFileName);
		
		newFile = new BufferedOutputStream(new FileOutputStream(new File(outFileName)));
		
		int recievedBytes=0;
		int size = fileHost.getFileSize(fName);
		byte[] chunk;
		
		//Continue to grab byte arrays from the remote object until the file is complete
		while(recievedBytes!=size || !fileHost.transferComplete()){
			chunk=fileHost.requestFile(fName, this.id);
			recievedBytes+=chunk.length;
			newFile.write(chunk);
		}
		newFile.close();
		System.out.println("File recieved");
	}
	/*
	 * Uses RMI to send a message to another client.
	 */
	private void sendMessage(){
		try
		{
			this.getUserList();
		} 
		catch (IOException e1)
		{
			System.out.println("Error getting user list");
		}
		System.out.println("What user would you like to send a message to? (enter their ID number)");
		int remoteId = in.nextInt();
		System.out.println("What is your message?");
		in.nextLine();
		String msg = in.nextLine();
		ClientRMI recipient;
		try
		{
			recipient = (ClientRMI) Naming.lookup("//localhost:"+rmiPort+"/Client"+remoteId);
			recipient.receiveMsg(this.id,msg);
			System.out.println("============");
			System.out.println("Message sent");
			System.out.println("============");
		}
		catch(Exception e)
		{
			System.err.println("Error locating recipient");
			e.printStackTrace();
		}
	}
	
	@Override
	/**
	 * This method is to be called remotely by other clients to recieve messages from them.
	 *
	 * @param senderID - The ID number of the sender
	 * @param msg - The single line message to be printed to the console
	 */
	public void receiveMsg(int senderID,String msg) throws RemoteException
	{
		System.out.println();
		System.out.println("=====================");
		System.out.println("Message from user #"+senderID);
		System.out.println("---------------------");
		
		System.out.println(msg);
		
		System.out.println("=====================");
	}
	/**
	 * Called remotely by other clients to let them know how big the file is before the start recieving it.
	 *
	 * @param  fName - The name of the file
	 * @return - The size of the file
	 */
	public int getFileSize(String fName){
		File temp = new File(fName);
		
		return (int)temp.length();
	}
	
	@Override
	/**
	 * Lets a remote client know whether or not their transfer is done being sent.
	 *
	 * This is just a double check since they already know the size of the file.
	 *
	 * @return - Whether or not that tranfer is done
	 */
	public boolean transferComplete() throws RemoteException{
		return this.transferComplete;
	}
	@Override
	/**
	 * Send a remote client a chunk of a file.
	 *
	 * This is called in repition until each byte of the file is completed.
	 *
	 * @param fileName - the name of the file to be transfered
	 * @param requesterID - the ID number of the client sending the request.
	 */
	public byte[] requestFile(String fileName,int requesterID) throws RemoteException
	{
		this.transferComplete=false;
		if(!this.myFiles.contains(fileName))
			return "File not found".getBytes();
		
		int bytesRead=0;
		ByteArrayOutputStream byteGenerator = new ByteArrayOutputStream();
		
		byte[] chunk = new byte[32*1024];
		
		if(this.currentFileReader==null){
			try
			{
				this.currentFileReader=new BufferedInputStream(new FileInputStream(new File(fileName)));
				
			} catch (FileNotFoundException e)
			{
				System.out.println("File not found");
				return  "File not found".getBytes();
			}
		}
		
		try
		{
			bytesRead = this.currentFileReader.read(chunk);
			
		} catch (IOException e)
		{ 
			System.out.println("Error reading file");
			return "error reading file".getBytes();
		}
		if(bytesRead>0)
		{
			byteGenerator.write(chunk,0,bytesRead);
		}
		if(bytesRead==-1){
			this.transferComplete=true;
			try
			{
				currentFileReader.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			this.currentFileReader=null;
			System.out.println("File "+fileName+" sent to user #"+requesterID);
		}
		return byteGenerator.toByteArray();
	}
}
