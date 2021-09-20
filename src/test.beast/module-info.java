module test.beast {
	
	// module depends on beast.pkgmgmt and beast.base
	requires beast.pkgmgmt;
	requires beast.base;
	requires beast.app;
	
	// standard module dependencies
	requires java.base;
	requires java.desktop;
	requires java.logging;
	
	// external libraries from lib folder
	requires beagle;
	requires antlr.runtime;
	requires colt;
	requires jam;
	requires junit;
	requires fest;
	
	// libraries customised for BEAST 2 from build/dist folder
	requires json;
	requires commons.math;
	
	// exports required to run tests inside Ecplipse
	exports test.beast.core.util;
	exports test.beast.core.parameter;
	exports test.beast.core;
	exports test.beast.app.tools;
	exports test.beast.app.beauti;
	exports test.beast.app;
	exports test.beast.util;
	exports test.beast.integration;
	exports test.beast.math.distributions;
	exports test.beast.evolution.substmodel;
	exports test.beast.evolution.tree.coalescent;
	exports test.beast.evolution.tree;
	exports test.beast.evolution.likelihood;
	exports test.beast.evolution.datatype;
	exports test.beast.evolution.alignment;
	exports test.beast.evolution.speciation;
	exports test.beast.evolution.operator;
	exports test.beast.statistic;
	exports test.beast.beast2vs1.trace;
	exports test.beast.beast2vs1.tutorials;
	exports test.beast.beast2vs1;

}