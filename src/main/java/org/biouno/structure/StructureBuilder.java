package org.biouno.structure;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.labels.LabelAtom;
import hudson.tasks.Builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.biouno.structure.parser.MainParamsParser;
import org.biouno.structure.parser.ParserException;
import org.biouno.structure.util.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Structure builder.
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class StructureBuilder extends Builder {
	private static final Logger LOGGER = Logger.getLogger("org.biouno.structure");
	/*
	 * Constants used for creating structure files.
	 */
	private static final String MAINPARAMS_PARAM_SET_K_PREFIX = "mainparams.param_set.k";
	private static final String STRUCTURE_EXTRAMPARAMS_FILENAME = "extraparams";
	private static final String STRUCTURE_FILES_ENCODING = "UTF-8";
	/**
	 * Le builder extension.
	 */
	@Extension
	public static final StructureBuilderDescriptor DESCRIPTOR = new StructureBuilderDescriptor();
	/**
	 * Parses mainparams file, replacing values with the ones provided by the 
	 * user in the jelly form (config.jelly).
	 */
	private final MainParamsParser parser;
	/**
	 * Structure installation.
	 */
	private final String structureInstallationName;
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
	 * Length of burn-in period (BURNIN).
	 */
	private final Long burnIn;
	/**
	 * Number of MCMC steps after burn-in (NUMREPS).
	 */
	private final Long numReps;
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
	 * Slave label.
	 */
	private final String label;
	/**
	 * Keep jobs executed for each K. False by default.
	 */
	private final Boolean keepJobsForEachK;
	/**
	 * Constructor with args, called from Jelly populating the object properties
	 * from the form.
	 * @param structureInstallationName
	 * @param maxPops
	 * @param numLoci
	 * @param numInds
	 * @param burnIn
	 * @param numReps
	 * @param inFile
	 * @param outFile
	 * @param mainParams
	 * @param extraParams
	 * @param label
	 * @param keepJobsForEachK
	 */
	@DataBoundConstructor
	public StructureBuilder(String structureInstallationName, Integer maxPops,
			Integer numLoci, Integer numInds, Long burnIn, Long numReps,
			String inFile, String outFile, String mainParams,
			String extraParams, String label, Boolean keepJobsForEachK) {
		super();
		this.structureInstallationName = structureInstallationName;
		this.maxPops = maxPops;
		this.numLoci = numLoci;
		this.numInds = numInds;
		this.burnIn = burnIn;
		this.numReps = numReps;
		this.inFile = inFile;
		this.outFile = outFile;
		this.mainParams = mainParams;
		this.extraParams = extraParams;
		this.label = label;
		this.keepJobsForEachK = keepJobsForEachK;
		parser = new MainParamsParser(numLoci, numInds, burnIn, numReps, inFile, outFile);
	}
	/**
	 * @return the structureInstallationName
	 */
	public String getStructureInstallationName() {
		return structureInstallationName;
	}
	/**
	 * @return the maxPops
	 */
	public Integer getMaxPops() {
		return maxPops;
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
	 * @return the mainParams
	 */
	public String getMainParams() {
		return mainParams;
	}
	/**
	 * @return the extraParams
	 */
	public String getExtraParams() {
		return extraParams;
	}
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	/**
	 * @return the keepJobsForEachK
	 */
	public Boolean getKeepJobsForEachK() {
		return (keepJobsForEachK == null ? Boolean.FALSE : keepJobsForEachK);
	}
	/**
	 * Creates one mainparam file for each K, and creates jobs for running 
	 * structure using each mainparam file. Finally, the output files are 
	 * sent back to this builder's job workspace. Then an action is included in 
	 * the build, to render summary about the plug-in execution.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener) throws AbortException,InterruptedException, IOException {

		listener.getLogger().println(Messages.StructureBuilder_InvokingStructure());

		// Get the structure installation used
		StructureInstallation structureInstallation = DESCRIPTOR.getInstallationByName(this.structureInstallationName);
		if (structureInstallation == null) {
			throw new AbortException(Messages.StructureBuilder_InvalidStructureInstallation());
		}
		listener.getLogger().println("Using structure " + structureInstallation.getName() + " at " + structureInstallation.getPathToExecutable());
		listener.getLogger().println("K="+this.maxPops);

		// list of builds to be executed
		ListMultimap<AbstractProject<?, ?>, Future<FreeStyleBuild>> futures = 
				this.getStructureKProjectsAndFutureBuilds(build, listener, structureInstallation);

		List<FreeStyleBuild> builds = new ArrayList<FreeStyleBuild>(futures.size());
		// Spawn one job for each K/mainparam
		for(final Future<FreeStyleBuild> future : futures.values()) {
			try {
				final FreeStyleBuild futureBuild = future.get();
				if(futureBuild == null || futureBuild.getResult() != Result.SUCCESS) {
					listener.getLogger().println("Error executing project " + futureBuild.getProject().getName() + " failing the build.");
					build.setResult(Result.FAILURE);
					builds.add(futureBuild);
				}
			} catch (ExecutionException e) {
				e.printStackTrace(listener.getLogger());
				build.setResult(Result.FAILURE);
			}
		}

		// Delete temporary projects
		if(!this.getKeepJobsForEachK()) {
			for(AbstractProject<?, ?> project : futures.keySet()) {
				if(LOGGER.isLoggable(Level.FINE)) {
					LOGGER.log(Level.FINE, "Deleting project " + project.getName());
				}
				project.delete();
			}
		}

		// Adds build action for displaying build summary and links for output files
		try {
			final FilePath workspace = build.getWorkspace();
			if(workspace != null  && workspace.exists()) {
				final File outputFolder = new File(workspace.getRemote(), StructureKBuilder.STRUCTURE_RUN_OUTPUT_DIRECTORY);
				final String[] list = outputFolder.list();
				build.addAction(new StructureBuildSummaryAction(build, list, this.maxPops));
			}
		} catch(Throwable t) { // we can't stop a researcher because of this :)
			t.printStackTrace(listener.getLogger());
			build.setResult(Result.UNSTABLE);
		}

		// Report the results (summary, details, etc)
		return Boolean.TRUE;
	}
	/**
	 * Gets the list/map of structure K projects (a project for each K value) 
	 * and its future builds.
	 * @param build the FreeStyleBuild
	 * @param listener 
	 * @param structureInstallation 
	 * @return ListMultimap of AbstractProject<?, ?> and FreeStyleBuild
	 */
	private ListMultimap<AbstractProject<?, ?>, Future<FreeStyleBuild>> getStructureKProjectsAndFutureBuilds(
			AbstractBuild<?, ?> build, BuildListener listener, 
			StructureInstallation structureInstallation) throws AbortException {
		// Le list/map of futures to return
		final ListMultimap<AbstractProject<?, ?>, Future<FreeStyleBuild>> futures = ArrayListMultimap.create();
		// Le workspace
		final FilePath workspace = build.getWorkspace();

		// Create one mainparam file for each K value
		for (int i = 1; i <= this.maxPops; ++i) {
			listener.getLogger().println("Generating mainparams for K=" + i);
			try {
				// Replace variables with the values provided by the user in the job configuration
				final String mainParamContent = parser.parse(this.mainParams, i);
				final FilePath mainparamsFilePath = new FilePath(workspace, MAINPARAMS_PARAM_SET_K_PREFIX + i);
				mainparamsFilePath.write(mainParamContent, STRUCTURE_FILES_ENCODING);
				final FilePath extraparamsFilePath = new FilePath(workspace, STRUCTURE_EXTRAMPARAMS_FILENAME);
				extraparamsFilePath.write(extraParams, STRUCTURE_FILES_ENCODING);

				// Create one job for each K/mainparam
				final String projectName = build.getProject().getName() + Integer.toString(i);
				// We remove the old job, so its configuration is always up to date
				this.deleteExistingProject(projectName);

				final FreeStyleProject project = Hudson.getInstance().<FreeStyleProject>createProject(FreeStyleProject.class, projectName);

				// Add structure builder (that's the one that executes the tool)
				this.addBuilder(build, project, structureInstallation, i, 
						mainparamsFilePath.getName(), extraparamsFilePath.getName());

				// Set the label for the slave
				this.addLabel(project);

				final Future<FreeStyleBuild> future = project.scheduleBuild2(0, new UserCause());
				futures.put(project, future);
			} catch (ParserException e) {
				throw new AbortException("Failed to parse mainparams. Error message: " + e.getMessage());
			} catch (Throwable e) {
				throw new AbortException("Failed to create a project for running structure for K="+i);
			}
		}
		return futures;
	}
	/**
	 * Deletes an existing project.
	 * @param projectName the project name
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void deleteExistingProject(String projectName) throws IOException, InterruptedException {
		for (AbstractProject<?, ?> project : Hudson.getInstance().getProjects()) {
			if (project.getName().equals(projectName)) {
				project.delete();
				break;
			}
		}
	}
	/**
	 * Adds a builder that executes Structure for a given K value.
	 * @param build
	 * @param project 
	 * @param structureInstallation
	 * @param i
	 * @param mainparamsFileName
	 * @param extraparamsFileName
	 * @throws IOException
	 */
	private void addBuilder(AbstractBuild<?, ?> build, FreeStyleProject project, StructureInstallation structureInstallation, 
			int i, String mainparamsFileName, String extraparamsFileName) throws IOException {
		final StructureKBuilder structureKBuilder = new StructureKBuilder(
				build.getProject().getName(), structureInstallation, i, 
				numLoci, numInds, inFile, outFile, mainparamsFileName, 
				extraparamsFileName);
		((FreeStyleProject)project).getBuildersList().add(structureKBuilder); 
	}
	/**
	 * Adds a label to the project.
	 * @param project the project
	 * @throws IOException 
	 */
	private void addLabel(AbstractProject<?, ?> project) throws IOException {
		if(StringUtils.isNotBlank(this.label)) {
			((FreeStyleProject)project).setAssignedLabel(new LabelAtom(this.label));
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public StructureBuilderDescriptor getDescriptor() {
		return (StructureBuilderDescriptor) super.getDescriptor();
	}
}
