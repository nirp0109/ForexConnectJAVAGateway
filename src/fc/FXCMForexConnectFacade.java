package fc;
import com.fxcore2.*;

import py4j.GatewayServer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FXCMForexConnectFacade {
	
	private Object lock = new Object();
	private FXCMForexConnectFacade instance;
	private O2GSession session;
	private SessionStatusListener listener;

	public static void main(String[] args) {
		GatewayServer gatewayServer = new GatewayServer(new FXCMForexConnectFacade());
		gatewayServer.start();		
	}
	
	public FXCMForexConnectFacade() {
		this.instance = this;
	}
	
	public void init(String userName, String password, String url, String connectionType) {
		this.session = login(userName, password, url, connectionType);
	}
	
	public void close() throws InterruptedException {
		logout(this.session);
	}
	
	public ArrayList<Map<String, Object>> getAccountInformation() throws InterruptedException, Throwable {
		return getAccountInformation(this.session);
	}
	
	public ArrayList<HashMap<String, Object>> getHistoryData(String instrument, String timeframe, String start,
			String end) throws InterruptedException {
		return getHistoryData(this.session, instrument, timeframe, start, end);
	}
	
	

	
	/**
	 * login FXCM using forexconnect
	 * @param userName
	 * @param password
	 * @param url
	 * @param connectionType
	 * @return
	 */
	public 	O2GSession login(String userName, String password, String url, String connectionType) {
		O2GSession session = O2GTransport.createSession();
        session.useTableManager(O2GTableManagerMode.YES, null);
        listener = new SessionStatusListener();
        session.subscribeSessionStatus(listener);
        try {
            session.login(userName, password, url, connectionType);
            //wait for the session to be connected
            while (listener.isConnected() == false) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Connected to the server");
            return session;
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
	
	/**
	 * logout FXCM using forexconnect
	 * @param session
	 * @return
	 * @throws InterruptedException
	 */
	public  boolean logout(O2GSession session) throws InterruptedException {
		session.logout();
        return true;
    }
	
	/**
	 * get the account information
	 * 
	 * @param session
	 * @return
	 * @throws InterruptedException
	 * @throws Throwable
	 */
	public  ArrayList<Map<String, Object>> getAccountInformation(O2GSession session) throws InterruptedException, Throwable {
		ResponseListener responseListener = new ResponseListener(session);
        session.subscribeResponse(responseListener);
        O2GRequestFactory factory = session.getRequestFactory();
		if (factory == null) {
			return new ArrayList<Map<String, Object>>();
		}
        O2GRequest getAccounts = factory.createRefreshTableRequest(O2GTableType.ACCOUNTS);
        ArrayList result = new ArrayList<ArrayList<Map<String, Object>>> ();
        //implement the callback to get the account information
		ResponseListener.ResponseCallback callback = new ResponseListener.ResponseCallback() {
			public ArrayList<Map<String, Object>> account = null;
			@Override
			public void onResponse(ArrayList<Map<String, Object>> data) {
				result.add(data);
				synchronized (lock) {
					lock.notify();
				}
			}
		};
		responseListener.register("GET_ACCOUNTS", callback);
        
        
        session.sendRequest(getAccounts);
        //wait for the response
        synchronized (lock) {
            lock.wait();
        }
        session.unsubscribeResponse(responseListener);
		if (result.size() > 0) {
			return (ArrayList<Map<String, Object>>) result.get(0);
		} else {
			return null;
        }
	}
	
	/**
	 * Get the history data of a specific instrument
	 * @param session the session with the server
	 * @param instrument the instrument
	 * @param timeframe the timeframe
	 * @param start date at format MM.DD.YYYY HH:mm:ss
	 * @param end date at format MM.DD.YYYY HH:mm:ss
	 * @return
	 * @throws InterruptedException
	 */
	public  ArrayList<HashMap<String, Object>> getHistoryData(O2GSession session, String instrument, String timeframe, String start, String end) throws InterruptedException {
		ResponseListener responseListener = new ResponseListener(session);
		session.subscribeResponse(responseListener);
		O2GRequestFactory factory = session.getRequestFactory();
		//convert the timeframe to O2GTimeframe
		O2GTimeframeCollection timeframes = factory.getTimeFrameCollection();
		O2GTimeframe timeframeObj = timeframes.get(timeframe);
		
		//convert the start and end time to Calendar by the format MM.DD.YYYY HH:mm:ss
		//convert string to date
		SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		Date startDate;
		Date endDate;
		try {
			startDate = sdf.parse(start);
			endDate = sdf.parse(end);
		} catch (ParseException e) {
			e.printStackTrace();
			//print to user that the format is not correct
			System.err.println("The format of the date is not correct. The correct format is MM.DD.YYYY HH:mm:ss");
			return null;
		}
		//convert date to calendar
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar startCalendar = Calendar.getInstance(tz);
		startCalendar.setTime(startDate);
		Calendar endCalendar = Calendar.getInstance(tz);
		endCalendar.setTime(endDate);
		
		//find the last date by the timeframe and the start date, api bring only 300 candles
		long millisecondsInTimeFrame = getMillisecondsInTimeFrame(timeframe);
		long startMilliseconds = startCalendar.getTimeInMillis();
		long endLimit = startMilliseconds + 300 * millisecondsInTimeFrame;
		
		//create calendar for the end limit
		Calendar endLimitCalendar = Calendar.getInstance(tz);
		endLimitCalendar.setTimeInMillis(endLimit);
		if (endLimitCalendar.after(endCalendar)) {
			endLimitCalendar = endCalendar;
			endLimit = endCalendar.getTimeInMillis();
		}

		ArrayList<ArrayList<Map<String, Object>>> result = new ArrayList<ArrayList<Map<String, Object>>> ();
        //implement the callback to get the account information
		ResponseListener.ResponseCallback callback = new ResponseListener.ResponseCallback() {
			public ArrayList<Map<String, Object>> account = null;
			@Override
			public void onResponse(ArrayList<Map<String, Object>> data) {
				if(data != null) {
					result.add(data);
				}
				synchronized (lock) {
					lock.notify();
				}
			}
		};
		responseListener.register("MARKET_DATA_SNAPSHOT", callback);

		
		//if the end limit is before the end date, set the end date to the end limit
		while (startCalendar.before(endCalendar) && (endLimitCalendar.before(endCalendar) || endLimitCalendar.equals(endCalendar))) {
			// create market data snapshot request
			O2GRequest marketDataSnapshotRequestInstrument = factory.createMarketDataSnapshotRequestInstrument(instrument,
					timeframeObj, 365 * 6);
			factory.fillMarketDataSnapshotRequestTime(marketDataSnapshotRequestInstrument, startCalendar, endLimitCalendar, true,
					O2GCandleOpenPriceMode.PREVIOUS_CLOSE);
			
	
			session.sendRequest(marketDataSnapshotRequestInstrument);
			// wait for the response
			synchronized (lock) {
				lock.wait();
			}

			startCalendar.setTimeInMillis(endLimit + 1000);
			// set the end limit to the next 300 candles
			endLimit += 300 * millisecondsInTimeFrame;
			if (endLimit > endCalendar.getTimeInMillis()) {
				endLimit = endCalendar.getTimeInMillis();
			}
			endLimitCalendar.setTimeInMillis(endLimit);
		}
		session.unsubscribeResponse(responseListener);
		if (result.size() > 0) {
			//merge all the data
			ArrayList<HashMap<String, Object>> finalResult = new ArrayList<HashMap<String, Object>>();
			for (Object data : result) {
				finalResult.addAll((ArrayList<HashMap<String, Object>>) data);
			}
			return finalResult;
		} else {
			return null;
		}

	}
	
	public boolean isSessionConnected() {
		if(this.listener != null) {
			return this.listener.isConnected();
		} else {
			return false;
		}
	}
	
	/**
	 * Get amount of milliseconds in a given time frame
	 * @param timeframe the time frame
	 * @return the amount of milliseconds
	 */
	public long getMillisecondsInTimeFrame(String timeframe) {
		switch (timeframe) {
			case "m1":
				return 60 * 1000;
			case "m5":
				return 5 * 60 * 1000;
			case "m15":
				return 15 * 60 * 1000;
			case "m30":
				return 30 * 60 * 1000;
			case "H1":
				return 60 * 60 * 1000;
			case "H4":
				return 4 * 60 * 60 * 1000;
			case "D1":
				return 24 * 60 * 60 * 1000;
			case "W1":
				return 7 * 24 * 60 * 60 * 1000;
			default:
				return 0;
		}
		
	}
	

}
