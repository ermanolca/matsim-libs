package org.matsim.lanes.basic;

import java.util.List;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

public interface BasicLaneDefinitions extends MatsimToplevelContainer {

	/**
	 * @deprecated use the map instead 
	 */
	@Deprecated
	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignmentsList();

	/**
	 * 
	 * @return Map with Link Ids as keys and assignments as values
	 */
	public SortedMap<Id, BasicLanesToLinkAssignment> getLanesToLinkAssignments();
	
	/**
	 * @param assignment
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment);

	public BasicLaneDefinitionsFactory getFactory();

}