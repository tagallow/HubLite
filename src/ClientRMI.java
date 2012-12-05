import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ClientRMI extends Remote
{
	void receiveMsg(int id,String msg) throws RemoteException;
	
	byte[] requestFile(String fileName,int requesterID) throws RemoteException;
	
	boolean transferComplete() throws RemoteException;
	
	int getFileSize(String fName) throws RemoteException;
}
