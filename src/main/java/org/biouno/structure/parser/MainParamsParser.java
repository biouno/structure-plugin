/*
 * The MIT License
 *
 * Copyright (c) <2012> <Bruno P. Kinoshita>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.biouno.structure.parser;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class MainParamsParser implements Serializable {

	private static final long serialVersionUID = 4974596043369293623L;

	private static final String REGEX = "(\\t*|\\s*)?#(\\t*|\\s*)?define(\\t*|\\s*)+(\\w+)(\\t*|\\s*)+(.*)";
	
	private final Pattern pattern = Pattern.compile(REGEX);
	
	private static final String MAXPOPS = "MAXPOPS";
	private static final String NUMLOCI = "NUMLOCI";
	private static final String NUMINDS = "NUMINDS";
	private static final String BURNIN = "BURNIN";
	private static final String NUMREPS = "NUMREPS";
	private static final String INFILE = "INFILE";
	private static final String OUTFILE = "OUTFILE";

	private int line = 0;
	/**
	 * Number of loci in data file (NUMLOCI).
	 */
	private final Integer numLoci;
	/**
	 * Number of diploid individuals in data file (NUMINDS).
	 */
	private final Integer numInds;
	/**
	 * Length of burn-in period (BURNIN).
	 */
	private final Long burnIn;
	/**
	 * Number of MCMC steps after burn-in (NUMREPS).
	 */
	private final Long numReps;
	/**
	 * Name of input data file.
	 */
	private final String inFile;
	/**
	 * Name of output data file.
	 */
	private final String outFile;

	/**
	 * @param numLoci
	 * @param numInds
	 * @param burnIn
	 * @param numReps
	 * @param inFile
	 * @param outFile
	 */
	public MainParamsParser(Integer numLoci, Integer numInds,
			Long burnIn, Long numReps, String inFile, String outFile) {
		super();
		this.numLoci = numLoci;
		this.numInds = numInds;
		this.burnIn = burnIn;
		this.numReps = numReps;
		this.inFile = inFile;
		this.outFile = outFile;
	}

	/**
	 * @return the line
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @param line
	 *            the line to set
	 */
	public void setLine(int line) {
		this.line = line;
	}

	/**
	 * @return the numLoci
	 */
	public Integer getNumLoci() {
		return numLoci;
	}

	/**
	 * @return the numInds
	 */
	public Integer getNumInds() {
		return numInds;
	}

	/**
	 * @return the burnIn
	 */
	public Long getBurnIn() {
		return burnIn;
	}

	/**
	 * @return the numReps
	 */
	public Long getNumReps() {
		return numReps;
	}

	/**
	 * @return the inFile
	 */
	public String getInFile() {
		return inFile;
	}

	/**
	 * @return the outFile
	 */
	public String getOutFile() {
		return outFile;
	}

	public String parse(String mainParams, int maxPops) throws ParserException {
		StringBuilder sb = new StringBuilder();
		for (String line : mainParams.split("\n")) {
			Matcher matcher = pattern.matcher(line);
			if(matcher.matches()) {
				String name = matcher.group(4); 
				//String value = matcher.group(5);
				if(name.trim().equals(MAXPOPS)) {
					sb.append("#define " + name + " " + maxPops + "\n");
				} else if(name.trim().equals(NUMLOCI)) {
					sb.append("#define " + name + " " + this.numLoci + "\n");
				} else if(name.trim().equals(NUMINDS)) {
					sb.append("#define " + name + " " + this.numInds + "\n");
				} else if(name.trim().equals(BURNIN)) {
					sb.append("#define " + name + " " + this.burnIn + "\n");
				} else if(name.trim().equals(NUMREPS)) {
					sb.append("#define " + name + " " + this.numReps + "\n");
				} else if(name.trim().equals(INFILE)) {
					sb.append("#define " + name + " " + this.inFile + "\n");
				} else if(name.trim().equals(OUTFILE)) {
					sb.append("#define " + name + " " + this.outFile + "\n");
				} else {
					sb.append(line + "\n");
				}
			}
		}
		return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		String mainParams = "#define OUTFILE /home/kinow/java/biouno-workspace/biology-data/structure-bodega/sample_1/param_set_1/Results\n"
				+ "#define INFILE /home/kinow/java/biouno-workspace/biology-data/structure-bodega/sample_1/project_data\n"
				+ "#define NUMINDS 43\n"
				+ "#define NUMLOCI 6\n"
				+ "#define LABEL 1 \n"
				+ "#define POPDATA 0 \n"
				+ "#define POPFLAG 1 \n"
				+ "#define LOCDATA 0 \n"
				+ "#define PHENOTYPE 0 \n"
				+ "#define MARKERNAMES 0 \n"
				+ "#define MAPDISTANCES 0 \n"
				+ "#define ONEROWPERIND 0 \n"
				+ "#define PHASEINFO 0 \n"
				+ "#define PHASED 0 \n"
				+ "#define RECESSIVEALLELES 0 \n"
				+ "#define EXTRACOLS 0\n"
				+ "#define MISSING -9\n"
				+ "#define PLOIDY 2\n"
				+ "#define MAXPOPS 2\n"
				+ "#define BURNIN 10000\n"
				+ "#define NUMREPS 20000\n"
				+ "\n"
				+ "\n"
				+ "#define NOADMIX 0\n"
				+ "#define LINKAGE 0\n"
				+ "#define USEPOPINFO 0\n"
				+ "\n"
				+ "#define LOCPRIOR 0\n"
				+ "#define INFERALPHA 1\n"
				+ "#define ALPHA 1.0\n"
				+ "#define POPALPHAS 0 \n"
				+ "#define UNIFPRIORALPHA 1 \n"
				+ "#define ALPHAMAX 10.0\n"
				+ "#define ALPHAPROPSD 0.025\n"
				+ "\n"
				+ "\n"
				+ "#define FREQSCORR 1 \n"
				+ "#define ONEFST 0\n"
				+ "#define FPRIORMEAN 0.01\n"
				+ "#define FPRIORSD 0.05\n"
				+ "\n"
				+ "\n"
				+ "#define INFERLAMBDA 0 \n"
				+ "#define LAMBDA 1.0\n"
				+ "#define COMPUTEPROB 1 \n"
				+ "#define PFROMPOPFLAGONLY 0 \n"
				+ "#define ANCESTDIST 0 \n"
				+ "#define STARTATPOPINFO 0 \n"
				+ "#define METROFREQ 10\n"
				+ "\n" + "\n" + "#define UPDATEFREQ 1 \n" + "";
		System.out.println(new MainParamsParser(2000, 3000, 4000L, 5000L, "oi", "tchau").parse(mainParams, 10));
	}

}
