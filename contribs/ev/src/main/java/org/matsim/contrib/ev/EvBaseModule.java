package org.matsim.contrib.ev;

import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.fleet.ElectricFleetModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureModule;
import org.matsim.contrib.ev.stats.EvStatsModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

import java.util.List;

public class EvBaseModule extends AbstractModule {
	public void install(){
		install(new ElectricFleetModule() );
		install(new ChargingInfrastructureModule() );
		install(new ChargingModule() );
		install(new DischargingModule() );
		install(new EvStatsModule() );
		{
			// this switches on all the QSimComponents that are registered at various places under EvModule.EV_Component.
			QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( this.getConfig(), QSimComponentsConfigGroup.class );
			List<String> cmps = qsimComponentsConfig.getActiveComponents();
			cmps.add( EvModule.EV_COMPONENT );
			qsimComponentsConfig.setActiveComponents( cmps );

			// chained version would be:
//			qsimComponentsConfig.setActiveComponents( Stream.concat( qsimComponentsConfig.getActiveComponents().stream(), Stream.of( EV_COMPONENT) ).toList() );
		}
	}

}
