//package org.geppetto.simulator.scidash.tests;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
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
//import org.geppetto.core.simulator.ExternalSimulatorConfig;
//import org.geppetto.model.neuroml.services.LEMSConversionService;
//import org.geppetto.model.neuroml.services.LEMSModelInterpreterService;
//import org.geppetto.model.neuroml.services.NeuroMLModelInterpreterService;
//import org.geppetto.simulation.manager.GeppettoManager;
//import org.geppetto.simulator.external.services.NeuronSimulatorService;
//import org.geppetto.simulator.scidash.services.ScidashSimulatorService;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runners.MethodSorters;
//import org.springframework.beans.factory.config.BeanDefinition;
//import org.springframework.beans.factory.support.RootBeanDefinition;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.web.context.support.GenericWebApplicationContext;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonDeserializationContext;
//import com.google.gson.JsonDeserializer;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonParseException;
//
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//public class MultipleSimulationSciDashRunTest
//{	
//	private static Log logger = LogFactory.getLog(MultipleSimulationSciDashRunTest.class);
//	
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
//		GenericWebApplicationContext context = new GenericWebApplicationContext();
//		BeanDefinition scidashSimulatorServiceBeanDefinition = new RootBeanDefinition(ScidashSimulatorService.class);
//		BeanDefinition neuroMLModelInterpreterBeanDefinition = new RootBeanDefinition(NeuroMLModelInterpreterService.class);
//		BeanDefinition lemsModelInterpreterBeanDefinition = new RootBeanDefinition(LEMSModelInterpreterService.class);
//		BeanDefinition conversionServiceBeanDefinition = new RootBeanDefinition(LEMSConversionService.class);
//		BeanDefinition neuronSimulatorServiceBeanDefinition = new RootBeanDefinition(NeuronSimulatorService.class);
//		
//		context.registerBeanDefinition("neuroMLModelInterpreter", neuroMLModelInterpreterBeanDefinition);
//		context.registerBeanDefinition("scopedTarget.neuroMLModelInterpreter", neuroMLModelInterpreterBeanDefinition);
//		context.registerBeanDefinition("lemsModelInterpreter", lemsModelInterpreterBeanDefinition);
//		context.registerBeanDefinition("scopedTarget.lemsModelInterpreter", lemsModelInterpreterBeanDefinition);
//		context.registerBeanDefinition("lemsConversion", conversionServiceBeanDefinition);
//		context.registerBeanDefinition("scopedTarget.lemsConversion", conversionServiceBeanDefinition);
//		context.registerBeanDefinition("neuronSimulator", neuronSimulatorServiceBeanDefinition);
//		context.registerBeanDefinition("scopedTarget.neuronSimulator", neuronSimulatorServiceBeanDefinition);
//		
//		context.registerBeanDefinition("scidashSimulator", scidashSimulatorServiceBeanDefinition);
//		context.registerBeanDefinition("scopedTarget.scidashSimulator", scidashSimulatorServiceBeanDefinition);
//		
//		ContextRefreshedEvent event = new ContextRefreshedEvent(context);
//		ApplicationListenerBean listener = new ApplicationListenerBean();
//		listener.onApplicationEvent(event);
//		ApplicationContext retrievedContext = ApplicationListenerBean.getApplicationContext("scidashSimulator");
//		Assert.assertNotNull(retrievedContext.getBean("scopedTarget.scidashSimulator"));
//		Assert.assertTrue(retrievedContext.getBean("scopedTarget.scidashSimulator") instanceof ScidashSimulatorService);
//		
//		String neuron_home = System.getenv("NEURON_HOME");
//		if (!(new File(neuron_home+"/nrniv")).exists())
//		{
//		    neuron_home = System.getenv("NEURON_HOME")+"/bin/";
//		    if (!(new File(neuron_home+"/nrniv")).exists())
//		    {
//		        throw new GeppettoExecutionException("Please set the environment variable NEURON_HOME to point to your local install of NEURON 7.4");
//		    }
//		}
//		ExternalSimulatorConfig externalConfig = new ExternalSimulatorConfig();
//		externalConfig.setSimulatorPath(neuron_home);
//		Assert.assertNotNull(externalConfig.getSimulatorPath());
//		SimulatorConfig simulatorConfig = new SimulatorConfig();
//		simulatorConfig.setSimulatorID("neuronSimulator");
//		simulatorConfig.setSimulatorName("neuronSimulator");
//		
//		((NeuronSimulatorService)retrievedContext.getBean("scopedTarget.neuronSimulator")).setNeuronExternalSimulatorConfig(externalConfig);
//		((NeuronSimulatorService)retrievedContext.getBean("scopedTarget.neuronSimulator")).setNeuronSimulatorConfig(simulatorConfig);
//				
//		DataManagerHelper.setDataManager(new DefaultGeppettoDataManager());
//		
//		simulator = new ScidashSimulatorService(manager);
//		simulator.setUploadResultsURL("http://ptsv2.com/t/kg13r-1533565685/post");
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
//	
//	@Test
//	public void test03LoadProject() throws IOException, GeppettoInitializationException, GeppettoExecutionException, GeppettoAccessException
//	{
//		InputStreamReader inputStreamReader = new InputStreamReader(MultipleSimulationSciDashRunTest.class.getResourceAsStream("/multipleSimulationNeuronTest/GEPPETTO.json"));
//		geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(getGson(), inputStreamReader, null);
//		manager.loadProject("1", geppettoProject);
//
//	}
//
//	@Test
//	public void test04ExperimentNeuronRun() throws GeppettoExecutionException, GeppettoAccessException, InterruptedException
//	{			
//		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
//		Assert.assertEquals(5, status.size());
//		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(0).getStatus());  //test design status on experiment
//		Assert.assertEquals(0, status.get(0).getSimulationResults().size());  //test empty experiment results list pre-running
//		
//		simulator.runExperiment(geppettoProject.getExperiments().get(0).getId(),geppettoProject.getId(), 1);
//		simulator.runExperiment(geppettoProject.getExperiments().get(1).getId(),geppettoProject.getId(), 2);
//		simulator.runExperiment(geppettoProject.getExperiments().get(2).getId(),geppettoProject.getId(), 3);
//		simulator.runExperiment(geppettoProject.getExperiments().get(3).getId(),geppettoProject.getId(), 4);
//		simulator.runExperiment(geppettoProject.getExperiments().get(4).getId(),geppettoProject.getId(), 5);
//		
//		Thread.sleep(250000);
//		
//		status = manager.checkExperimentsStatus("1", geppettoProject);
//		if(status.get(0).getStatus() == ExperimentStatus.RUNNING) {
//			Thread.sleep(30000);
//		}
//		status = manager.checkExperimentsStatus("1", geppettoProject);
//		Assert.assertEquals(1, status.size());  
//		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());  //test completion of experiment run
//		Assert.assertEquals(2, status.get(0).getSimulationResults().size());  //test experiment simulation list results
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
//}
