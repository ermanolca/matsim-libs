/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCollector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmonitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.network.WithinDayLinkImpl;


/**
 * Collects link travel times over a given time span (storedTravelTimesBinSize) and
 * calculates an average travel time over this time span.
 *
 * TODO:
 * - take transport mode for multi-modal simulations into account
 *   Extend link enter / leave events with transport mode field or use a
 *   lookup table in this class.
 * - make storedTravelTimesBinSize configurable (e.g. via config)
 * - get rid of WithinDayLinkImpl, would suggest via Customizable
 *
 * @author cdobler
 */
public class TravelTimeCollector implements PersonalizableTravelTime, AgentStuckEventHandler,
	LinkEnterEventHandler, LinkLeaveEventHandler,
	AgentArrivalEventHandler, AgentDepartureEventHandler, SimulationInitializedListener,
	SimulationBeforeSimStepListener, SimulationAfterSimStepListener {

//	/*
//	 * If multiple TravelTimeCollectors are used, each of them get its unique Id.
//	 * This will be needed, if the travel times are attached to the Links via a
//	 * Customizable attribute. The key of that attribute will then contain the Id
//	 * of its corresponding TravelTimeCollector.
//	 */
//	private final int id;

	private Network network;

	// Trips with no Activity on the current Link
	private Map<Id, TripBin> regularActiveTrips;	// PersonId
	private Map<Id, TravelTimeInfo> travelTimeInfos;	// LinkId

	/*
	 * For parallel Execution
	 */
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	private UpdateMeanTravelTimesThread[] updateMeanTravelTimesThreads;
	private final int numOfThreads;

	// use the factory
	/*package*/ TravelTimeCollector(Scenario scenario, int id) {
		/*
		 * The parallelization should scale almost linear, therefore
		 * we do use the number of available threads accoring to the
		 * config file.
		 */
		this(scenario.getNetwork(), scenario.getConfig().global().getNumberOfThreads(), id);
	}

	// use the factory
	/*package*/ TravelTimeCollector(Network network, int numOfThreads, int id) {
		this.network = network;
		this.numOfThreads = numOfThreads;
//		this.id = id;

		init();
	}

	private void init() {
		regularActiveTrips = new HashMap<Id, TripBin>();
		travelTimeInfos = new ConcurrentHashMap<Id, TravelTimeInfo>();

		for (Link link : this.network.getLinks().values()) {
			TravelTimeInfo travelTimeInfo = new TravelTimeInfo();
			travelTimeInfos.put(link.getId(), travelTimeInfo);
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time) {
//		return travelTimeInfos.get(link.getId()).travelTime;
		return ((WithinDayLinkImpl)link).getTravelTime();
	}

	@Override
	public void reset(int iteration) {
		init();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		double time = event.getTime();

		TripBin tripBin = new TripBin();
		tripBin.enterTime = time;

		this.regularActiveTrips.put(personId, tripBin);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		double time = event.getTime();

		TripBin tripBin = this.regularActiveTrips.remove(personId);
		if (tripBin != null) {
			tripBin.leaveTime = time;

			TravelTimeInfo travelTimeInfo = travelTimeInfos.get(linkId);
			travelTimeInfo.tripBins.add(tripBin);
			travelTimeInfo.addedTravelTimes = travelTimeInfo.addedTravelTimes + (tripBin.leaveTime - tripBin.enterTime);
			travelTimeInfo.addedTrips++;
		}
	}

	/*
	 * We don't have to count Stuck Events. The MobSim creates
	 * LeaveLink Events before throwing Stuck Events.
	 */
	@Override
	public void handleEvent(AgentStuckEvent event) {

	}

	/*
	 * If an Agent performs an Activity on a Link we have
	 * to remove his current Trip. Otherwise we would have a
	 * Trip with the Duration of the Trip itself and the Activity.
	 */
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();

		this.regularActiveTrips.remove(personId);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {

	}

	/*
	 * Initially set free speed travel time.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {

		for (Link link : this.network.getLinks().values()) {
			double freeSpeedTravelTime = link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME);
			TravelTimeInfo travelTimeInfo = travelTimeInfos.get(link.getId());
//			travelTimeInfo.travelTime = freeSpeedTravelTime;
			travelTimeInfo.link = (WithinDayLinkImpl) link;
			travelTimeInfo.link.setTravelTime(freeSpeedTravelTime);
			travelTimeInfo.freeSpeedTravelTime = freeSpeedTravelTime;
		}

		// Now initialize the Parallel Update Threads
		initParallelThreads();
	}

	// Add Link TravelTimes
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
	}

	// Update Link TravelTimes
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		// parallel Execution
		this.run(e.getSimulationTime());
	}

	@Override
	public void setPerson(Person person) {
		// nothing to do here
	}

	private static class TripBin {
		double enterTime;
		double leaveTime;

	}

	private static class TravelTimeInfo {
		WithinDayLinkImpl link;
		List<TripBin> tripBins = new ArrayList<TripBin>();

		int addedTrips = 0;
		double addedTravelTimes = 0.0;
		double sumTravelTimes = 0.0;	// We cache the sum of the TravelTimes
		double freeSpeedTravelTime = Double.MAX_VALUE;	// We cache the FreeSpeedTravelTimes
	}

	/*
	 * ----------------------------------------------------------------
	 * Methods for parallel Execution
	 * ----------------------------------------------------------------
	 */

	/*
	 * The Threads are waiting at the TimeStepStartBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Nodes. We wait until
	 * all of them reach the TimeStepEndBarrier to move on.
	 * We should not have any Problems with Race Conditions
	 * because even if the Threads would be faster than this
	 * Thread, means the reach the TimeStepEndBarrier before
	 * this Method does, it should work anyway.
	 */
	private void run(double time) {

		try {
			// set current Time
			for (UpdateMeanTravelTimesThread updateMeanTravelTimesThread : updateMeanTravelTimesThreads) {
				updateMeanTravelTimesThread.setTime(time);
			}

			this.startBarrier.await();

			this.endBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
	      	Gbl.errorMsg(e);
		}
	}

	/*
	 * Create equal sized Arrays.
	 */
	private TravelTimeInfo[][] createArrays() {

		List<List<TravelTimeInfo>> infos = new ArrayList<List<TravelTimeInfo>>();
		for (int i = 0; i < numOfThreads; i++) {
			infos.add(new ArrayList<TravelTimeInfo>());
		}

		int roundRobin = 0;
		for (Link link : this.network.getLinks().values()) {
			Id id = link.getId();
			infos.get(roundRobin % numOfThreads).add(travelTimeInfos.get(id));
			roundRobin++;
		}

		/*
		 * Now we create Arrays out of our Lists because iterating over them
		 * is much faster.
		 */
		TravelTimeInfo[][] parallelArrays = new TravelTimeInfo[this.numOfThreads][];
		for (int i = 0; i < infos.size(); i++) {
			List<TravelTimeInfo> list = infos.get(i);

			TravelTimeInfo[] array = new TravelTimeInfo[list.size()];
			list.toArray(array);
			parallelArrays[i] = array;
		}

		return parallelArrays;
	}

	private void initParallelThreads() {

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		TravelTimeInfo[][] parallelArrays = createArrays();

		updateMeanTravelTimesThreads = new UpdateMeanTravelTimesThread[numOfThreads];

		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			UpdateMeanTravelTimesThread updateMeanTravelTimesThread = new UpdateMeanTravelTimesThread();
			updateMeanTravelTimesThread.setName("UpdateMeanTravelTimes" + i);
			updateMeanTravelTimesThread.setTravelTimeInfos(parallelArrays[i]);
			updateMeanTravelTimesThread.setStartBarrier(this.startBarrier);
			updateMeanTravelTimesThread.setEndBarrier(this.endBarrier);
			updateMeanTravelTimesThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			updateMeanTravelTimesThreads[i] = updateMeanTravelTimesThread;

			updateMeanTravelTimesThread.start();
		}

		/*
		 * After initialization the Threads are waiting at the
		 * endBarrier. We trigger this Barrier once so
		 * they wait at the startBarrier what has to be
		 * their state if the run() method is called.
		 */
		try {
			this.endBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
	}

	/*
	 * The thread class that updates the Mean Travel Times in the MyLinksImpls.
	 */
	private static class UpdateMeanTravelTimesThread extends Thread {

		private CyclicBarrier startBarrier;
		private CyclicBarrier endBarrier;

		private double storedTravelTimesBinSize = 600;

		private double time = 0.0;
		private TravelTimeInfo[] travelTimeInfos;

		public UpdateMeanTravelTimesThread() {
		}

		public void setStartBarrier(CyclicBarrier cyclicBarrier) {
			this.startBarrier = cyclicBarrier;
		}

		public void setEndBarrier(CyclicBarrier cyclicBarrier) {
			this.endBarrier = cyclicBarrier;
		}

		public void setTravelTimeInfos(TravelTimeInfo[] travelTimeInfos) {
			this.travelTimeInfos = travelTimeInfos;
		}

		public void setTime(final double t) {
			time = t;
		}

		@Override
		public void run() {

			while(true) {
				try {
					/*
					 * The End of the Moving is synchronized with
					 * the endBarrier. If all Threads reach this Barrier
					 * the main run() Thread can go on.
					 *
					 * The Threads wait now at the startBarrier until
					 * they are triggered again in the next TimeStep by the main run()
					 * method.
					 */
					endBarrier.await();

					startBarrier.await();

					for (TravelTimeInfo travelTimeInfo : travelTimeInfos) {
						calcBinTravelTime(this.time, travelTimeInfo);
					}
				} catch (InterruptedException e) {
					Gbl.errorMsg(e);
				} catch (BrokenBarrierException e) {
	            	Gbl.errorMsg(e);
	            }
			}
		}	// run()

		private void calcBinTravelTime(double time, TravelTimeInfo travelTimeInfo) {
			double removedTravelTimes = 0.0;

			List<TripBin> tripBins = travelTimeInfo.tripBins;

			// first remove old TravelTimes
			Iterator<TripBin> iter = tripBins.iterator();
			while (iter.hasNext()) {
				TripBin tripBin = iter.next();
				if (tripBin.leaveTime + this.storedTravelTimesBinSize < time) {
					double travelTime = tripBin.leaveTime - tripBin.enterTime;
					removedTravelTimes = removedTravelTimes + travelTime;
					iter.remove();
				}
				else break;
			}

			/*
			 * We don't need an update if no Trips have been added or
			 * removed within the current SimStep.
			 * The initial FreeSpeedTravelTime has to be set correctly via
			 * setTravelTime!
			 */
			if (removedTravelTimes == 0.0 && travelTimeInfo.addedTravelTimes == 0.0) return;

			travelTimeInfo.sumTravelTimes = travelTimeInfo.sumTravelTimes - removedTravelTimes + travelTimeInfo.addedTravelTimes;

			travelTimeInfo.addedTravelTimes = 0.0;

			/*
			 * Ensure, that we don't allow TravelTimes shorter than the
			 * FreeSpeedTravelTime.
			 */
			double meanTravelTime = travelTimeInfo.freeSpeedTravelTime;
			if (!tripBins.isEmpty()) meanTravelTime = travelTimeInfo.sumTravelTimes / tripBins.size();

			if (meanTravelTime < travelTimeInfo.freeSpeedTravelTime) {
				System.out.println("Warning: Mean TravelTime to short?");
				travelTimeInfo.link.setTravelTime(travelTimeInfo.freeSpeedTravelTime);
			}
			else travelTimeInfo.link.setTravelTime(meanTravelTime);

//			if (meanTravelTime < travelTimeInfo.freeSpeedTravelTime)
//			{
//				System.out.println("Warning: Mean TravelTime to short?");
//				travelTimeInfo.travelTime = travelTimeInfo.freeSpeedTravelTime;
//			}
//			else travelTimeInfo.travelTime = meanTravelTime;
//
//			travelTimeInfo.link.setTravelTime(travelTimeInfo.travelTime);
		}

	}	// ReplannerThread

}