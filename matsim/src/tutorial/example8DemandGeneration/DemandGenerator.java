/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package tutorial.example8DemandGeneration;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.basic.v01.population.BasicPopulationBuilder;
import org.matsim.api.basic.v01.population.BasicPopulationWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Point;

/**
 * This class generates a simple artificial MATSim demand for 
 * the german city Löbau. This is similar to the tutorial held 
 * at the MATSim user meeting 09 by glaemmel, however based on
 * the matsim api.
 * 
 * The files needed to run this tutorial are placed in the matsim examples
 * repository that can be found in the root directory of the matsim
 * sourceforge svn under the path matsimExamples/tutorial/example8DemandGeneration.
 * 
 * @author glaemmel
 * @author dgrether
 *
 */
public class DemandGenerator {

	private static final Logger log = Logger.getLogger(DemandGenerator.class);
	
	private static int ID = 0;

	private static final String exampleDirectory = "../matsimExamples/tutorial/example8DemandGeneration/";
	
	public static void main(String [] args) throws IOException {
		
		//out input files
		String netFile = exampleDirectory + "network.xml";
		String zonesFile = exampleDirectory + "zones.shp";
		
		BasicScenario scenario = new BasicScenarioImpl();
		
		FeatureSource fts = ShapeFileReader.readDataFile(zonesFile); //reads the shape file in
		Random rnd = new Random();
		
		Feature commercial = null;
		Feature recreation = null;

		//Iterator to iterate over the features from the shape file
		Iterator<Feature> it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = it.next(); //A feature contains a geometry (in this case a polygon) and an arbitrary number
			//of other attributes
			if (((String)ft.getAttribute("type")).equals("commercial")) {
				commercial = ft;
			} 
			else if (((String)ft.getAttribute("type")).equals("recreation")) {
				recreation = ft;
			} 
			else if (((String)ft.getAttribute("type")).equals("housing")) {
				long l = ((Long)ft.getAttribute("inhabitant"));
				createPersons(scenario, ft, rnd, (int) l); //creates l new persons and chooses for each person a random link
				//within the corresponding polygon. after this method call every new generated person has one plan and one home activity
			} 
			else {
				throw new RuntimeException("Unknown zone type:" + ft.getAttribute("type"));
			}
		}
		createActivities(scenario, rnd, recreation, commercial); //this method creates the remaining activities
		String popFilename = exampleDirectory + "population.xml";
		new BasicPopulationWriter(scenario.getPopulation()).write(popFilename); // and finally the population will be written to a xml file
		log.info("population written to: " + popFilename);
	}

	private static void createActivities(BasicScenario scenario, Random rnd,  Feature recreation, Feature commercial) {
		BasicPopulation<BasicPerson<BasicPlan>> pop =  scenario.getPopulation();
		BasicPopulationBuilder pb = pop.getPopulationBuilder(); //the population builder creates all we need 
		
		for (BasicPerson<BasicPlan> pers : pop.getPersons().values()) { //this loop iterates over all persons
			BasicPlan<BasicActivity> plan = pers.getPlans().get(0); //each person has exactly one plan, that has been created in createPersons(...)
			BasicActivity homeAct = plan.getPlanElements().get(0); //every plan has only one activity so far (home activity)
			homeAct.setEndTime(7*3600); // sets the endtime of this activity to 7 am

			BasicLeg leg = pb.createLeg(TransportMode.car);
			plan.addLeg(leg); // there needs to be a log between two activities
			
			//work activity on a random link within one of the commercial areas
			Point p = getRandomPointInFeature(rnd, commercial);
			BasicActivity work = pb.createActivityFromCoord("w", scenario.createCoord(p.getX(), p.getY()));
			double startTime = 8*3600;
			work.setStartTime(startTime);
			work.setEndTime(startTime + 6*3600);
			plan.addActivity(work);

			plan.addLeg(pb.createLeg(TransportMode.car));
			
			//recreation activity on a random link within one of the recreation area 
			p = getRandomPointInFeature(rnd, recreation);
			BasicActivity leisure = pb.createActivityFromCoord("l", scenario.createCoord(p.getX(), p.getY()));
			leisure.setEndTime(3600*19);
			plan.addActivity(leisure);

			plan.addLeg(pb.createLeg(TransportMode.car));
			
			//finally the second home activity - it is clear that this activity needs to be on the same link
			//as the first activity - since in this tutorial our agents do not relocate ;-)
			BasicActivity homeActII = pb.createActivityFromCoord("h", homeAct.getCoord());
			plan.addActivity(homeActII);
		}

	}

	private static void createPersons(BasicScenario scenario, Feature ft, Random rnd, int number) {
		BasicPopulation<BasicPerson<BasicPlan<?>>> pop = scenario.getPopulation();
		BasicPopulationBuilder pb = pop.getPopulationBuilder();
		for (; number > 0; number--) {
			BasicPerson<BasicPlan<?>> pers = pb.createPerson(scenario.createId(Integer.toString(ID++)));
			pop.getPersons().put(pers.getId(), pers);
			BasicPlan<?> plan = pb.createPlan(pers);
			Point p = getRandomPointInFeature(rnd, ft);
			BasicActivity act = pb.createActivityFromCoord("h", scenario.createCoord(p.getX(), p.getY()));
			plan.addActivity(act);
			pers.getPlans().add(plan);
		}
	}
	
	private static Point getRandomPointInFeature(Random rnd, Feature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (ft.getDefaultGeometry().contains(p));		
		return p;
	}


}
