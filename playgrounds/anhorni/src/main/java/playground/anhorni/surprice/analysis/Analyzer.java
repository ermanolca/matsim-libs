/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import playground.anhorni.surprice.Surprice;

public class Analyzer {
	private ScenarioImpl scenario = null; 
	private Config config = null;
	private final static Logger log = Logger.getLogger(Analyzer.class);
	private double tt[] = new double[8]; 
	private double td[] = new double[8]; 
	private double tolltd[] = new double[8];
	private double utilities[] = new double[8];
	
	public static void main (final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		Analyzer analyzer = new Analyzer();
		String configFile = args[0];
		analyzer.init(configFile);
		analyzer.run();	
	}
	
	public void init(String configFile) {
		this.config = ConfigUtils.loadConfig(configFile);
		this.init(config);
	}
	
	public void init(Config config) {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
	}
	
	public void run() {
		String outPath = config.controler().getOutputDirectory();
		this.analyze(outPath);
		log.info("=================== Finished analyses ====================");
	}
		
	public void analyze(String outPath) {		
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());		
		new FacilitiesReaderMatsimV1(scenario).readFile(config.facilities().getInputFile());
						
		for (String day : Surprice.days) {
			log.info("Analyzing " + day + " --------------------------------------------");
			String plansFilePath = outPath + "/" + day + "/" + day + ".output_plans.xml.gz";
			MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
			populationReader.readFile(plansFilePath);						
			
			String eventsfile = outPath + "/" + day + "/ITERS/it.100/" + day + ".100.events.xml.gz";
			this.readEvents(eventsfile, day, config);
			
			double avgUtility = 0.0;
			CalcAverageTripLength tdCalculator = new CalcAverageTripLength(this.scenario.getNetwork());
			
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
//				tdCalculator.run(person.getSelectedPlan());
				avgUtility += person.getSelectedPlan().getScore() / this.scenario.getPopulation().getPersons().size();
			}
			this.td[Surprice.days.indexOf(day)] = tdCalculator.getAverageTripLength();	
			this.utilities[Surprice.days.indexOf(day)] = avgUtility;
			
			this.scenario.getPopulation().getPersons().clear();
		}	
		this.write(outPath);
	}
	
	private void readEvents(String eventsfile, String day, Config config) {
		EventsManager events = EventsUtils.createEventsManager();
		
		CalcLegTimes ttCalculator = new CalcLegTimes();
		events.addHandler(ttCalculator);
		
		RoadPricingSchemeImpl scheme = (RoadPricingSchemeImpl)this.scenario.getRoadPricingScheme();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		
		log.info(config.scenario().isUseRoadpricing());
		scheme.setName("test");
		
		
		try {
			rpReader.parse(config.roadpricing().getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		CalcAverageTolledTripLength tollCalculator = new CalcAverageTolledTripLength(this.scenario.getNetwork(), scheme);
		events.addHandler(tollCalculator);
		
		new MatsimEventsReader(events).readFile(eventsfile);

		this.tt[Surprice.days.indexOf(day)] = ttCalculator.getAverageTripDuration();
			
		// TODO:
		this.tolltd[Surprice.days.indexOf(day)] = tollCalculator.getAverageTripLength();		
	}
			
	private void write(String outPath) {
		DecimalFormat formatter = new DecimalFormat("0.00");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/summary.txt")); 
			
			bufferedWriter.write("tt\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			String line = "tt\t";
			double avgTT = 0.0;
			for (String day : Surprice.days) {	
				double tt = this.tt[Surprice.days.indexOf(day)];
				line += formatter.format(tt) + "\t";			
				avgTT += tt / Surprice.days.size();
			}	
			line += formatter.format(avgTT) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
			bufferedWriter.write("td\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			line = "td\t";
			double avgTD = 0.0;
			for (String day : Surprice.days) {	
				double td = this.td[Surprice.days.indexOf(day)];
				line += formatter.format(td) + "\t";			
				avgTD += td / Surprice.days.size();
			}	
			line += formatter.format(avgTD) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
			bufferedWriter.write("tolltd\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			line = "tolltd\t";
			double avgTollTD = 0.0;
			for (String day : Surprice.days) {	
				double tolltd = this.tolltd[Surprice.days.indexOf(day)];
				line += formatter.format(tolltd) + "\t";			
				avgTollTD += tolltd / Surprice.days.size();
			}	
			line += formatter.format(avgTollTD) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
			bufferedWriter.write("utility\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			line = "utility\t";
			double avgUtility = 0.0;
			for (String day : Surprice.days) {	
				double utility = this.utilities[Surprice.days.indexOf(day)];
				line += formatter.format(utility) + "\t";			
				avgUtility += utility / Surprice.days.size();
			}	
			line += formatter.format(avgUtility) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
