import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ClientRMI extends Remote
{
	void receiveMsg(String msg) throws RemoteException;
	
	byte[] requestFile(String fileName) throws RemoteException;
}
