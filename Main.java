package com.local.ga;

import java.io.IOException;

import org.apache.log4j.Logger;

import Exceptions.ArgumentException;

import com.local.ga.formatting.FormatterFactory;
import com.local.ga.formatting.IFormatter;
import com.local.ga.io.FileWriter;

public class Main {
	static Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) {
		ArgumentsManager argumentsManager = new ArgumentsManager(args);
		System.out.print("AH");
		String viewId = "";
		String configFile = "";
		logger.debug("Execution started");
		try {
			viewId = argumentsManager
					.getArgValue(ArgumentsManager.Argument.viewId);
			configFile = argumentsManager
					.getArgValue(ArgumentsManager.Argument.config);
			logger.debug("Commandline arguments parsed");
		} catch (ArgumentException e) {
			logger.fatal(String.format("Failed to parse commandline argument: %s", e.getMessage()));
			e.printStackTrace();
			System.exit(5);
		}

		ConfigManager configManager = new ConfigManager(configFile);
		try {
			configManager.loadConfig();
			logger.debug("Configuration loaded from file: " + configFile);
		} catch (ConfigurationException e) {
			logger.fatal("Failed to load configuration file: " + e.getMessage());
			e.printStackTrace();
			System.exit(10);
		}

		try {
			GALoader gaLoader = new GALoader(configManager, viewId);
			QueryResult queryResult = gaLoader.ExecuteQuery();

			if (queryResult.isSuccessful()) {
				logger.debug(String.format("Successfully loaded %s rows from GA", queryResult.getRows().size()));

				IFormatter formatter = FormatterFactory.getFormatter(
						configManager.getOutputFormat(), queryResult);
				FileWriter writer = new FileWriter(configManager);
				writer.Write(formatter.getFormattedString());

				logger.debug(String.format("Successfully saved data in file %s", writer.getFileName()));
			}

		} catch (IOException e) {
			logger.fatal("Fatal error occured when fetching data: "
					+ e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			logger.fatal("Fatal error occured: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		logger.debug("Execution ended");

	}

}
