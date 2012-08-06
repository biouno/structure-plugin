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
 * A parser of mainparams that replaces certain fields. Written specifically 
 * for this plug-in, may not be useful for other projects.
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class MainParamsParser implements Serializable {
	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 4974596043369293623L;
	/*
	 * Constants used for replacing parameters.
	 */
	private static final String MAXPOPS = "MAXPOPS";
	private static final String NUMLOCI = "NUMLOCI";
	private static final String NUMINDS = "NUMINDS";
	private static final String BURNIN = "BURNIN";
	private static final String NUMREPS = "NUMREPS";
	private static final String INFILE = "INFILE";
	private static final String OUTFILE = "OUTFILE";
	/*
	 * Constants used for filling the new mainparams file (after the variables 
	 * replacement).
	 */
	private static final String NEWLINE_UNIX_TOKEN = "\n";
	private static final String DEFINE_TOKEN = "#define ";
	/**
	 * Regular expression used for finding mainparams entries. 
	 * <p>
	 * An example line: #define LABEL 1
	 */
	private static final String REGEX = "(\\t*|\\s*)?#(\\t*|\\s*)?define(\\t*|\\s*)+(\\w+)(\\t*|\\s*)+(.*)";
	/**
	 * The pattern used for REGEX parsing.
	 */
	private final Pattern pattern = Pattern.compile(REGEX);
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
	 * Constructor with args.
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
	/**
	 * Parses a mainparams file content, replacing certain fields specified 
	 * during the construction of this parser. The K value is used in the same 
	 * way, but may differ from one mainparam file to the other, so that's 
	 * why this field is included in this method signature.
	 * @param mainparamsContent mainparams file content
	 * @param k K
	 * @return mainparams file with updated values
	 * @throws ParserException
	 */
	public String parse(String mainparamsContent, int k) throws ParserException {
		final StringBuilder sb = new StringBuilder();
		for (String line : mainparamsContent.split(NEWLINE_UNIX_TOKEN)) {
			final Matcher matcher = pattern.matcher(line);
			if(matcher.matches()) {
				String name = matcher.group(4); 
				//String value = matcher.group(5);
				if(name.trim().equals(MAXPOPS)) {
					sb.append(DEFINE_TOKEN + name + " " + k + NEWLINE_UNIX_TOKEN);
				} else if(name.trim().equals(NUMLOCI)) {
					sb.append(DEFINE_TOKEN + name + " " + this.numLoci + NEWLINE_UNIX_TOKEN);
				} else if(name.trim().equals(NUMINDS)) {
					sb.append(DEFINE_TOKEN + name + " " + this.numInds + NEWLINE_UNIX_TOKEN);
				} else if(name.trim().equals(BURNIN)) {
					sb.append(DEFINE_TOKEN + name + " " + this.burnIn + NEWLINE_UNIX_TOKEN);
				} else if(name.trim().equals(NUMREPS)) {
					sb.append(DEFINE_TOKEN + name + " " + this.numReps + NEWLINE_UNIX_TOKEN);
				} else if(name.trim().equals(INFILE)) {
					sb.append(DEFINE_TOKEN + name + " " + this.inFile + NEWLINE_UNIX_TOKEN);
				} else if(name.trim().equals(OUTFILE)) {
					sb.append(DEFINE_TOKEN + name + " " + this.outFile + NEWLINE_UNIX_TOKEN);
				} else {
					sb.append(line + NEWLINE_UNIX_TOKEN);
				}
			}
		}
		return sb.toString();
	}
}
