module beast.app {
	
	// module depends on beast.pkgmgmt and beast.base
	requires beast.pkgmgmt;
	requires beast.base;
	
	// standard module dependencies
	requires java.base;
	requires java.desktop;
	requires java.logging;

	
	// external libraries from lib folder
	requires beagle;
	requires antlr.runtime;
	requires colt;
	requires jam;
	
	// libraries customised for BEAST 2 from build/dist folder
	requires json;
	requires commons.math;

	exports beast.app.inputeditor;
	exports beast.app.beastapp;
	exports beast.app.tools;
	exports beast.app.util;
	exports beast.app.packagemanager;
	exports beast.app.treeannotator;
	exports beast.app.seqgen;
	exports beast.app.draw;
	exports beast.app.beauti;

	
	uses beast.app.inputeditor.InputEditor;
	provides beast.app.inputeditor.InputEditor with
		beast.app.inputeditor.AlignmentListInputEditor,
		beast.app.inputeditor.BEASTObjectInputEditor,
		beast.app.inputeditor.BooleanInputEditor,
		beast.app.inputeditor.DoubleInputEditor,
		beast.app.inputeditor.DoubleListInputEditor,
		beast.app.inputeditor.EnumInputEditor,
		beast.app.inputeditor.FileInputEditor,
		beast.app.inputeditor.FileListInputEditor,
		beast.app.inputeditor.IntegerInputEditor,
		beast.app.inputeditor.IntegerListInputEditor,
		beast.app.inputeditor.ListInputEditor,
		beast.app.inputeditor.LogFileInputEditor,
		beast.app.inputeditor.LogFileListInputEditor,
		beast.app.inputeditor.LoggerListInputEditor,
		beast.app.inputeditor.LongInputEditor,
		beast.app.inputeditor.MRCAPriorInputEditor,
		beast.app.inputeditor.OutFileInputEditor,
		beast.app.inputeditor.OutFileListInputEditor,
		beast.app.inputeditor.ParameterInputEditor,
		beast.app.inputeditor.ParametricDistributionInputEditor,
		beast.app.inputeditor.SiteModelInputEditor,
		beast.app.inputeditor.StringInputEditor,
		beast.app.inputeditor.TaxonSetInputEditor,
		beast.app.inputeditor.TaxonSetListInputEditor,
		beast.app.inputeditor.TipDatesInputEditor,
		beast.app.inputeditor.TreeFileInputEditor,
		beast.app.inputeditor.TreeFileListInputEditor,
		beast.app.inputeditor.XMLFileInputEditor,
		beast.app.inputeditor.XMLFileListInputEditor,
		beast.app.beauti.ClockModelListInputEditor,
		beast.app.beauti.ConstantPopulationInputEditor,
		beast.app.beauti.FrequenciesInputEditor,
		beast.app.beauti.GeneTreeForSpeciesTreeDistributionInputEditor,
		beast.app.beauti.OperatorListInputEditor,
		beast.app.beauti.PriorInputEditor,
		beast.app.beauti.PriorListInputEditor,
		beast.app.beauti.SpeciesTreePriorInputEditor,
		beast.app.beauti.StateNodeInitialiserListInputEditor,
		beast.app.beauti.StateNodeListInputEditor,
		beast.app.beauti.TreeDistributionInputEditor
	;
	
	opens beast.app.draw.icons;
}