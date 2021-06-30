/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.analysis.emissions;

import org.apache.commons.math3.util.Pair;

import java.util.List;

class HBEFAEmissionConceptShares {
	static final List<Pair<String, Double>> HBEFA_PSNGCAR_SHARE_2020 = List.of(
			new Pair<>("petrol (4S)",  0.512744725),
			new Pair<>("diesel", 0.462841421),
			new Pair<>("electricity", 0.003288374),
			new Pair<>("bifuel CNG/petrol", 0.003857924),
			new Pair<>("Plug-in Hybrid petrol/electric", 0.005743608),
			new Pair<>("Plug-in Hybrid diesel/electric", 0.000142326),
			new Pair<>("bifuel LPG/petrol", 0.011381645));

	static final List<Pair<String, Double>> HBEFA_PSNGCAR_SHARE_2030 = List.of(
			new Pair<>("petrol (4S)",  0.49311316),
			new Pair<>("diesel", 0.334273875),
			new Pair<>("electricity", 0.065971114),
			new Pair<>("bifuel CNG/petrol", 0.018612623),
			new Pair<>("Plug-in Hybrid petrol/electric", 0.071173161),
			new Pair<>("Plug-in Hybrid diesel/electric", 0.005315198),
			new Pair<>("bifuel LPG/petrol", 0.011540868));

	static final List<Pair<String, Double>> HBEFA_HGV_SHARE_2020 = List.of(
			new Pair<>("diesel", 0.997777343),
			new Pair<>("CNG", 0.000599772),
			new Pair<>("electricity", 0.001256243),
			new Pair<>("LNG", 0.000366702));

	static final List<Pair<String, Double>> HBEFA_HGV_SHARE_2030 = List.of(
			new Pair<>("diesel", 0.987951279),
			new Pair<>("CNG", 0.001234634),
			new Pair<>("electricity", 0.009572712),
			new Pair<>("LNG", 0.001241331));

	static final List<Pair<String, Double>> HBEFA_LCV_SHARE_2020 = List.of(
			new Pair<>("petrol (4S)",  0.03909041),
			new Pair<>("diesel", 0.95276165),
			new Pair<>("electricity", 0.003650961),
			new Pair<>("bifuel CNG/petrol", 0.004064813),
			new Pair<>("Plug-in Hybrid petrol/electric", 0.000422853),
			new Pair<>("Plug-in Hybrid diesel/electric", 0.00000933));

	static final List<Pair<String, Double>> HBEFA_LCV_SHARE_2030 = List.of(
			new Pair<>("petrol (4S)",  0.038174488),
			new Pair<>("diesel", 0.922275901),
			new Pair<>("electricity", 0.018041067),
			new Pair<>("bifuel CNG/petrol", 0.00534465),
			new Pair<>("Plug-in Hybrid petrol/electric", 0.014704795),
			new Pair<>("Plug-in Hybrid diesel/electric", 0.001459154));

}
