package org.geppetto.simulator.scidash.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.geppetto.core.beans.SimulatorConfig;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.data.model.local.LocalAspectConfiguration;
import org.geppetto.core.data.model.local.LocalExperiment;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ExternalSimulatorConfig;
import org.geppetto.model.ExperimentState;
import org.geppetto.model.ExternalDomainModel;
import org.geppetto.model.GeppettoFactory;
import org.geppetto.simulator.external.services.NeuronSimulatorService;
import org.geppetto.simulator.external.services.Utilities;
import org.geppetto.simulator.scidash.services.ScidashSimulatorService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimulationSciDashRunTest implements ISimulatorCallbackListener
{

	private static String dirToExecute;
	private static String fileToExecute;
	private static NeuronSimulatorService simulator;
	private static String resultsDir;
	private static boolean done = false;

	@BeforeClass
	public static void setup() throws GeppettoExecutionException
	{
		File dir = new File(SimulationSciDashRunTest.class.getResource("/neuronConvertedModel/").getFile());
		dirToExecute = dir.getAbsolutePath();
		fileToExecute = "/main_script.py";

		simulator = new ScidashSimulatorService();
		simulator.registerGeppettoService();

		String neuron_home = System.getenv("NEURON_HOME");
		ExternalSimulatorConfig externalConfig = new ExternalSimulatorConfig();
		externalConfig.setSimulatorPath(neuron_home);
		Assert.assertNotNull(externalConfig.getSimulatorPath());
		simulator.setNeuronExternalSimulatorConfig(externalConfig);
		SimulatorConfig simulatorConfig = new SimulatorConfig();
		simulatorConfig.setSimulatorID("neuronSimulator");
		simulatorConfig.setSimulatorName("neuronSimulator");
		simulator.setNeuronSimulatorConfig(simulatorConfig);
	}

	/**
	 * Test method for {@link org.geppetto.simulator.external.services.NeuronSimulatorService}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNeuronExecution() throws GeppettoExecutionException, GeppettoInitializationException, InterruptedException
	{
		ExternalDomainModel model = GeppettoFactory.eINSTANCE.createExternalDomainModel();
		model.setFormat(ServicesRegistry.getModelFormat("NEURON"));
		model.setDomainModel(dirToExecute + fileToExecute);
		simulator.initialize(model, new LocalAspectConfiguration(1, "testModel", null, null, null), null, this, null);
		simulator.setProjectId(1);
		simulator.setExperiment(new LocalExperiment(1, null, null, null, null, null, null, null, null, null, null));
		simulator.simulate();
		Thread.sleep(50000);
		Assert.assertTrue(done);
	}

	@Override
	public void endOfSteps(IAspectConfiguration aspectConfiguration, Map<File, ResultsFormat> results)
	{

		resultsDir = dirToExecute + "results/";
		BufferedReader input = null;
		// will store values and variables found in DAT
		HashMap<String, List<Float>> dataValues = new HashMap<String, List<Float>>();
		try
		{
			// read DAT into a buffered reader
			File dir = new File(SimulationSciDashRunTest.class.getResource("/neuronConvertedModel/results/ex5_vars.dat").getFile());

			input = new BufferedReader(new FileReader(dir));

			// read rest of DAT file and extract values
			String line = input.readLine();
			String[] columns = line.split("\\s+");

			Assert.assertEquals(Float.valueOf(columns[0]), 0.0f);
			Assert.assertEquals(Float.valueOf(columns[1]), 0.052932f);
			Assert.assertEquals(Float.valueOf(columns[2]), 0.596121f);
			Assert.assertEquals(Float.valueOf(columns[3]), 0.317677f);

			input.close();

			// read DAT into a buffered reader
			dir = new File(SimulationSciDashRunTest.class.getResource("/neuronConvertedModel/results/ex5_v.dat").getFile());
			input = new BufferedReader(new FileReader(dir));

			// read rest of DAT file and extract values
			line = input.readLine();
			columns = line.split("\\s+");

			Assert.assertEquals(Float.valueOf(columns[0]), 0.0f);
			Assert.assertEquals(Float.valueOf(columns[1]), -0.065000f);

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		// handles End of file exception
		finally
		{
			try
			{
				input.close();
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}

		Assert.assertEquals(2, results.size());
		done = true;
	}

	@AfterClass
	public static void doYourOneTimeTeardown() throws IOException
	{
		 Utilities.delete(new File(SimulationSciDashRunTest.class.getResource("/neuronConvertedModel/results/").getFile()));
		 Utilities.delete(new File(SimulationSciDashRunTest.class.getResource("/neuronConvertedModel/x86_64/").getFile()));
		 Utilities.delete(new File(SimulationSciDashRunTest.class.getResource("/neuronConvertedModel/time.dat").getFile()));
	}

	@Override
	public void externalProcessFailed(String message, Exception e)
	{
		// TODO Auto-generated method stub

	}

}