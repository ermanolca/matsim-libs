package playground.mmoyo.TransitSimulation;

import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;

/**
 * A sequence of PT-Acts and PT-Activities starts with a leg "walk to PT" and ends with a act "exit PT" and a walk leg 
 * This class deletes them and introduces a new empty PT leg instead
 */
public class SimplifyPtLegs {
	
	public SimplifyPtLegs(){
		
	}
	
	/**
	 * Identifies and marks ptActs
	 */
	public void run (PlanImpl plan){
		int i=0;
		List<Integer> ptElements = new ArrayList<Integer>();
		boolean marking =false;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				String actType = ((ActivityImpl)pe).getType();
				if (actType.equals("wait pt")) marking= true;
				if (marking)  ptElements.add(i);
				if (actType.equals("exit pt veh")){
					marking = false;
					deleteElements(plan, ptElements);
				}
			}
			i++;
		}
	}

	/**
	 * Deletes marked ptActs and their legs 
	 */
	private void deleteElements(PlanImpl plan, List<Integer> ptElements){
		LegImpl firstLeg = (LegImpl)plan.getPlanElements().get(ptElements.get(0)-1);
		LegImpl lastLeg = (LegImpl)plan.getPlanElements().get(ptElements.get(ptElements.size())+1);
		firstLeg.setMode(TransportMode.pt);
		firstLeg.setArrivalTime(lastLeg.getArrivalTime());
		firstLeg.setTravelTime(lastLeg.getArrivalTime() -firstLeg.getDepartureTime());
		for (Integer i : ptElements) {
			plan.removeActivity(i.intValue());
		}
	}
	
}