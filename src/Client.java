import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;


public class Client extends UnicastRemoteObject implements ClientRMI{

	private Socket clientSocket;
	private String hostname;
	private int port;
	private int id;
	private int rmiPort;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private Scanner in;
	private boolean done;
	private ArrayList<String> myFiles;
	
	public Client(String server,String port) throws RemoteException{
		in = new Scanner(System.in);
		clientSocket = null;
		hostname = server;
		this.port=-1;
		this.rmiPort=8092;
		id=-1;
		myFiles = new ArrayList<String>();
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
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
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
//		Scanner in = new Scanner(System.in);
		result = in.nextInt();
//		in.close();
		return result;
	}
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
			Naming.rebind("//localhost:"+rmiPort+"/Client"+id,this);
			System.err.println("Client ready");
		}
		catch(Exception e)
		{
			System.err.println("Error binding RMI: ");
			e.printStackTrace();
		}
	}
	private void addFile() throws IOException{
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
//		Scanner in = new Scanner(System.in);
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("FILE_ADD" + '\n');
		outToServer.writeBytes(id + "\n");
		System.out.println("Enter the name of the file to be added: ");
		
		String fName = in.next();
		
		outToServer.writeBytes(fName + '\n');
		System.out.println("\n=============================");
		System.out.println(inFromServer.readLine());
		System.out.println("===============================");
		clientSocket.close();
		
	}
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
			System.out.println((i+1)+". "+inFromServer.readLine());
		}
		System.out.println("==========================");
		clientSocket.close();
		
	}
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
		System.out.println("ID\tIP ADDRESS");
		System.out.println("-----------------------");
		for(int i=0;i<count;i++){
			fromServer=inFromServer.readLine();
			if(Integer.parseInt(fromServer.charAt(0)+"")==this.id)
				System.out.println(fromServer+" (you)");
			else
				System.out.println(fromServer);
		}
		System.out.println("=======================");
		clientSocket.close();
	}
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
	private void fileRequest() throws IOException{
		System.out.println("Enter the name of the file to request: ");
//		Scanner in = new Scanner(System.in);
		String fName = in.next();
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
		
		String hostIP = inFromServer.readLine();
		System.out.println("Send request to " + hostIP );
		
		clientSocket.close();
	}
	/*private void checkRequest() throws IOException{
		try{
			clientSocket = new Socket(hostname,port); //Socket is created
		}
		catch(UnknownHostException e){
			System.out.println("ERR - Host not found"); //Host not found
			System.exit(0);
		}
		outToServer = new DataOutputStream(clientSocket.getOutputStream()); //Output stream to send ping to server
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Buffer for receiving the ping back from server
		outToServer.writeBytes("REQUEST_CHECK" + '\n');
		outToServer.writeBytes(id+"\n");
		String result = inFromServer.readLine();
		if(result.equals("YES")){
			System.out.println("We must send a file");
		}
	}*/
	private void sendMessage(){
		try
		{
			this.getUserList();
		} catch (IOException e1)
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
			recipient.receiveMsg("Incomming message from client #"+this.id+":\n"+msg);
		}
		catch(Exception e)
		{
			System.err.println("Error locating recipient");
			e.printStackTrace();
		}
		
		
		
	}
	@Override
	public void receiveMsg(String msg) throws RemoteException
	{
		System.out.println(msg);
		
	}
	@Override
	public byte[] requestFile(String fileName) throws RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
