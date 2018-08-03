//package org.geppetto.simulator.scidash.tests;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import org.geppetto.core.beans.SimulatorConfig;
//import org.geppetto.core.common.GeppettoAccessException;
//import org.geppetto.core.common.GeppettoExecutionException;
//import org.geppetto.core.common.GeppettoInitializationException;
//import org.geppetto.core.data.DataManagerHelper;
//import org.geppetto.core.data.DefaultGeppettoDataManager;
//import org.geppetto.core.data.model.ExperimentStatus;
//import org.geppetto.core.data.model.IExperiment;
//import org.geppetto.core.data.model.IGeppettoProject;
//import org.geppetto.core.data.model.IUserGroup;
//import org.geppetto.core.data.model.UserPrivileges;
//import org.geppetto.core.manager.Scope;
//import org.geppetto.core.services.registry.ApplicationListenerBean;
//import org.geppetto.core.simulation.ISimulationRunExternalListener;
//import org.geppetto.simulation.manager.GeppettoManager;
//import org.geppetto.simulator.scidash.services.ScidashSimulatorService;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runners.MethodSorters;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonDeserializationContext;
//import com.google.gson.JsonDeserializer;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonParseException;
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//public class SimulationSciDashRunTest implements ISimulationRunExternalListener 
//{	
//	private static GeppettoManager manager = new GeppettoManager(Scope.CONNECTION);
//	private static IGeppettoProject geppettoProject;
//
//	private static ScidashSimulatorService simulator;
//	
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@SuppressWarnings("deprecation")
//	@BeforeClass
//	public static void setUp() throws Exception
//	{			
//		DataManagerHelper.setDataManager(new DefaultGeppettoDataManager());
//		
//		simulator = new ScidashSimulatorService(manager);
//	}
//	
//	/**
//	 * Test method for {@link org.geppetto.simulation.manager.frontend.controllers.GeppettoManager#setUser(org.geppetto.core.data.model.IUser)}.
//	 * 
//	 * @throws GeppettoExecutionException
//	 */
//	@Test
//	public void test01SetUser() throws GeppettoExecutionException
//	{
//		long value = 1000l * 1000 * 1000;
//		List<UserPrivileges> privileges = new ArrayList<UserPrivileges>();
//		privileges.add(UserPrivileges.RUN_EXPERIMENT);
//		privileges.add(UserPrivileges.READ_PROJECT);
//		IUserGroup userGroup = DataManagerHelper.getDataManager().newUserGroup("unaccountableAristocrats", privileges, value, value * 2);
//		manager.setUser(DataManagerHelper.getDataManager().newUser("nonna", "passauord", true, userGroup));
//	}
//
//	/**
//	 * Test method for {@link org.geppetto.simulation.manager.frontend.controllers.GeppettoManager#getUser()}.
//	 */
//	@Test
//	public void test02GetUser()
//	{
//		Assert.assertEquals("nonna", manager.getUser().getName());
//		Assert.assertEquals("passauord", manager.getUser().getPassword());
//	}
//	
//	@Test
//	public void test03LoadProject() throws IOException, GeppettoInitializationException, GeppettoExecutionException, GeppettoAccessException
//	{
//		InputStreamReader inputStreamReader = new InputStreamReader(SimulationSciDashRunTest.class.getResourceAsStream("/simulationNeuronTest/GEPPETTO.json"));
//		geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(getGson(), inputStreamReader, null);
//		manager.loadProject("1", geppettoProject);
//
//	}
//
//	@Test
//	public void test04ExperimentNeuronRun() throws GeppettoExecutionException, GeppettoAccessException, InterruptedException
//	{			
//		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
//		Assert.assertEquals(1, status.size());
//		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(0).getStatus());
//		
//		simulator.runExperiment(geppettoProject.getExperiments().get(0).getId(),geppettoProject.getId(), 232323232);
//		
//		Thread.sleep(90000);		
//	}
//	
//	public static Gson getGson()
//	{
//		GsonBuilder builder = new GsonBuilder();
//		builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>()
//		{
//			@Override
//			public Date deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException
//			{
//				return new Date(json.getAsJsonPrimitive().getAsLong());
//			}
//		});
//		return builder.create();
//	}
//
//	@Override
//	public void simulationDone(IExperiment experiment) throws GeppettoExecutionException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void simulationResultsReady(IExperiment experiment, List<URL> results) throws GeppettoExecutionException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void simulationFailed(String errorMessage, Exception e, IExperiment experiment) {
//		// TODO Auto-generated method stub
//		
//	}
//}
