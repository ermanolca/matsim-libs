/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.snowball.spatial;

import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.snowball.SampledGraph;


/**
 * Representation of a snowball sampled spatial graph.
 * 
 * @author illenberger
 *
 */
public interface SampledSpatialGraph extends SampledGraph, SpatialGraph {

	/**
	 * @see {@link SpatialGraph#getVertices()}
	 * @see {@link SampledGraph#getVertices()}
	 */
	public Set<? extends SampledSpatialVertex> getVertices();

	/**
	 * @see {@link SpatialGraph#getEdges()}
	 * @see {@link SampledGraph#getEdges()}
	 */
	public Set<? extends SampledSpatialEdge> getEdges();
	
}
