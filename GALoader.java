package com.local.ga;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.GaData;

public class GALoader {

	static Logger logger = Logger.getLogger(Main.class.getName());
	private static final String APPLICATION_NAME = "local-ga";
	private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), "/local-ga");
	private static FileDataStoreFactory dataStoreFactory;
	private static HttpTransport httpTransport;
	
	private static int MAX_ROWS_PER_REQUEST = 10000;
	
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();
	
	private String viewId;
	
	private ConfigManager configManager;
	
	public GALoader(ConfigManager configManager, String viewId) throws IOException, Exception{
		this.configManager = configManager;
		this.viewId = viewId;
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
	}
	
	public QueryResult ExecuteQuery() throws Exception{
		Analytics analytics = initializeAnalytics();
		return executeDataQuery(analytics, this.viewId);
	}
	
	/** Authorizes the installed application to access user's protected data. */
	private static Credential authorize() throws Exception {
		// load client secrets
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY,
				new InputStreamReader(Main.class
						.getResourceAsStream("/client_secrets.json")));
		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret()
						.startsWith("Enter ")) {
			System.out
					.println("Enter Client ID and Secret from https://code.google.com/apis/console/?api=analytics "
							+ "into analytics-cmdline-sample/src/main/resources/client_secrets.json");
			System.exit(1);
		}
		// set up authorization code flow
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, JSON_FACTORY, clientSecrets,
				Collections.singleton(AnalyticsScopes.ANALYTICS_READONLY))
				.setDataStoreFactory(dataStoreFactory).build();
		// authorize
		return new AuthorizationCodeInstalledApp(flow,
				new LocalServerReceiver()).authorize("user");
	}

	/**
	 * Performs all necessary setup steps for running requests against the API.
	 * 
	 * @return An initialized Analytics service object.
	 * 
	 * @throws Exception
	 *             if an issue occurs with OAuth2Native authorize.
	 */
	private Analytics initializeAnalytics() throws Exception {
		// Authorization.
		Credential credential = authorize();

		// Set up and return Google Analytics API client.
		return new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME).build();
	}


	/**
	 * Returns the top 25 organic search keywords and traffic source by visits.
	 * The Core Reporting API is used to retrieve this data.
	 * 
	 * @param analytics
	 *            the analytics service object used to access the API.
	 * @param profileId
	 *            the profile ID from which to retrieve data.
	 * @return the response from the API.
	 * @throws IOException
	 *             tf an API error occured.
	 */
	private QueryResult executeDataQuery(Analytics analytics, String profileId)
			throws IOException {
		logger.debug("ViewID (profileID) :" + profileId);
		QueryResult queryResult = new QueryResult();
		
		GaData gaData = null;
		
		String dimensions = configManager.getDimensions();
		String sorts = configManager.getSorts();
		String filters = configManager.getFilters();
		
		int totalRowsFetched = 0;
		int startIndex = 1;
		
		do {
			Get get = analytics.data().ga()
					.get("ga:" + profileId, // Table Id. ga: + profile id.
							configManager.getStartDate(), // Start date.
							configManager.getEndDate(), // End date.
							configManager.getMetrics());// Metrics
			
			if ( !dimensions.equals("")){
				get.setDimensions(dimensions);
			}
			if (!sorts.equals("")){
				get.setSort(sorts);
			}
			if (!filters.equals("")){
				get.setFilters(filters);
			}
			get.setMaxResults(MAX_ROWS_PER_REQUEST);
			get.setStartIndex(startIndex);
			
			gaData = get.execute();

			startIndex += MAX_ROWS_PER_REQUEST;
			if ( gaData.getTotalResults() > 0){
				totalRowsFetched += gaData.getRows().size();
			} else {
				gaData.setRows(new ArrayList<List<String>>());
			}
			
			queryResult.addGaData(gaData);
			
		} while ( totalRowsFetched < gaData.getTotalResults());
		
		return queryResult;
	}

}
