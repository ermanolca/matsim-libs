/* *********************************************************************** *
 * project: kai
 * SimpleAdaptiveSignalEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.kai.usecases.myownnode;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.ptproject.qsim.InternalInterface;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;

/**
 * @author nagel
 *
 */
class SimpleAdaptiveSignalEngine implements MobsimEngine, LinkEnterEventHandler, LinkLeaveEventHandler {

	private InternalInterface internalInterface;
	private Queue<Double> vehicleExitTimesOnLink5 = new LinkedList<Double>() ;
	private long cnt4 = 0 ;
	private long cnt5 = 0 ;
	private Controler controler;
	private Writer out ;
	
	class Result {
		int iteration ;
		double shareUp ;
		double shareDown ;
	} ;
	
	private List<Result> results = new ArrayList<Result>() ;
	
	public SimpleAdaptiveSignalEngine(Controler controler) {
		this.controler = controler ;
	}

	@Override
	public void afterSim() {
	}

	@Override
	public Netsim getMobsim() {
		return internalInterface.getMobsim() ;
	}

	@Override
	public void onPrepareSim() {
//		SignalizeableItem link4 = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(new IdImpl("4")) ;
//		link4.setSignalized(true) ;
//		SignalizeableItem link5 = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(new IdImpl("5")) ;
//		link5.setSignalized(true) ;
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface ;
	}

	@Override
	public void doSimStep(double time) {
//		SignalizeableItem link4 = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(new IdImpl("4")) ;
//		SignalizeableItem link5 = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(new IdImpl("5")) ;
//		final Double dpTime = this.vehicleExitTimesOnLink5.peek();
//		if ( dpTime !=null && dpTime < time && (long)time%4 < 4  ) {
//			link4.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
//			link5.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
//		} else {
//			link4.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
//			link5.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
//		}

//		if ( time<20.*60 && (long)time%2==0 ) { 
//			link4.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
//		} else {
//			link4.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
//		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if ( event.getLinkId().equals(new IdImpl("5")) ) {
			this.vehicleExitTimesOnLink5.add( event.getTime() + 100. ) ;
			// yy replace "100" by freeSpeedTravelTime
			cnt5++ ;
		} 
		if ( event.getLinkId().equals(new IdImpl("4")) ) {
			cnt4++ ;
		}
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if ( event.getLinkId().equals(new IdImpl("5")) ) {
			this.vehicleExitTimesOnLink5.remove() ;
		}
	}

	@Override
	public void reset(int iteration) {
		double sum = cnt4 + cnt5 ;
		Logger.getLogger(this.getClass()).warn("iteration: " + iteration + " cnt4: " + (cnt4/sum) + " cnt5: " + (cnt5/sum) ) ; 
		
		Result result = new Result() ;
		result.iteration = iteration ;
		result.shareUp = cnt5/sum ;
		result.shareDown = cnt4/sum ;
		results.add(result) ;
		
		cnt4 = 0 ;
		cnt5 = 0 ;

		if ( out==null ) {
			out = IOUtils.getBufferedWriter(controler.getControlerIO().getOutputFilename("split.txt")) ;
		}

		
		try {
			out.write( result.iteration + "\t" + result.shareUp + "\t" + result.shareDown + "\n" ) ;
			out.flush() ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	static public void main( String[] args ) {
		Main.main( args ) ;
	}

}
