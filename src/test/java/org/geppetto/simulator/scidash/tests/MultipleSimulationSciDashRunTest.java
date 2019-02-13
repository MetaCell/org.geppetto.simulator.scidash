package org.geppetto.simulator.scidash.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.PathConfiguration;
import org.geppetto.core.beans.SimulatorConfig;
import org.geppetto.core.common.GeppettoAccessException;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.DefaultGeppettoDataManager;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IUserGroup;
import org.geppetto.core.data.model.UserPrivileges;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.services.registry.ApplicationListenerBean;
import org.geppetto.core.simulator.ExternalSimulatorConfig;
import org.geppetto.model.neuroml.services.LEMSConversionService;
import org.geppetto.model.neuroml.services.LEMSModelInterpreterService;
import org.geppetto.model.neuroml.services.NeuroMLModelInterpreterService;
import org.geppetto.simulation.GeppettoManagerConfiguration;
import org.geppetto.simulation.manager.GeppettoManager;
import org.geppetto.simulator.external.services.NeuronSimulatorService;
import org.geppetto.simulator.scidash.config.ScidashSimulatorConfig;
import org.geppetto.simulator.scidash.services.ScidashSimulatorService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.GenericWebApplicationContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Runs multiple experiments which use scidashSimulator. 
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/META-INF/spring/app-config.xml"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MultipleSimulationSciDashRunTest
{	
	private static Log logger = LogFactory.getLog(GeppettoManagerScidashRunExperimentTest.class);
	private static GeppettoManager manager;
	private static IGeppettoProject geppettoProject;
	private static GenericWebApplicationContext context;

	/**
	 * @throws java.lang.Exception
	 */
	@SuppressWarnings("deprecation")
	@BeforeClass
	public static void setUp() throws Exception
	{
		context = new GenericWebApplicationContext();

		//needed to have newly created config beans available at time of services creation
		context.refresh();

		String neuron_home = System.getenv("NEURON_HOME");
		if (!(new File(neuron_home+"/nrniv")).exists())
		{
			neuron_home = System.getenv("NEURON_HOME")+"/bin/";
			if (!(new File(neuron_home+"/nrniv")).exists())
			{
				throw new GeppettoExecutionException("Please set the environment variable NEURON_HOME to point to your local install of NEURON 7.4");
			}
		}

		//Create configuration beans used by neuron and scidash services
		BeanDefinition scidashConfiguration = new RootBeanDefinition(ScidashSimulatorConfig.class);
		BeanDefinition neuronConfiguration = new RootBeanDefinition(SimulatorConfig.class);
		BeanDefinition neuronExternalConfig = new RootBeanDefinition(ExternalSimulatorConfig.class);

		//register config beans with spring context
		context.registerBeanDefinition("neuronExternalSimulatorConfig", neuronExternalConfig);
		context.registerBeanDefinition("neuronSimulatorConfig", neuronConfiguration);
		context.registerBeanDefinition("scidashSimulatorConfig", scidashConfiguration);

		//retrieve config beans from context and set properties 
		((ExternalSimulatorConfig)context.getBean("neuronExternalSimulatorConfig")).setSimulatorPath(neuron_home);
		((SimulatorConfig)context.getBean("neuronSimulatorConfig")).setSimulatorID("neuronSimulator");
		((SimulatorConfig)context.getBean("neuronSimulatorConfig")).setSimulatorID("neuronSimulator");
		((ScidashSimulatorConfig)context.getBean("scidashSimulatorConfig")).setSimulatorName("scidashSimulator");
		((ScidashSimulatorConfig)context.getBean("scidashSimulatorConfig")).setSimulatorID("scidashSimulator");
		((ScidashSimulatorConfig)context.getBean("scidashSimulatorConfig")).setServerURL("http://ptsv2.com/t/ykj4f-1534932468/post");


		//create the different spring services ussed by geppetto
		BeanDefinition scidashSimulatorServiceBeanDefinition = new RootBeanDefinition(ScidashSimulatorService.class,2,false);
		scidashSimulatorServiceBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
		BeanDefinition neuroMLModelInterpreterBeanDefinition = new RootBeanDefinition(NeuroMLModelInterpreterService.class);
		neuroMLModelInterpreterBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
		BeanDefinition lemsModelInterpreterBeanDefinition = new RootBeanDefinition(LEMSModelInterpreterService.class);
		lemsModelInterpreterBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
		BeanDefinition conversionServiceBeanDefinition = new RootBeanDefinition(LEMSConversionService.class);
		conversionServiceBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
		BeanDefinition neuronSimulatorServiceBeanDefinition = new RootBeanDefinition(NeuronSimulatorService.class);
		neuronSimulatorServiceBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);

		//register services beans with context
		context.registerBeanDefinition("neuroMLModelInterpreter", neuroMLModelInterpreterBeanDefinition);
		context.registerBeanDefinition("scopedTarget.neuroMLModelInterpreter", neuroMLModelInterpreterBeanDefinition);
		context.registerBeanDefinition("lemsModelInterpreter", lemsModelInterpreterBeanDefinition);
		context.registerBeanDefinition("scopedTarget.lemsModelInterpreter", lemsModelInterpreterBeanDefinition);
		context.registerBeanDefinition("lemsConversion", conversionServiceBeanDefinition);
		context.registerBeanDefinition("scopedTarget.lemsConversion", conversionServiceBeanDefinition);
		context.registerBeanDefinition("neuronSimulator", neuronSimulatorServiceBeanDefinition);
		context.registerBeanDefinition("scopedTarget.neuronSimulator", neuronSimulatorServiceBeanDefinition);		
		context.registerBeanDefinition("scidashSimulator", scidashSimulatorServiceBeanDefinition);
		context.registerBeanDefinition("scopedTarget.scidashSimulator", scidashSimulatorServiceBeanDefinition);

		ContextRefreshedEvent event = new ContextRefreshedEvent(context);
		ApplicationListenerBean listener = new ApplicationListenerBean();
		listener.onApplicationEvent(event);
		ApplicationContext retrievedContext = ApplicationListenerBean.getApplicationContext("scidashSimulator");
		Assert.assertNotNull(retrievedContext.getBean("scopedTarget.scidashSimulator"));		
		Assert.assertTrue(retrievedContext.getBean("scopedTarget.scidashSimulator") instanceof ScidashSimulatorService);

		DataManagerHelper.setDataManager(new DefaultGeppettoDataManager());
		
		GeppettoManagerConfiguration geppettoManagerConfiguration = new GeppettoManagerConfiguration();
		geppettoManagerConfiguration.setAllowVolatileProjectsSimulation(true);
		manager = new GeppettoManager(Scope.CONNECTION, geppettoManagerConfiguration);
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.frontend.controllers.GeppettoManager#setUser(org.geppetto.core.data.model.IUser)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test01SetUser() throws GeppettoExecutionException
	{
		long value = 1000l * 1000 * 1000;
		List<UserPrivileges> privileges = new ArrayList<UserPrivileges>();
		privileges.add(UserPrivileges.RUN_EXPERIMENT);
		privileges.add(UserPrivileges.READ_PROJECT);
		IUserGroup userGroup = DataManagerHelper.getDataManager().newUserGroup("unaccountableAristocrats", privileges, value, value * 2);
		manager.setUser(DataManagerHelper.getDataManager().newUser("scidashtestuser", "passauord", true, userGroup));
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.frontend.controllers.GeppettoManager#getUser()}.
	 */
	@Test
	public void test02GetUser()
	{
		Assert.assertEquals("scidashtestuser", manager.getUser().getName());
		Assert.assertEquals("passauord", manager.getUser().getPassword());
	}

	@Test
	public void test03LoadProject() throws IOException, GeppettoInitializationException, GeppettoExecutionException, GeppettoAccessException
	{
		InputStreamReader inputStreamReader = new InputStreamReader(GeppettoManagerScidashRunExperimentTest.class.getResourceAsStream("/multipleSimulationNeuronTest/GEPPETTO.json"));
		geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(getGson(), inputStreamReader, null);
		manager.loadProject("1", geppettoProject);

	}

	@Test
	public void test04ExperimentNeuronRun() throws GeppettoExecutionException, GeppettoAccessException, InterruptedException
	{			
		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(5, status.size());
		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(0).getStatus());  //test design status on experiment
		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(1).getStatus());  //test design status on experiment
		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(2).getStatus());  //test design status on experiment
		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(3).getStatus());  //test design status on experiment
		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(4).getStatus());  //test design status on experiment

		Assert.assertEquals(0, status.get(0).getSimulationResults().size());  //test empty experiment results list pre-running
		Assert.assertEquals(0, status.get(1).getSimulationResults().size());  //test empty experiment results list pre-running
		Assert.assertEquals(0, status.get(2).getSimulationResults().size());  //test empty experiment results list pre-running
		Assert.assertEquals(0, status.get(3).getSimulationResults().size());  //test empty experiment results list pre-running
		Assert.assertEquals(0, status.get(4).getSimulationResults().size());  //test empty experiment results list pre-running

		manager.runExperiment("1", geppettoProject.getExperiments().get(0));
		manager.runExperiment("1", geppettoProject.getExperiments().get(1));
		manager.runExperiment("1", geppettoProject.getExperiments().get(2));
		manager.runExperiment("1", geppettoProject.getExperiments().get(3));
		manager.runExperiment("1", geppettoProject.getExperiments().get(4));

		Thread.sleep(150000);

		status = manager.checkExperimentsStatus("5", geppettoProject);
		if(status.get(0).getStatus() != ExperimentStatus.COMPLETED) {
			Thread.sleep(30000);
		}
		status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(5, status.size());  
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());  //test completion of experiment run
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(1).getStatus());  //test completion of experiment run
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(2).getStatus());  //test completion of experiment run
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(3).getStatus());  //test completion of experiment run
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(4).getStatus());  //test completion of experiment run

		Assert.assertEquals(2, status.get(0).getSimulationResults().size());  //test experiment simulation list results
		Assert.assertEquals(2, status.get(1).getSimulationResults().size());  //test experiment simulation list results
		Assert.assertEquals(2, status.get(2).getSimulationResults().size());  //test experiment simulation list results
		Assert.assertEquals(2, status.get(3).getSimulationResults().size());  //test experiment simulation list results
		Assert.assertEquals(2, status.get(4).getSimulationResults().size());  //test experiment simulation list results
	}

	@Test
	public void test05DeleteProjectFiles() throws GeppettoExecutionException, IOException
	{
		File projectTmPath = new File(PathConfiguration.getProjectTmpPath(Scope.RUN, geppettoProject.getId()));
		Assert.assertTrue(projectTmPath.exists());

		PathConfiguration.deleteProjectTmpFolder(Scope.RUN, geppettoProject.getId());

		projectTmPath = new File(PathConfiguration.getProjectTmpPath(Scope.RUN, geppettoProject.getId()));
		Assert.assertFalse(projectTmPath.exists());
	}

	public static Gson getGson()
	{
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>()
		{
			@Override
			public Date deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException
			{
				return new Date(json.getAsJsonPrimitive().getAsLong());
			}
		});
		return builder.create();
	}
}
