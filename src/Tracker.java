import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
/**
 * A tracker class to keep track of several Clients with files.
 *
 */
public class Tracker {

	/**
	 * @param args - 
	 * 	[0] The port number to broadcast on
	 */
	public static void main(String[] args) throws IOException {
		Map<Integer,Host> hosts = new HashMap<Integer,Host>();
		Map<String,Integer> files = new HashMap<String,Integer>();
		Map<String,Integer> fileSizes = new HashMap<String,Integer>();
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
			if(clientSentence.equals("NEW_CLIENT")){
				nextID++;
				System.out.println("New client found at "+ connectionSocket.getInetAddress().toString().substring(1));
				Host newHost = new Host(connectionSocket.getInetAddress().toString().substring(1),nextID);
				hosts.put(nextID, newHost);
				outToClient.writeBytes(nextID+"\n");
				System.out.println("Assigned ID number "+nextID);
			}
			else if(clientSentence.equals("FILE_REQUEST")){
				try{
					int id = Integer.parseInt(inFromClient.readLine());
					String fileName = inFromClient.readLine();
					int hostID = files.get(fileName);
					outToClient.writeBytes(hostID +"\n");
					System.out.println("Client "+id +" is getting file "+fileName+" from client "+hostID);
					
					int fileSize = fileSizes.get(fileName);
					
					hosts.get(id).download+=fileSize;
					hosts.get(hostID).upload+=fileSize;
				}
				catch(Exception e){
					outToClient.writeBytes("Error\n");
					System.out.println("Invalid Request\n");
				}
			}
			else if(clientSentence.equals("FILE_ADD")){
				int id = Integer.parseInt(inFromClient.readLine());
				String fileName = inFromClient.readLine();
				int fileSize = Integer.parseInt(inFromClient.readLine());
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
					fileSizes.put(fileName, fileSize);
					System.out.println("File "+fileName + " added to host ID " +id+" at " + temp.getAddress());
					outToClient.writeBytes("File "+ fileName + " added\n");
				}
			}
			else if(clientSentence.equals("FILE_LIST")){
				outToClient.writeBytes(files.size()+"\n");
				String s="";
				for(String key : files.keySet()){
					s=String.format("%10s%15s", key,processSize(fileSizes.get(key)));
					try{
						outToClient.writeBytes(s+"\n");
					}
					catch(Exception e){
						System.out.println("error writting to buffer: "+key+"\n");
					}
				}
				System.out.println("File list sent to client");
				
			}
			else if(clientSentence.equals("USER_LIST")){
				outToClient.writeBytes(hosts.size()+"\n");
				for(Integer key : hosts.keySet()){
					outToClient.writeBytes(hosts.get(key).toString()+'\n');
				}
				System.out.println("Host list sent to client");
			}
			else if(clientSentence.equals("DISCONNECT")){
				int id = Integer.parseInt(inFromClient.readLine());
				Host temp = hosts.get(id);
				for(String file : temp.getFileList()){
					files.remove(file);
					fileSizes.remove(file);
				}
				hosts.remove(id);
				System.out.println("Host "+id+" and all associated files removed");
			}
			
			else{
				System.out.println("Invalid Request: "+clientSentence);
			}
			
		}

	}
	public static String processSize(int size){
		String result="";
		DecimalFormat df = new DecimalFormat("###,###.##");
		double tempDL=size;
		if(size<1024){
			result = df.format(size) +" b";
		}
		else if(size < 1048576){
			tempDL/=1024;
			result = df.format(tempDL) +" kB";
		}
		else{
			tempDL/=1048576;
			result = df.format(tempDL) +" MB";
		}
		
		return result;
	}

}
/**
 * A Host class to keep track of a client, and what files they have
 */
class Host
{
	private String address;
	private ArrayList<String> files;
	double upload;
	double download;
	int id;
	/*
	 * A constructor to accept the IP and port number
	 */
	public Host(String address,int id){
		this.address=address;
		//this.port=port;
		this.id=id;
		upload=0;
		download=0;
		files = new ArrayList<String>();
	}
	/*
	 * adds a file to the list
	 */
	public void addFile(String fName){
		files.add(fName);
	}
	/*
	 * Checks if two hosts are the same
	 */
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
	/*
	 * Calculates the ratio of data uploaded/downloaded and 
	 * returns it as a nicely formatted string
	 */
	public String getRatio(){
		DecimalFormat df = new DecimalFormat("#,###,###.##");
		
		String result = "inf";
		if(download>.1){
			result = df.format(upload/download);
		}
		return result;
	}
	/*
	 * Returns the amount of data this host has downloaded as a nicely
	 * formatted string with the appropriate units
	 */
	public String getDownload(){
		String result="";
		DecimalFormat df = new DecimalFormat("###,###.##");
		double tempDL=download;
		if(download<1024){
			result = df.format(download) +" b";
		}
		else if(download < 1048576){
			tempDL/=1024;
			result = df.format(tempDL) +" kB";
		}
		else{
			tempDL/=1048576;
			result = df.format(tempDL) +" MB";
		}
		
		return result;
		
	}
	/*
	 * Returns the amount of data this host has uploaded as a nicely
	 * formatted string with the appropriate units
	 */
	public String getUpload(){
		String result="";
		DecimalFormat df = new DecimalFormat("###,###.##");
		double tempUpload=upload;
		if(upload<1024){
			result = df.format(tempUpload) +" b";
		}
		else if(upload < 1048576){
			tempUpload/=1024;
			result = df.format(tempUpload) +" kB";
		}
		else{
			tempUpload/=1048576;
			result = df.format(tempUpload) +" MB";
		}
		
		return result;
	}
	public String toString(){
//		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter();
		String result=f.format("%2d%17s%18s%18s%13s",id,address,getUpload(),getDownload(),getRatio()).toString();
//		String result = id+".\t"+address+"\t\t"+getUpload()+"\t\t"+getDownload()+"\t\t"+this.getRatio();
		System.out.println(result);
		return result;
	}
}
