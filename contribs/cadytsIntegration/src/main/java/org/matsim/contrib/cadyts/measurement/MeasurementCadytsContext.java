/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsPlanStrategy.java
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

package org.matsim.contrib.cadyts.measurement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.distribution.DistributionPlanToPlanStepBasedOnEvents;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.counts.Counts;
//import org.matsim.counts.MatsimCountsReader;





import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.Plan;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
//public class CadytsContextDistributionBased implements CadytsContextI<Link>, StartupListener, IterationEndsListener, BeforeMobsimListener {
//public class CadytsContextDistributionBased_V2 implements CadytsContextI<Integer>, StartupListener, IterationEndsListener, BeforeMobsimListener {
public class MeasurementCadytsContext implements CadytsContextI<Measurement>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	private final static Logger log = Logger.getLogger(MeasurementCadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";
	
//	private final double countsScaleFactor;
//	private final Counts counts;
	private final Measurements measurements;	
//	private final TreeMap<Integer, Integer> measurementsMap; // idea
	private final boolean writeAnalysisFile;

//	private AnalyticalCalibrator<Link> calibrator;
//	private AnalyticalCalibrator<Integer> calibrator;
	private AnalyticalCalibrator<Measurement> calibrator;
//	private PlanToPlanStepBasedOnEvents planToPlanStep;
	private MeasurementPlanToPlanStepBasedOnEvents planToPlanStep;
	private SimResultsContainerImpl simResults;
	
//	public CadytsContextDistributionBased(Config config, Counts counts ) {
	public MeasurementCadytsContext(Config config, Measurements measurements) {
//	public CadytsContextDistributionBased_V2(Config config, TreeMap<Integer, Integer> measurementsMap) { // idea
		
//		this.countsScaleFactor = config.counts().getCountsScaleFactor();

//		CadytsConfigGroup cadytsConfig = new CadytsConfigGroup();
		GenericCadytsConfigGroup<Measurement> cadytsConfig = new GenericCadytsConfigGroup<>(Measurement.class);
		config.addModule(cadytsConfig);
		// addModule() also initializes the config group with the values read from the config file
		cadytsConfig.setWriteAnalysisFile(true);
		
//		if ( counts==null ) {
//			this.counts = new Counts();
//			String occupancyCountsFilename = config.counts().getCountsFileName();
//			new MatsimCountsReader(this.counts).readFile(occupancyCountsFilename);
//		} else {
//			this.counts = counts ;
//		}
		this.measurements = measurements;
//		this.measurementsMap = measurementsMap;
		
//		Set<Id<Link>> countedLinks = new TreeSet<>();
//		for (Id<Link> id : this.counts.getCounts().keySet()) {
//			countedLinks.add(id);
//		}
//		
//		cadytsConfig.setCalibratedItems(countedLinks);
		Set<Id<Measurement>> measurementsSet = new TreeSet<>();
		for (Id<Measurement> id : this.measurements.getMeasurements().keySet()) {
			measurementsSet.add(id);
		}
		
		cadytsConfig.setCalibratedItems(measurementsSet);
		// cadytsConfig.setCalibratedItems(distribution); // idea
		
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}
	
	public MeasurementCadytsContext(Config config ) {
		this( config, null ) ;
	}

	@Override
//	public PlansTranslator<Link> getPlansTranslator() {
//	public PlansTranslator<Integer> getPlansTranslator() {
	public PlansTranslator<Measurement> getPlansTranslator() {
		return this.planToPlanStep;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Scenario scenario = event.getControler().getScenario();
		
//		VolumesAnalyzer volumesAnalyzer = event.getControler().getVolumes();
		TravelDistanceAnalyzer travelDistanceAnalyzer = new TravelDistanceAnalyzer(3600, 3600*36, scenario.getNetwork());
		event.getControler().getEvents().addHandler(travelDistanceAnalyzer);
//		Map<Integer, Double> travelDistanceMap = new HashMap<Integer, Double>();
		
//		this.simResults = new SimResultsContainerImpl(volumesAnalyzer, this.countsScaleFactor);
		this.simResults = new SimResultsContainerImpl(travelDistanceAnalyzer);
//		this.simResults = new SimResultsContainerImpl(travelDistanceMap);
		
		// this collects events and generates cadyts plans from it
		this.planToPlanStep = new MeasurementPlanToPlanStepBasedOnEvents(scenario);
		event.getControler().getEvents().addHandler(planToPlanStep);

		// ---------- 1st important Cadyts method is "calibrator.addMesurement"; in this implementation it is called by the "CadytsBuilder"
//		this.calibrator = CadytsBuilder.buildCalibrator(scenario.getConfig(), this.counts , new LinkLookUp(scenario) /*, cadytsConfig.getTimeBinSize()*/, Link.class);
		this.calibrator = CadytsBuilder.buildCalibrator_V2(scenario.getConfig(), this.measurements, new BinLookUp(this.measurements), Measurement.class);
//		this.calibrator = CadytsBuilder.buildCalibratorDistributionBased(scenario.getConfig(), this.measurementsMap);
	}

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// Register demand for this iteration with Cadyts.
		// Note that planToPlanStep will return null for plans which have never been executed.
		// This is fine, since the number of these plans will go to zero in normal simulations,
		// and Cadyts can handle this "noise". Checked this with Gunnar.
		// mz 2015
    	
    	// ---------- 2nd important Cadyts method is "analyzer.calcLinearPlanEffect"
        for (Person person : event.getControler().getScenario().getPopulation().getPersons().values()) {
//          Plan<Link> planSteps = this.planToPlanStep.getPlanSteps(person.getSelectedPlan());
//          Plan<Integer> planSteps = this.planToPlanStep.getPlanSteps(person.getSelectedPlan());
            Plan<Measurement> planSteps = this.planToPlanStep.getPlanSteps(person.getSelectedPlan());
			this.calibrator.addToDemand(planSteps);
        }
    }

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.writeAnalysisFile) {
			String analysisFilepath = null;
			if (isActiveInThisIteration(event.getIteration(), event.getControler())) {
				analysisFilepath = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			}
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}
		
		
		
		

		// ---------- 3rd important method "calibrator.afterNetworkLoading"
//		this.calibrator.afterNetworkLoading(this.simResults);
		this.calibrator.afterNetworkLoading(simResults);

		// write some output
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		// TODO writing does not work currently; reactivate this when other stuff has been sorted out
//		try {
////			new CadytsCostOffsetsXMLFileIO<Link>(new LinkLookUp(event.getControler().getScenario()), Link.class)
//			new CadytsCostOffsetsXMLFileIO<Double>(new DistributionBinLookUp(), Double.class)
//   			   .write(filename, this.calibrator.getLinkCostOffsets());
//		} catch (IOException e) {
//			log.error("Could not write link cost offsets!", e);
//		}
	}

	/**
	 * for testing purposes only
	 */
	@Override
//	public AnalyticalCalibrator<Link> getCalibrator() {
//	public AnalyticalCalibrator<Integer> getCalibrator() {
	public AnalyticalCalibrator<Measurement> getCalibrator() {
		return this.calibrator;
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	@SuppressWarnings("static-method")
	private boolean isActiveInThisIteration(final int iter, final Controler controler) {
		return (iter > 0 && iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
//		return (iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
	}
		
	
//	/*package*/ static class SimResultsContainerImpl implements SimResults<Link> {
//	/*package*/ static class SimResultsContainerImpl implements SimResults<Integer> {
	/*package*/ static class SimResultsContainerImpl implements SimResults<Measurement> {
		private static final long serialVersionUID = 1L;
//		private final VolumesAnalyzer volumesAnalyzer;
		private final TravelDistanceAnalyzer travelDistanceAnalyzer;
//		private final double countsScaleFactor;
//		private final Map<Integer, Integer> travelDistanceMap;
//		private final Map<Id<Measurement>, Integer[]> travelDistanceMap;

//		SimResultsContainerImpl(final VolumesAnalyzer volumesAnalyzer, final double countsScaleFactor) {
//		SimResultsContainerImpl(final Map<Integer, Integer> travelDistanceMap) {
		SimResultsContainerImpl(final TravelDistanceAnalyzer travelDistanceAnalyzer) {
//		SimResultsContainerImpl(final Map<Id<Measurement>, Integer[]> travelDistanceMap) {
//			this.volumesAnalyzer = volumesAnalyzer;
			this.travelDistanceAnalyzer = travelDistanceAnalyzer;
//			this.countsScaleFactor = countsScaleFactor;
//			this.travelDistanceMap = travelDistanceMap;
		}

		@Override
//		public double getSimValue(final Link link, final int startTime_s, final int endTime_s, final TYPE type) { // stopFacility or link
//		public double getSimValue(final Integer link, final int startTime_s, final int endTime_s, final TYPE type) {
		public double getSimValue(final Measurement measurement, final int startTime_s, final int endTime_s, final TYPE type) {

//			Id<Link> linkId = link.getId();
			Id<Measurement> id = measurement.getMeasurementId();
//			double[] values = volumesAnalyzer.getVolumesPerHourForLink(linkId);
			double[] values = null; //travelDistanceAnalyzer.getVolumesPerHourForLink(id); // TODO
//			Integer[] values = this.travelDistanceMap.get(id);
			
			log.warn("bin = " + measurement + " -- value = " + values);

			if (values == null) {
				return 0;
			}

			int startHour = startTime_s / 3600;
			int endHour = (endTime_s-3599)/3600 ;
			// (The javadoc specifies that endTime_s should be _exclusive_.  However, in practice I find 7199 instead of 7200.  So
			// we are giving it an extra second, which should not do any damage if it is not used.) 
			if (endHour < startHour) {
				System.err.println(" startTime_s: " + startTime_s + "; endTime_s: " + endTime_s + "; startHour: " + startHour + "; endHour: " + endHour );
				throw new RuntimeException("this should not happen; check code") ;
			}
			double sum = 0. ;
			for ( int ii=startHour; ii<=endHour; ii++ ) {
				sum += values[startHour] ;
			}
			switch(type){
			case COUNT_VEH:
				return sum;
			case FLOW_VEH_H:
				throw new RuntimeException(" not yet implemented") ;
			default:
				throw new RuntimeException("count type not implemented") ;
			}

		}


		// TODO
//		@Override
//		public String toString() {
//			final StringBuffer stringBuffer2 = new StringBuffer();
//			final String LINKID = "linkId: ";
//			final String VALUES = "; values:";
//			final char TAB = '\t';
//			final char RETURN = '\n';
//
//			for (Id linkId : this.volumesAnalyzer.getLinkIds()) { // Only occupancy!
//				StringBuffer stringBuffer = new StringBuffer();
//				stringBuffer.append(LINKID);
//				stringBuffer.append(linkId);
//				stringBuffer.append(VALUES);
//
//				boolean hasValues = false; // only prints stops with volumes > 0
//				int[] values = this.volumesAnalyzer.getVolumesForLink(linkId);
//
//				for (int ii = 0; ii < values.length; ii++) {
//					hasValues = hasValues || (values[ii] > 0);
//
//					stringBuffer.append(TAB);
//					stringBuffer.append(values[ii]);
//				}
//				stringBuffer.append(RETURN);
//				if (hasValues) stringBuffer2.append(stringBuffer.toString());
//			}
//			return stringBuffer2.toString();
//		}

	}
}