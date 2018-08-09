
package org.geppetto.simulator.scidash.config;

/**
 * @author Jesus Martinez (jesus@metacell.us)
 *
 */
public class ScidashSimulatorConfig
{

	private String simulatorName;
	private String simulatorID;
	private String serverURL;

	public void setSimulatorName(String simulatorName)
	{
		this.simulatorName = simulatorName;
	}

	public String getSimulatorName()
	{
		return this.simulatorName;
	}

	public String getSimulatorID()
	{
		return this.simulatorID;
	}

	public void setSimulatorID(String simulatorID)
	{
		this.simulatorID = simulatorID;
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}
}
