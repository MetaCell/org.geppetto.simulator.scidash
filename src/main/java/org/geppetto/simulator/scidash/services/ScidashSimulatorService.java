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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.PathConfiguration;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IParameter;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.externalprocesses.ExternalProcess;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.recordings.ConvertDATToRecording;
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

	private static Log logger = LogFactory.getLog(ScidashSimulatorService.class);

	@Autowired
	private ScidashSimulatorConfig scidashSimulatorConfig;
	
	Map <Long,IExperiment> runningSimulations = new ConcurrentHashMap<>();
	
	private String processToken = "";
	

	@Override
	public void initialize(DomainModel model, IAspectConfiguration aspectConfiguration, ExperimentState experimentState, ISimulatorCallbackListener listener, GeppettoModelAccess modelAccess)
			throws GeppettoInitializationException, GeppettoExecutionException
	{
		super.initialize(model, aspectConfiguration, experimentState, listener, modelAccess);
		processToken = getProcessToken(aspectConfiguration); //FIXME : Replace with actual reading from aspect configuration's model parameters
	}
	
	public String getProcessToken(IAspectConfiguration ac) {
		Random rand = new Random();
		int  n = rand.nextInt(50) + 1;
		String token = String.valueOf(n); //FIXME : Replace with actual reading from aspect configuration's model parameters
		if(ac.getModelParameter()!=null) {
			for(IParameter param : ac.getModelParameter()) {
				if(param.getVariable().equals("processToken")) {
					token = param.getValue();
				}
			}
		}
		
		return token;
	}
	
	private int sendResults(List<URL> results) {
				
		String resultsJSON = new Gson().toJson(results);
				
		JsonObject resultsPost = new JsonObject();
		resultsPost.addProperty("userToken", processToken);
		resultsPost.addProperty("results", resultsJSON);
		
		int responseCode = 0;
		try {
			URL obj = new URL(scidashSimulatorConfig.getServerURL());
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
			logger.info("\nSending 'POST' request to URL : " + scidashSimulatorConfig.getServerURL());
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
			logger.error("Error uploadiing results to " + this.scidashSimulatorConfig.getServerURL());
			logger.error(e.getMessage());
		}
		
		return responseCode;
	}
	
	@Override
	public void processDone(String[] processCommand) throws GeppettoExecutionException
	{
		super.processDone(processCommand);
		
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
		
		this.sendResults(resultsURL);
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
	public void setScidashSimulatorConfig(ScidashSimulatorConfig scidashSimulatorConfig)
	{
		this.scidashSimulatorConfig = scidashSimulatorConfig;
	}
}
