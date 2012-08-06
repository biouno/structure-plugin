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
import org.biouno.structure.util.Messages;

/**
 * A builder that executes structure for a K parameter. This builder is 
 * used by {@link StructureBuilder StructureBuilder}, and has no user 
 * interface. 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @see StructureBuilder
 * @since 0.1
 */
class StructureKBuilder extends Builder {
	/*
	 * Structure command line option constants.
	 */
	private static final String MAINPARAMS_OPTION = "-m";
	private static final String EXTRAPARAMS_OPTION = "-e";
	private static final String MAXPOPS_OPTION = "-K";
	private static final String NUMLOCI_OPTION = "-L";
	private static final String NUMINDS_OPTION = "-N";
	private static final String INFILE_OPTION = "-i";
	private static final String OUTFILE_OPTION = "-o";
	/*
	 * Structure constants used for the output file name.
	 */
	private static final String STRUCTURE_RUN = "_run_";
	public static final String STRUCTURE_RUN_OUTPUT_DIRECTORY = "structure_run_output";
	private static final String STRUCTURE_OUTPUT_FILE_SUFFIX = "_f";
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
	public StructureKBuilder(String structureProject, StructureInstallation structure,
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
		listener.getLogger().println(Messages.StructureKBuilder_InvokingStructureK(this.maxPops));

		// prepare workspace
		final FilePath workspace = build.getWorkspace();
		listener.getLogger().println(Messages.StructureKBuilder_DisplayWorkspace(workspace.getRemote()));
		if(Computer.currentComputer() instanceof SlaveComputer) {
			listener.getLogger().println(Messages.StructureKBuilder_DisplaySlave(Computer.currentComputer().getName()));
		} else {
			listener.getLogger().println(Messages.StructureKBuilder_DisplayMaster());
		}
		if(!workspace.exists()) {
			listener.getLogger().println(Messages.StructureKBuilder_CreateWorkspace(workspace.getRemote()));
			workspace.mkdirs();
			if(!workspace.exists()) {
				throw new AbortException(Messages.StructureKBuilder_CreateWorkspaceError());
			}
		}

		// Prepare the project with structure files
		AbstractProject<?, ?> project = (AbstractProject<?, ?>) Hudson.getInstance().getItem(structureProject);
		if(project == null) {
			throw new AbortException(Messages.StructureKBuilder_MissingStructureProject());
		}
		//final FilePath structureWorkspace = new FilePath(new File(project.getRootDir(), "workspace"));
		if(!project.getLastBuild().isBuilding()) {
			throw new AbortException(Messages.StructureKBuilder_InvalidStructureProjectState());
		}
		final FilePath structureWorkspace = project.getLastBuild().getWorkspace();

		// Copy structure files from other project to this project's workspace 
		// handling with care in case of a distributed env.
		listener.getLogger().println(Messages.StructureKBuilder_CopyStructureFiles(structureWorkspace.getRemote()));
		this.copyStructureFiles(workspace, structureWorkspace);

		// Prepare the command line arguments, and then execute it!
		final ArgumentListBuilder args = this.createStructureArgs(workspace); 
		Map<String, String> env = build.getEnvironment(listener);
		final Integer exitCode = launcher.launch().cmds(args).envs(env)
				.stdout(listener).pwd(build.getModuleRoot()).join();
		listener.getLogger().println(Messages.StructureKBuilder_ExecuteStructure(args.toStringWithQuote()));

		if (exitCode != 0) {
			listener.getLogger().println(Messages.StructureKBuilder_ExecuteStructureError(exitCode));
			return Boolean.FALSE;
		} else {
			// If the command was executed with success, send the outfile back to the master
			FilePath outFileFilePath = new FilePath(workspace, outFile+STRUCTURE_OUTPUT_FILE_SUFFIX);
			if(outFileFilePath.exists()) {
				FilePath structureOutputFilePath = new FilePath(structureWorkspace, STRUCTURE_RUN_OUTPUT_DIRECTORY);
				if(!structureOutputFilePath.exists()) {
					structureOutputFilePath.mkdirs();
				}
				outFileFilePath.copyTo(new FilePath(structureOutputFilePath, outFile+STRUCTURE_RUN+this.maxPops+STRUCTURE_OUTPUT_FILE_SUFFIX));
			}
			
			listener.getLogger().println(Messages.StructureKBuilder_ExecuteStructureSuccess());
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
			args.add(MAINPARAMS_OPTION);
			args.add(mainParams);
		}
		if (StringUtils.isNotBlank(extraParams)) {
			args.add(EXTRAPARAMS_OPTION);
			args.add(extraParams);
		}
		if (maxPops != null && maxPops > 0) {
			args.add(MAXPOPS_OPTION);
			args.add(maxPops);
		}
		if (numLoci != null && numLoci > 0) {
			args.add(NUMLOCI_OPTION);
			args.add(numLoci);
		}
		if (numInds != null && numInds > 0) {
			args.add(NUMINDS_OPTION);
			args.add(numInds);
		}
		if (StringUtils.isNotBlank(inFile)) {
			args.add(INFILE_OPTION);
			args.add(new FilePath(workspace, inFile).getRemote());
		}
		if (StringUtils.isNotBlank(outFile)) {
			args.add(OUTFILE_OPTION);
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
}
