package fc;
import com.fxcore2.IO2GSessionStatus;
import com.fxcore2.O2GSessionStatusCode;

/**
 * A class that listens for changes in the session status
 */
public class SessionStatusListener implements IO2GSessionStatus {
	private boolean mConnected;
	private boolean mDisconnected;
	private boolean mReconnecting;
	
	

	@Override
	public void onLoginFailed(String arg0) {
		System.out.println("Login failed: " + arg0);
	}

	@Override
	public void onSessionStatusChanged(O2GSessionStatusCode status) {
		System.out.println("Session status changed. New status: " + status.toString());
		
		//set the status flags
		switch (status) {
			case CONNECTED:
				mConnected = true;
				break;
			case DISCONNECTED:
				mDisconnected = true;
				break;
			case RECONNECTING:
				mReconnecting = true;
				break;
					
		}
	}
	
	public boolean isConnected() {
		return mConnected;
	}
	
	public boolean isDisconnected() {
		return mDisconnected;
	}
	
	public boolean isReconnecting() {
		return mReconnecting;
	}	

}
