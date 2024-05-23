package fc;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.fxcore2.IO2GResponseListener;
import com.fxcore2.O2GAccountRow;
import com.fxcore2.O2GAccountsTableResponseReader;
import com.fxcore2.O2GMarketDataSnapshotResponseReader;
import com.fxcore2.O2GOfferRow;
import com.fxcore2.O2GOffersTableResponseReader;
import com.fxcore2.O2GResponse;
import com.fxcore2.O2GResponseReaderFactory;
import com.fxcore2.O2GResponseType;
import com.fxcore2.O2GSession;
import com.fxcore2.O2GTableColumn;
import com.fxcore2.O2GTableColumnCollection;

public class ResponseListener implements IO2GResponseListener {
	
	//store session
	private O2GSession session;
	// callback listeners Map
	private Map<String, ArrayList<ResponseCallback>> callbacks;
	
	

	public ResponseListener(O2GSession session) {
		//store session
		this.session = session;
		//initialize callbacks
		this.callbacks = new HashMap<String, ArrayList<ResponseCallback>>();
	
	}

	@Override
	public void onRequestCompleted(String arg0, O2GResponse response) {
//		System.out.println("Request completed "+arg0 + " ==>"+response+" type:"+response.getType());
		O2GResponseType response_type = response.getType();
		O2GResponseReaderFactory responseReaderFactory = session.getResponseReaderFactory();
		ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		switch(response_type){
			case GET_OFFERS:
				//get response data reader
				O2GOffersTableResponseReader offersTableReader = responseReaderFactory.createOffersTableReader(response);
				//get the number of offers
				int offersCount = offersTableReader.size();
				System.out.println("Offers count: "+offersCount);
				//iterate through offers
				for (int i = 0; i < offersCount; i++) {
					// get offer row
					O2GOfferRow offerRow = offersTableReader.getRow(i);
					// get offer id
					String offerId = offerRow.getOfferID();
					// get offer instrument
					String instrument = offerRow.getInstrument();
					// get offer bid
					double bid = offerRow.getBid();
					// get offer ask
					double ask = offerRow.getAsk();
					// get offer high
					double high = offerRow.getHigh();
					// get offer low
					double low = offerRow.getLow();
					// get offer time
					Calendar time = offerRow.getTime();
					// print offer
					System.out.println("Offer: " + offerId + " " + instrument + " bid: " + bid + " ask: " + ask
							+ " high: " + high + " low: " + low + " time: " + time.getTime());
				}				
				System.out.println("GET_OFFERS");
				break;
			case GET_ACCOUNTS:
				System.out.println("GET_ACCOUNTS");
				O2GAccountsTableResponseReader accountsTableReader = responseReaderFactory.createAccountsTableReader(response);
				//get the number of accounts
				int accountsCount = accountsTableReader.size();
//				System.out.println("Accounts count: "+accountsCount);
				//iterate through accounts
				for (int i = 0; i < accountsCount; i++) {
					O2GAccountRow row = accountsTableReader.getRow(i);
					O2GTableColumnCollection columns = row.getColumns();
					HashMap<String, Object> account = new HashMap<String, Object>();
					for (int j = 0; j < columns.size(); j++) {
						O2GTableColumn column = columns.get(j);
						String id = column.getId();
						//according to the id call a getter method on the row
						String methodName = "get"+id;
						Object value = null;
						try {
							value = row.getClass().getMethod(methodName).invoke(row);
//							System.out.println("Column id: "+id+" value: "+value);
							//add to account Map
							account.put(id, value);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
					}
					data.add(account);
				}
				
				notify("GET_ACCOUNTS", data);
			
				break;
//			case GET_ORDERS:
//				System.out.println("GET_ORDERS "+response);
//				//get response data reader
//				O2GOffersTableResponseReader ordersTableReader = responseReaderFactory.createOffersTableReader(response);
//				System.out.println("Orders table reader: "+ordersTableReader);
//				//get the number of orders
//				int ordersCount = ordersTableReader.size();
//				System.out.println("Orders count: "+ordersCount);
//				//iterate through orders
//				for (int i = 0; i < ordersCount; i++) {
//					// get order row
//					O2GOfferRow orderRow = ordersTableReader.getRow(i);
//					// get order id
//					String orderId = orderRow.getOfferID();
//					// get order instrument
//					String instrument = orderRow.getInstrument();
//					// get order bid
//					double bid = orderRow.getBid();
//					// get order ask
//					double ask = orderRow.getAsk();
//					// get order high
//					double high = orderRow.getHigh();
//					// get order low
//					double low = orderRow.getLow();
//					// get order time
//					Calendar time = orderRow.getTime();
//					// print order
//					System.out.println("Order: " + orderId + " " + instrument + " bid: " + bid + " ask: " + ask
//							+ " high: " + high + " low: " + low + " time: " + time.getTime());
//				}
//				break;
			case GET_CLOSED_TRADES:
				System.out.println("GET_CLOSED_TRADES");
				break;
			case MARKET_DATA_SNAPSHOT:
				//get response data
//				System.out.println("MARKET_DATA_SNAPSHOT");
				//get response factory
				O2GMarketDataSnapshotResponseReader marketDataSnapshotReader = responseReaderFactory.createMarketDataSnapshotReader(response);
				//get the number of offers
				int historyCount = marketDataSnapshotReader.size();
				System.out.println("history count: "+historyCount);
				//iterate through offers
				
				for (int i = 0; i < historyCount; i++) {
					// get bid open close high low
					double bidOpen = marketDataSnapshotReader.getBidOpen(i);
					double bidClose = marketDataSnapshotReader.getBidClose(i);
					double bidHigh = marketDataSnapshotReader.getBidHigh(i);
					double bidLow = marketDataSnapshotReader.getBidLow(i);
					// get ask open close high low
					double askOpen = marketDataSnapshotReader.getAskOpen(i);
					double askClose = marketDataSnapshotReader.getAskClose(i);
					double askHigh = marketDataSnapshotReader.getAskHigh(i);
					double askLow = marketDataSnapshotReader.getAskLow(i);
					// get bid ask
					double bid = marketDataSnapshotReader.getBid(i);
					double ask = marketDataSnapshotReader.getAsk(i);
					int volume = marketDataSnapshotReader.getVolume(i);
					Calendar date = marketDataSnapshotReader.getDate(i);
					// if date is in UTC do nothing, otherwise convert to UTC
					if (date.getTimeZone().getID() != "UTC") {
						date.setTimeZone(TimeZone.getTimeZone("UTC"));
					}
					// get Date time					
					Date time = date.getTime();
					
					// convert Date time to String in format MM.dd.yyyy HH:mm:ss
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					String timeStr = sdf.format(time);
					
					
					
					// print bid and ask OHLC candle
//					System.out.println("Date:"+time+"bidOpen: "+bidOpen+" bidClose: "+bidClose+" bidHigh: "+bidHigh+" bidLow: "+bidLow+ " volume: "+volume);
//					System.out.println("askOpen: "+askOpen+" askClose: "+askClose+" askHigh: "+askHigh+" askLow: "+askLow);
					// add to data both bid and ask
					HashMap<String, Object> candle = new HashMap<String, Object>();
					candle.put("Date", timeStr);
					candle.put("BidOpen", bidOpen);
					candle.put("BidClose", bidClose);
					candle.put("BidHigh", bidHigh);
					candle.put("BidLow", bidLow);
					candle.put("Volume", volume);
					candle.put("AskOpen", askOpen);
					candle.put("AskClose", askClose);
					candle.put("AskHigh", askHigh);
					candle.put("AskLow", askLow);
					data.add(candle);
				}
				notify("MARKET_DATA_SNAPSHOT", data);
				
				
				
				
				break;
			case COMMAND_RESPONSE:
				System.out.println("COMMAND_RESPONSE");
				break;
					
						
		}
		

	}

	@Override
	public void onRequestFailed(String arg0, String arg1) {
		System.out.println("Request failed "+arg0 + " "+arg1);

	}

	@Override
	public void onTablesUpdates(O2GResponse response) {
	}

	/**
	 * notify all callbacks for the response type
	 * 
	 * @param responseType String
	 * @param data         ArrayList<Map<String, Object>>
	 */
	public void notify(String responseType, ArrayList<Map<String, Object>> data) {
		// check if the response type is registered
		if (this.callbacks.containsKey(responseType)) {
			// get all callbacks for the response type
			ArrayList<ResponseCallback> callbacks = this.callbacks.get(responseType);
			// call all callbacks
			for (ResponseCallback callback : callbacks) {
				callback.onResponse(data);
			}
		}
	}
	
	/**
	 * set callback for specfic response type
	 * @param responseType String
	 * @param callback ResponseCallback
	 */
	public void register(String responseType, ResponseCallback callback) {
		//check if the response type is already registered
		if (this.callbacks.containsKey(responseType)) {
			// add callback to the list
			this.callbacks.get(responseType).add(callback);
		} else {
			// create a new list and add callback to the list
			ArrayList<ResponseCallback> list = new ArrayList<ResponseCallback>();
			list.add(callback);
			this.callbacks.put(responseType, list);
		}
		
	}
	
	public interface ResponseCallback {
		public void onResponse(ArrayList<Map<String, Object>> data);
	}
}
