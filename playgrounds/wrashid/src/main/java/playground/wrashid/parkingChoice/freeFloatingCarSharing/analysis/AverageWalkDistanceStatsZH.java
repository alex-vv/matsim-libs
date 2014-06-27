/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.PC2.analysis.AverageWalkDistanceStats;
import org.matsim.contrib.parking.PC2.infrastructure.Parking;

public class AverageWalkDistanceStatsZH extends AverageWalkDistanceStats {

	public AverageWalkDistanceStatsZH(Network network, HashMap<Id, Parking> parking) {
		super(network, parking);
	}

	@Override
	public String getGroupName(Id parkingId) {
		return ParkingGroupOccupanciesZH.getGroup(parkingId);
	}
	
	@Override
	public void reset(int iteration) {
		if (iteration>0){
			printStatistics();
		}
		super.reset(iteration);
	}

}
