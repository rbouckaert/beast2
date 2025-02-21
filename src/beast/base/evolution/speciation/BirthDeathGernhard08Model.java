/*
 * BirthDeathGernhard08Model.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.base.evolution.speciation;


import static org.apache.commons.math.special.Gamma.logGamma;

import java.util.Arrays;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.parameter.RealParameter;

/* Ported from Beast 1.6
 * @author Joseph Heled
 *         Date: 24/02/2008
 */
@Description("Birth Death model based on Gernhard 2008. <br/>" +
        "This derivation conditions directly on fixed N taxa. <br/>" +
        "The inference is directly on b-d (strictly positive) and d/b (constrained in [0,1)) <br/>" +
        "Verified using simulated trees generated by Klass tree sample. (http://www.klaashartmann.com/treesample/) <br/>" +
        "Sampling proportion not verified via simulation. Proportion set by default to 1, an assignment which makes " +
        "the expressions identical to the expressions before the change.")

@Citation(value = "Gernhard 2008. The conditioned reconstructed process. Journal of Theoretical Biology Volume 253, " +
        "Issue 4, 21 August 2008, Pages 769-778",
        DOI = "doi:10.1016/j.jtbi.2008.04.005", // (https://doi.org/10.1016/j.jtbi.2008.04.005)
        year = 2008,
        firstAuthorSurname = "gernhard")

public class BirthDeathGernhard08Model extends YuleModel {

    final static String[] TYPES = {"unscaled", "timesonly", "oriented", "labeled"};

    final public Input<String> typeInput =
            new Input<>("type", "tree type, should be one of " + Arrays.toString(TYPES) + " (default unscaled)",
                    "unscaled", TYPES);
    final public Input<RealParameter> relativeDeathRateParameterInput =
            new Input<>("relativeDeathRate", "relative death rate parameter, mu/lambda in birth death model (turnover parameter)", Validate.REQUIRED);
    final public Input<RealParameter> sampleProbabilityInput =
            new Input<>("sampleProbability", "sample probability, rho in birth/death model");

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        final String typeName = typeInput.get().toLowerCase();
        if (typeName.equals("unscaled")) {
            type = TreeType.UNSCALED;
        } else if (typeName.equals("timesonly")) {
            type = TreeType.TIMESONLY;
        } else if (typeName.equals("oriented")) {
            type = TreeType.ORIENTED;
        } else if (typeName.equals("labeled")) {
            type = TreeType.LABELED;
        } else {
            throw new IllegalArgumentException("type '" + typeName + "' is not recognized. Should be one of unscaled, timesonly, oriented and labeled.");
        }
    }

    @Override
    public double calculateTreeLogLikelihood(final TreeInterface tree) {
        final double a = relativeDeathRateParameterInput.get().getValue();
        final double rho = (sampleProbabilityInput.get() == null ? 1.0 : sampleProbabilityInput.get().getValue());
        return calculateTreeLogLikelihood(tree, rho, a);
    }

    private TreeType type;

    public enum TreeType {
        UNSCALED,     // no coefficient 
        TIMESONLY,    // n!
        ORIENTED,     // n
        LABELED,      // 2^(n-1)/(n-1)!
                      // conditional on root: 2^(n-1)/n!(n-1)
                      // conditional on origin: 2^(n-1)/n!
    }

    /**
     * scaling coefficient of tree *
     */
    @Override
	protected double logCoeff(final int taxonCount) {
        switch (type) {
            case UNSCALED:
                break;
            case TIMESONLY:
                return logGamma(taxonCount + 1);
            case ORIENTED:
                return Math.log(taxonCount);
            case LABELED: {
                final double two2nm1 = (taxonCount - 1) * Math.log(2.0);
                if (conditionalOnRoot) {
                    return two2nm1 - Math.log(taxonCount - 1) - logGamma(taxonCount + 1);
                } else if (conditionalOnOrigin) {
                	return two2nm1 - logGamma(taxonCount + 1);
                } else {
                    return two2nm1 - logGamma(taxonCount);
                }
            }
        }
        return 0.0;
    }

//    @Override
//    public boolean includeExternalNodesInLikelihoodCalculation() {
//        return true;
//    }

    @Override
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation() || relativeDeathRateParameterInput.get().somethingIsDirty() || sampleProbabilityInput.get().somethingIsDirty();
    }
} // class BirthDeathGernhard08Model
