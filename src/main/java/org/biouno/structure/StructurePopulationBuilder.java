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
package org.biouno.structure;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A Builder that executes structure for one population.
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class StructurePopulationBuilder extends Builder {
	private final String structureProject;
	/**
	 * Structure installation.
	 */
	private final StructureInstallation structure;
	/**
	 * Number of populations assumed (MAXPOPS).
	 */
	private final Integer maxPops;
	/**
	 * Number of loci in data file (NUMLOCI).
	 */
	private final Integer numLoci;
	/**
	 * Number of diploid individuals in data file (NUMINDS).
	 */
	private final Integer numInds;
	/**
	 * Name of input data file (INFILE).
	 */
	private final String inFile;
	/**
	 * Name of output data file (OUTFILE).
	 */
	private final String outFile;
	/**
	 * Main parameters (mainparams).
	 */
	private final String mainParams;
	/**
	 * Extra parameters (extraparams).
	 */
	private final String extraParams;

	/**
	 * @param structure
	 * @param maxPops
	 * @param numLoci
	 * @param numInds
	 * @param burnIn
	 * @param numReps
	 * @param inFile
	 * @param outFile
	 * @param mainParams
	 * @param extraParams
	 */
	@DataBoundConstructor
	public StructurePopulationBuilder(String structureProject, StructureInstallation structure,
			Integer maxPops, Integer numLoci, Integer numInds, String inFile, 
			String outFile, String mainParams, String extraParams) {
		super();
		this.structureProject = structureProject;
		this.structure = structure;
		this.maxPops = maxPops;
		this.numLoci = numLoci;
		this.numInds = numInds;
		this.inFile = inFile;
		this.outFile = outFile;
		this.mainParams = mainParams;
		this.extraParams = extraParams;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
	 * , hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		// create command to be executed
		listener.getLogger().println("Launching Structure...");

		FilePath workspace = build.getWorkspace();
		
		if(!workspace.exists()) {
			workspace.mkdirs();
		}
		
		AbstractProject<?, ?> project = (AbstractProject<?, ?>) Hudson.getInstance().getItem(structureProject);
		
		FilePath structureWorkspace = new FilePath(new File(project.getRootDir(), "workspace"));
		FilePath mainParamsFilePath = new FilePath(structureWorkspace, mainParams);
		FilePath extraParamsFilePath = new FilePath(structureWorkspace, extraParams);
		FilePath inputFilePath = new FilePath(structureWorkspace, inFile);
		
		FilePath localMainParamsFilePath = workspace.child(mainParams);
		mainParamsFilePath.copyTo(localMainParamsFilePath);
		
		FilePath localExtraParamsFilePath = workspace.child(extraParams);
		extraParamsFilePath.copyTo(localExtraParamsFilePath);
		
		FilePath localInputFilePath = new FilePath(workspace, inFile);
		if(!localInputFilePath.getParent().equals(workspace)) {
			localInputFilePath.getParent().mkdirs();
		}
		inputFilePath.copyTo(localInputFilePath);
		
		final String command = structure.getPathToExecutable();
		final ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(command);

		if (StringUtils.isNotBlank(mainParams)) {
			args.add("-m");
			args.add(mainParams);
		}
		if (StringUtils.isNotBlank(extraParams)) {
			args.add("-e");
			args.add(extraParams);
		}
		if (maxPops != null && maxPops > 0) {
			args.add("-K");
			args.add(maxPops);
		}
		if (numLoci != null && numLoci > 0) {
			args.add("-L");
			args.add(numLoci);
		}
		if (numInds != null && numInds > 0) {
			args.add("-N");
			args.add(numInds);
		}
		if (StringUtils.isNotBlank(inFile)) {
			args.add("-i");
			args.add(new FilePath(workspace, inFile).getRemote());
		}
		if (StringUtils.isNotBlank(outFile)) {
			args.add("-o");
			args.add(new FilePath(workspace, outFile).getRemote());
		}

		Map<String, String> env = build.getEnvironment(listener);

		final Integer exitCode = launcher.launch().cmds(args).envs(env)
				.stdout(listener).pwd(build.getModuleRoot()).join();

		if (exitCode != 0) {
			listener.getLogger().println(
					"Error executing Structure. Exit code : " + exitCode);
			return Boolean.FALSE;
		} else {
			listener.getLogger().println("Successfully executed Structure.");
			return Boolean.TRUE;
		}
		
		// TODO: send the outfile back to the master
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<Builder> {

		public DescriptorImpl() {
			super(StructurePopulationBuilder.class);
			load();
		}
		
		/* (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "Execute Structure for a population (K)";
		}
		
	}

}
