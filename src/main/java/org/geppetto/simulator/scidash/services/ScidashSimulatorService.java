package org.geppetto.simulator.scidash.services;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.SimulatorConfig;
import org.geppetto.core.common.GeppettoAccessException;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.IGeppettoDataManager;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.manager.IGeppettoManager;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.simulation.ISimulationRunExternalListener;
import org.geppetto.core.simulator.ASimulator;
import org.geppetto.model.ModelFormat;
import org.geppetto.simulation.manager.ExperimentRunManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.google.gson.Gson;

public class ScidashSimulatorService extends ASimulator implements ISimulationRunExternalListener {

	private static Log logger = LogFactory.getLog(ScidashSimulatorService.class);

	@Autowired
	private SimulatorConfig scidashSimulatorConfig;
	
	@Autowired
	private IGeppettoManager geppettoManager;
	
	Map <Long,IExperiment> runningSimulations = new ConcurrentHashMap<>();
	
	public ScidashSimulatorService()
	{
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
		logger.info("New ScidashSimulatorService object");
	}
	
	public ScidashSimulatorService(IGeppettoManager geppettoManager)
	{
		this.geppettoManager = geppettoManager;
		logger.info("New ScidashSimulatorService object");
	}
	
	public void runExperiment(long experimentID, long projectId, long token_id)
	{
		IGeppettoProject geppettoProject = retrieveGeppettoProject(projectId);
		IExperiment experiment = retrieveExperiment(experimentID, geppettoProject);
		try
		{
			// run the matched experiment
			if(experiment != null)
			{
				if(experiment.getStatus() == ExperimentStatus.ERROR)
				{
					experiment.setStatus(ExperimentStatus.DESIGN);
				}
				ExperimentRunManager.getInstance().setExternalExperimentListener(this);
				this.geppettoManager.runExperiment("", experiment);
				runningSimulations.put(token_id, experiment);
			}
			else
			{
				logger.error("Error running experiment, the experiment " + experimentID + " was not found in project " + projectId);
			}

		}
		catch(GeppettoExecutionException | GeppettoAccessException e)
		{
			logger.error("Error running experiment " + e.getMessage());
		}
	}
	
	private void sendResults(List<URL> results) {
		String json = new Gson().toJson(results);
		logger.info("Json: " + json);
	}
	
	/**
	 * @param experimentID
	 * @param geppettoProject
	 * @return
	 */
	private IExperiment retrieveExperiment(long experimentID, IGeppettoProject geppettoProject)
	{
		IExperiment theExperiment = null;
		// Look for experiment that matches id passed
		for(IExperiment e : geppettoProject.getExperiments())
		{
			if(e.getId() == experimentID)
			{
				// The experiment is found
				theExperiment = e;
				break;
			}
		}
		return theExperiment;
	}

	/**
	 * @param projectId
	 * @return
	 */
	private IGeppettoProject retrieveGeppettoProject(long projectId)
	{
		IGeppettoDataManager dataManager = DataManagerHelper.getDataManager();
		return dataManager.getGeppettoProjectById(projectId);
	}

	@Override
	public void simulationFailed(String errorMessage, Exception e, IExperiment experiment) {
	}

	@Override
	public void simulationDone(IExperiment experiment, List<URL> results)  throws GeppettoExecutionException {
		this.sendResults(results);		
	}



	@Override
	public void registerGeppettoService() throws Exception {
		List<ModelFormat> modelFormats = new ArrayList<ModelFormat>(Arrays.asList(ServicesRegistry.registerModelFormat("NEURON")));
		ServicesRegistry.registerSimulatorService(this, modelFormats);
	}

	@Override
	public void simulate() throws GeppettoExecutionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return this.scidashSimulatorConfig.getSimulatorName();
	}

	@Override
	public String getId() {
		return this.scidashSimulatorConfig.getSimulatorID();
	}
}
