package org.geppetto.simulator.scidash.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.PathConfiguration;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoHTTPClient;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.model.DomainModel;
import org.geppetto.model.ExperimentState;
import org.geppetto.simulator.external.services.NeuronSimulatorService;
import org.geppetto.simulator.scidash.config.ScidashSimulatorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Service
public class ScidashSimulatorService extends NeuronSimulatorService{

	private static final String EXPERIMENT_RESULTS = "experiment_results";
	private static final String EXPERIMENT_Error = "experiment_error";
	private static final String EXPERIMENT_NAME = "experiment_name";
	private static final String SCORE_ID = "scoreID";

	private static Log logger = LogFactory.getLog(ScidashSimulatorService.class);

	@Autowired
	private ScidashSimulatorConfig scidashSimulatorConfig;

	//Identifier to match simulation to where request is coming from
	private String processToken = "";

	@Override
	public void initialize(DomainModel model, IAspectConfiguration aspectConfiguration, ExperimentState experimentState, ISimulatorCallbackListener listener, GeppettoModelAccess modelAccess)
			throws GeppettoInitializationException, GeppettoExecutionException
	{
		super.initialize(model, aspectConfiguration, experimentState, listener, modelAccess);
		processToken = getProcessToken(aspectConfiguration);
	}

	/**
	 * Retrieve score ID from aspect configuration parameters
	 */
	public String getProcessToken(IAspectConfiguration ac) {
		Iterator it = ac.getSimulatorConfiguration().getParameters().entrySet().iterator();
		String token = null;
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			if(pair.getKey().equals(SCORE_ID)) {
				token = (String) pair.getValue();
			}
			it.remove(); // avoids a ConcurrentModificationException
		}

		return token;
	}

	/**
	 * Post results to a server
	 */
	private String sendResults(List<URL> results) {		
		//Create JSON object with results list
		String resultsJSON = new Gson().toJson(results);		
		JsonObject resultsPost = new JsonObject();
		resultsPost.addProperty(SCORE_ID, processToken);
		resultsPost.addProperty(EXPERIMENT_NAME, this.getExperiment().getName());
		resultsPost.addProperty(EXPERIMENT_RESULTS, resultsJSON);

		String response = "";
		//HTTP Post request happens here
		try {
			response =
					GeppettoHTTPClient.doJSONPost(scidashSimulatorConfig.getServerURL(), resultsPost.toString());
			logger.info(response);			

		} catch (Exception e) {
			logger.error("Error uploadiing results to " + this.scidashSimulatorConfig.getServerURL());
			logger.error(e.getMessage());
		}

		return response;
	}

	@Override
	public void processDone(String[] processCommand) throws GeppettoExecutionException
	{
		super.processDone(processCommand);

		try {
			// create URL list with location of simulation results files
			Iterator resultsMap = this.getResults().entrySet().iterator();
			List<URL> resultsURL = new ArrayList<URL>();
			while(resultsMap.hasNext()) {
				Map.Entry pair = (Map.Entry)resultsMap.next();
				File file = (File) pair.getKey();
				resultsURL.add(file.toURL());
			}
			//upload results to server
			this.sendResults(resultsURL);
		} catch (MalformedURLException e) {
			throw new GeppettoExecutionException(e);
		}
	}

	@Override
	public String getName() {
		return this.scidashSimulatorConfig.getSimulatorName();
	}

	@Override
	public String getId() {
		return this.scidashSimulatorConfig.getSimulatorID();
	}

	/**
	 * @deprecated for test purposes only
	 */
	public void deleteFiles(long projectID) throws IOException {
		PathConfiguration.deleteProjectTmpFolder(Scope.RUN, projectID);
	}

	/**
	 * @param neuronSimulatorConfig
	 * @deprecated for test purposes only
	 */
	public void setScidashSimulatorConfig(ScidashSimulatorConfig scidashSimulatorConfig)
	{
		this.scidashSimulatorConfig = scidashSimulatorConfig;
	}
	
	@Override
	public void processFailed(String errorMessage, Exception e){
		String error = new Gson().toJson(e);		
		JsonObject resultsPost = new JsonObject();
		resultsPost.addProperty(SCORE_ID, processToken);
		resultsPost.addProperty(EXPERIMENT_NAME, this.getExperiment().getName());
		resultsPost.addProperty(EXPERIMENT_RESULTS, error);

		String response = "";
		//HTTP Post request happens here
		try {
			response =
					GeppettoHTTPClient.doJSONPost(scidashSimulatorConfig.getServerURL(), resultsPost.toString());
			logger.info(response);			

		} catch (Exception e1) {
			logger.error("Error uploadiing results to " + this.scidashSimulatorConfig.getServerURL());
			logger.error(e1.getMessage());
		}
	}
}
