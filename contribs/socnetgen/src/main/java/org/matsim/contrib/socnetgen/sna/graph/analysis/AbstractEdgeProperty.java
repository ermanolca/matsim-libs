/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractEdgeProperty.java
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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Edge;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public abstract class AbstractEdgeProperty implements EdgeProperty {

	@Override
	public DescriptiveStatistics statistics(Set<? extends Edge> edges) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		TObjectDoubleHashMap<Edge> values = values(edges);
		TObjectDoubleIterator<Edge> it = values.iterator();
		
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			stats.addValue(it.value());
		}
		
		return stats;
	}

}