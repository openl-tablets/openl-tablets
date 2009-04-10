/**
 * Created Jan 31, 2007
 */
package org.openl.tablets.tutorial4;


/**
 * @author snshor
 *
 */
public class VehiclePrice extends Price
{
	Vehicle vehicle;

	public VehiclePrice(Vehicle vehicle)
	{
		super(vehicle.getName());
		this.vehicle = vehicle;
	}

	

}
