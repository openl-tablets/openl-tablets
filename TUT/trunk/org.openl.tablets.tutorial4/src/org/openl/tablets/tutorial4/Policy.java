/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial4;

import org.openl.base.NamedThing;

/**
 * @author snshor
 * 
 */
public class Policy extends NamedThing {

	Driver[] drivers;
	Vehicle[] vehicles;

	String clientTier;
	String clientTerm;

	/**
	 * @return Returns the drivers.
	 */
	public Driver[] getDrivers() {
		return drivers;
	}

	/**
	 * @param drivers
	 *            The drivers to set.
	 */
	public void setDrivers(Driver[] drivers) {
		this.drivers = drivers;
	}

	/**
	 * @return Returns the vehicles.
	 */
	public Vehicle[] getVehicles() {
		return vehicles;
	}

	/**
	 * @param vehicles
	 *            The vehicles to set.
	 */
	public void setVehicles(Vehicle[] vehicles) {
		this.vehicles = vehicles;
	}

	/**
	 * @return Returns the clientTier.
	 */
	public String getClientTier() {
		return clientTier;
	}

	/**
	 * @param clientTier
	 *            The clientTier to set.
	 */
	public void setClientTier(String clientTier) {
		this.clientTier = clientTier;
	}

	/**
	 * @return Returns the clientTerm.
	 */
	public String getClientTerm() {
		return clientTerm;
	}

	/**
	 * @param clientTerm
	 *            The clientTerm to set.
	 */
	public void setClientTerm(String clientTerm) {
		this.clientTerm = clientTerm;
	}
}
