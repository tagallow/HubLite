import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
//import java.util.Iterator;
import java.util.Queue;


public class Tracker {

	
	public static void main(String[] args) throws IOException {
		Map<Integer,Host> hosts = new HashMap<Integer,Host>();
		Map<String,Integer> files = new HashMap<String,Integer>();
		Queue<Request> requests = new LinkedList<Request>();
		int port=0;
		try{
			port = Integer.parseInt(args[0]);
			System.out.println("Tracker Ready");
		}
		catch(Exception e){
			System.out.println("Err - Invalid Port");
			System.exit(0);
		}
		
		
		ServerSocket welcomeSocket = new ServerSocket(port);
		String clientSentence;
		int nextID=0;
		while(true)
		{
			Socket connectionSocket = welcomeSocket.accept();
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			clientSentence = inFromClient.readLine();
//			System.out.println("here");
			if(clientSentence.equals("NEW_CLIENT")){
				nextID++;
				System.out.println("New client found at "+ connectionSocket.getInetAddress().toString().substring(1));
				Host newHost = new Host(connectionSocket.getInetAddress().toString().substring(1),nextID);
				hosts.put(nextID, newHost);
				outToClient.writeBytes(nextID+"\n");
				System.out.println("Assigned ID number "+nextID);
				
				
			}
			else if(clientSentence.equals("FILE_REQUEST")){
				int id = Integer.parseInt(inFromClient.readLine());
				String fileName = inFromClient.readLine();
				int hostID = files.get(fileName);
				outToClient.writeBytes(hostID +"\n");
				System.out.println("Client "+id +" is getting file "+fileName+" from client "+hostID);
			}
			else if(clientSentence.equals("FILE_ADD")){
				int id = Integer.parseInt(inFromClient.readLine());
				String fileName = inFromClient.readLine();
				Host temp = hosts.get(id);
				if(files.containsKey(fileName))
				{
					System.out.println("Server already has this file");
					outToClient.writeBytes("Server already has that file\n");
				}
				else
				{
					temp.addFile(fileName);
					files.put(fileName, id);
					System.out.println("File "+fileName + " added to host ID " +id+" at " + temp.getAddress());
					outToClient.writeBytes("File "+ fileName + " added\n");
				}
			}
			else if(clientSentence.equals("FILE_LIST")){
//				String toClient = "";
				outToClient.writeBytes(files.size()+"\n");
				for(String key : files.keySet()){
					//toClient+=key+"\n";
					try{
						
						outToClient.writeBytes(key+"\n");
					}
					catch(Exception e){
						System.out.println("error writting to buffer: "+key+"\n");
					}
				}
//				outToClient.writeBytes(toClient);
				System.out.println("File list sent to client");
				
			}
			else if(clientSentence.equals("USER_LIST")){
				outToClient.writeBytes(hosts.size()+"\n");
				for(Integer key : hosts.keySet()){
					outToClient.writeBytes(key+".\t"+hosts.get(key).getAddress()+"\n");
				}
				System.out.println("Host list sent to client");
			}
			else if(clientSentence.equals("DISCONNECT")){
				int id = Integer.parseInt(inFromClient.readLine());
				Host temp = hosts.get(id);
				for(String file : temp.getFileList()){
					files.remove(file);
				}
				hosts.remove(id);
				System.out.println("Host "+id+" and all associated files removed");
			}
			else if(clientSentence.equals("REQUEST_CHECK")){
				int id = Integer.parseInt(inFromClient.readLine());
				if(requests.peek().sender.getID()==id){
					outToClient.writeBytes("YES\n");
				}
				else{
					outToClient.writeBytes("NO\n");
				}
			}
			
			else{
				System.out.println("Invalid Request: "+clientSentence);
			}
			
		}

	}

}

class Host
{
	private String address;
//	private int port;
	private ArrayList<String> files;
	double upload;
	double download;
	int id;
	
	public Host(String address,int id){
		this.address=address;
		//this.port=port;
		this.id=id;
		upload=0;
		download=0;
		files = new ArrayList<String>();
	}
	public void addFile(String fName){
		files.add(fName);
	}
	
	public boolean equals(Object o){
		boolean result=false;
		Host h = (Host)o;
		if(h.id==this.id)
			result=true;
		//else if(h.address.equals(this.address) && h.)
		return result;
	}
	public int getID(){
		return id;
	}
	public String getAddress(){
		return address;
	}
	public ArrayList<String> getFileList(){
		return files;
	}
	public String toString(){
		String result = "Host: "+address;
		result+="\nID: "+id;
		result+="\nFiles:\n";
		for(int i=0;i<files.size();i++){
			result+=(i+1)+". "+files.get(i)+"\n";
		}
		
		return result;
	}
}
class Request{
	public Host sender;
	public Host dest;
	public String file;
	public Request(Host sender,Host dest,String file){
		this.sender=sender;
		this.dest=dest;
		this.file=file;
	}
}