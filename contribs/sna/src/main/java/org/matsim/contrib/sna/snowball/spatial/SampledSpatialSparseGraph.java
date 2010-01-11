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

import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Implementation of {@link SampledSpatialGraph} with a {@link SpatialSparseGraph}.
 * 
 * @author illenberger
 *
 */
public class SampledSpatialSparseGraph extends SpatialSparseGraph implements SampledSpatialGraph {

	/**
	 * Creates a new sampled spatial sparse graph with the given coordinate
	 * reference system.
	 * 
	 * @param crs
	 *            a coordinate reference system.
	 */
	public SampledSpatialSparseGraph(CoordinateReferenceSystem crs) {
		super(crs);
	}

	/**
	 * @see {@link SpatialSparseGraph#getEdges()}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SampledSpatialSparseEdge> getEdges() {
		return (Set<? extends SampledSpatialSparseEdge>) super.getEdges();
	}

	/**
	 * @see {@link SpatialSparseGraph#getVertices()}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SampledSpatialSparseVertex> getVertices() {
		return (Set<? extends SampledSpatialSparseVertex>) super.getVertices();
	}

	/**
	 * @see {@link SpatialSparseGraph#getEdge(SparseVertex, SparseVertex)}
	 */
	@Override
	public SampledSpatialSparseEdge getEdge(SparseVertex v1, SparseVertex v2) {
		return (SampledSpatialSparseEdge) super.getEdge(v1, v2);
	}

}
