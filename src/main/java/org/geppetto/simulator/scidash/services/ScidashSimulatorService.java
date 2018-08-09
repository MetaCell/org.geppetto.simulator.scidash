package org.geppetto.simulator.scidash.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.PathConfiguration;
import org.geppetto.core.beans.SimulatorConfig;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.externalprocesses.ExternalProcess;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.recordings.ConvertDATToRecording;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.model.DomainModel;
import org.geppetto.model.ExperimentState;
import org.geppetto.simulator.external.services.NeuronSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Service
public class ScidashSimulatorService extends NeuronSimulatorService{

	private static Log logger = LogFactory.getLog(ScidashSimulatorService.class);

	private String uploadResultsURL = "http://ptsv2.com/t/lzyso-1533739271/post";

	@Autowired
	private SimulatorConfig scidashSimulatorConfig;
	
	Map <Long,IExperiment> runningSimulations = new ConcurrentHashMap<>();
	
	private String processToken = "";
	

	@Override
	public void initialize(DomainModel model, IAspectConfiguration aspectConfiguration, ExperimentState experimentState, ISimulatorCallbackListener listener, GeppettoModelAccess modelAccess)
			throws GeppettoInitializationException, GeppettoExecutionException
	{
		super.initialize(model, aspectConfiguration, experimentState, listener, modelAccess);
		processToken = "123"; //FIXME : Replace with actual reading from aspect configuration's model parameters
	}
	
	@Override
	public void simulate() throws GeppettoExecutionException {
		// send command, directory where execution is happening, and path to original file script to execute
		if(!started)
		{
			ExternalProcess process = new ExternalProcess(commands, directoryToExecuteFrom, originalFileName, this, outputFolder);
			process.setName("External Process");
			process.setProcessToken(processToken);
			process.start();

			this.externalProcesses.put(commands, process);
			started = true;
		}
		else
		{
			throw new GeppettoExecutionException("Simulate has been called again");
		}
	}
	
	private int sendResults(String tokenID, List<URL> results) {
		String resultsJSON = new Gson().toJson(results);
				
		JsonObject resultsPost = new JsonObject();
		resultsPost.addProperty("userToken", tokenID);
		resultsPost.addProperty("results", resultsJSON);
		
		int responseCode = 0;
		try {
			URL obj = new URL(uploadResultsURL);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(resultsPost.toString());
			wr.flush();
			wr.close();

			responseCode = con.getResponseCode();
			logger.info("\nSending 'POST' request to URL : " + uploadResultsURL);
			logger.info("Post parameters : " + resultsPost);
			logger.info("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			//print result
			logger.info(response.toString());

		} catch (Exception e) {
			logger.error("Error uploadiing results to " + this.uploadResultsURL);
			logger.error(e.getMessage());
		}
		
		return responseCode;
	}
	
	@Override
	public void processDone(String token,String[] processCommand) throws GeppettoExecutionException
	{
		super.processDone(token,processCommand);
		
		ExternalProcess process = this.getExternalProccesses().get(processCommand);
		List<URL> resultsURL = new ArrayList<URL>();

		try
		{
			List<String> variableNames = new ArrayList<String>();

			ConvertDATToRecording datConverter = new ConvertDATToRecording(PathConfiguration.createExperimentTmpPath(Scope.RUN, projectId, getExperiment().getId(), aspectConfiguration.getInstance(), PathConfiguration.getName("results", true)+ ".h5"),this.geppettoModelAccess);

			Map<File,ResultsFormat> results=new HashMap<File,ResultsFormat>();
			
			File mappingResultsFile = new File(process.getOutputFolder() + "/outputMapping.dat");
			results.put(mappingResultsFile,ResultsFormat.RAW);
			resultsURL.add(mappingResultsFile.toURL());
			
			BufferedReader input;

			input = new BufferedReader(new FileReader(mappingResultsFile));

			// read rest of DAT file and extract values
			String filePath = "";
			String line = "";
			while((line = input.readLine()) != null)
			{
				if(filePath.equals(""))
				{
					filePath = line;
				}
				else
				{
					String[] variables = line.split("\\s+");
					for(String s : variables)
					{
						variableNames.add(s);
					}
					String fileName=mappingResultsFile.getParent() + "/" + filePath;
					datConverter.addDATFile(fileName, variables);
					File newFile = new File(fileName);
					results.put(newFile,ResultsFormat.RAW);
					resultsURL.add(newFile.toURL());
					filePath = "";
				}
			}
			input.close();
			
		}
		catch(Exception e)
		{
			//The HDF5 library throws a generic Exception :/
			throw new GeppettoExecutionException(e);
		}
		
		this.sendResults(token,resultsURL);
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
	 * @param neuronSimulatorConfig
	 * @deprecated for test purposes only, the configuration is autowired
	 */
	public void setScidashSimulatorConfig(SimulatorConfig scidashSimulatorConfig)
	{
		this.scidashSimulatorConfig = scidashSimulatorConfig;
	}
}
