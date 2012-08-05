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

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.slaves.SlaveComputer;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * A Builder that executes structure for one population.
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @see StructureBuilder
 * @since 0.1
 */
public class StructurePopulationBuilder extends Builder {
	/**
	 * Name of structure project.
	 */
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
	 * Constructor with args, called from Jelly.
	 * @param structureProject
	 * @param structure
	 * @param maxPops
	 * @param numLoci
	 * @param numInds
	 * @param inFile
	 * @param outFile
	 * @param mainParams
	 * @param extraParams
	 */
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
	/**
	 * Calls structure for a certain K. It copies the input files needed from 
	 * the project that triggered the build (with the StructureBuilder) and 
	 * prepares a call to structure tool. Finally, it copies back the output 
	 * file back to the project that triggered the build.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("Launching structure for K " + this.maxPops);

		// prepare workspace
		final FilePath workspace = build.getWorkspace();
		listener.getLogger().println("Using workspace " + workspace.getRemote());
		if(Computer.currentComputer() instanceof SlaveComputer) {
			listener.getLogger().println("Running on slave " + Computer.currentComputer().getName());
		} else {
			listener.getLogger().println("Running on master");
		}
		if(!workspace.exists()) {
			listener.getLogger().println("Creating workspace at " + workspace.getRemote());
			workspace.mkdirs();
			if(!workspace.exists()) {
				throw new AbortException("Couldn't create workspace");
			}
		}
		
		// Prepare the project with structure files
		AbstractProject<?, ?> project = (AbstractProject<?, ?>) Hudson.getInstance().getItem(structureProject);
		if(project == null) {
			listener.getLogger().println("Non existing structure project, using this build's project as structure project");
			project = build.getProject(); // Use this project if no other was provided
		}
		//final FilePath structureWorkspace = new FilePath(new File(project.getRootDir(), "workspace"));
		if(!project.getLastBuild().isBuilding()) {
			throw new AbortException("Structure project is not building");
		}
		final FilePath structureWorkspace = project.getLastBuild().getWorkspace();
		
		// Copy structure files from other project to this project's workspace 
		// handling with care in case of a distributed env.
		listener.getLogger().println("Copying structure files from " + structureWorkspace.getRemote());
		this.copyStructureFiles(workspace, structureWorkspace);
		
		// Prepare the command line arguments, and then execute it!
		final ArgumentListBuilder args = this.createStructureArgs(workspace); 
		Map<String, String> env = build.getEnvironment(listener);
		final Integer exitCode = launcher.launch().cmds(args).envs(env)
				.stdout(listener).pwd(build.getModuleRoot()).join();
		listener.getLogger().println("Preparing to execute structure. Command line args: " + args.toStringWithQuote());

		if (exitCode != 0) {
			listener.getLogger().println(
					"Error executing Structure. Exit code : " + exitCode);
			return Boolean.FALSE;
		} else {
			// If the command was executed with success, send the outfile back to the master
			FilePath outFileFilePath = new FilePath(workspace, outFile+"_f");
			if(outFileFilePath.exists()) {
				FilePath structureOutputFilePath = new FilePath(structureWorkspace, "structure_run_output");
				if(!structureOutputFilePath.exists()) {
					structureOutputFilePath.mkdirs();
				}
				outFileFilePath.copyTo(new FilePath(structureOutputFilePath, outFile+"_run_"+this.maxPops+"_f"));
			}
			
			listener.getLogger().println("Successfully executed Structure.");
			return Boolean.TRUE;
		}
	}
	/**
	 * Copies structure files.
	 * @param workspace
	 * @param structureWorkspace
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void copyStructureFiles(FilePath workspace, FilePath structureWorkspace) throws IOException, InterruptedException {
		// Copying mainparams
		FilePath mainParamsFilePath = new FilePath(structureWorkspace, mainParams);
		FilePath localMainParamsFilePath = workspace.child(mainParams);
		mainParamsFilePath.copyTo(localMainParamsFilePath);
		
		// Copying extraparams
		FilePath extraParamsFilePath = new FilePath(structureWorkspace, extraParams);
		FilePath localExtraParamsFilePath = workspace.child(extraParams);
		extraParamsFilePath.copyTo(localExtraParamsFilePath);
		
		// Copying structure input data file
		FilePath inputFilePath = new FilePath(structureWorkspace, inFile);
		FilePath localInputFilePath = new FilePath(workspace, inFile);
		if(!localInputFilePath.getParent().equals(workspace)) {
			localInputFilePath.getParent().mkdirs();
		}
		inputFilePath.copyTo(localInputFilePath);
	}
	/**
	 * Creates structure args.
	 * @param workspace 
	 * @return ArgumentListBuilder
	 */
	private ArgumentListBuilder createStructureArgs(FilePath workspace) {
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(structure.getPathToExecutable());
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
		return args;
	}
	/**
	 * Throws UnsupportedOperationException, as TestBuilder.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Descriptor<Builder> getDescriptor() {
		throw new UnsupportedOperationException();
	}
//	/**
//	 * Le builder of Structure Population Builder.
//	 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
//	 * @since 0.1
//	 */
//	@Extension
//	public static class DescriptorImpl extends Descriptor<Builder> {
//		public DescriptorImpl() {
//			super(StructurePopulationBuilder.class);
//			load();
//		}
//		/* (non-Javadoc)
//		 * @see hudson.model.Descriptor#getDisplayName()
//		 */
//		@Override
//		public String getDisplayName() {
//			return "Execute Structure for a population (K)";
//		}
//		
//	}
}
