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

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Structure build action. Adds a summary with the value of K and a list of the
 * output files with links. When clicked, the links will display the file
 * contents.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class StructureBuildSummaryAction implements Action {
	/*
	 * Constants of the action.
	 */
	private static final String URL = "structureResults";
	private static final String DISPLAY_NAME = "Structure results";
	// private static final String ICON_URL =
	// "/plugin/structure/icons/icon-details.gif";
	/**
	 * Le owner of me.
	 */
	private final AbstractBuild<?, ?> owner;
	/**
	 * The list of files.
	 */
	private final String[] files;
	/**
	 * The value of K.
	 */
	private final int k;
	/**
	 * Constructor with args.
	 * 
	 * @param owner
	 *            the build that is owner of this action
	 * @param files
	 *            the files
	 * @param k
	 *            the value of K
	 */
	public StructureBuildSummaryAction(AbstractBuild<?, ?> owner,
			String[] files, int k) {
		this.owner = owner;
		this.files = files;
		this.k = k;
	}
	/**
	 * @return the owner
	 */
	public AbstractBuild<?, ?> getOwner() {
		return owner;
	}
	/**
	 * @return the files
	 */
	public String[] getFiles() {
		return files;
	}
	/**
	 * @return the k
	 */
	public int getK() {
		return k;
	}
	/**
	 * Shows file content.
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void doShowFileContent(final StaplerRequest request, final StaplerResponse response) 
			throws IOException {
		String fileContents = "";
		if(request.hasParameter("file")) {
			String fileName = request.getParameter("file");
			FilePath workspace = owner.getWorkspace();
			if(workspace != null) {
				File outputFile = new File(workspace.getRemote(), StructureKBuilder.STRUCTURE_RUN_OUTPUT_DIRECTORY + "/" +fileName);
				if(outputFile.exists()) {
					fileContents = FileUtils.readFileToString(outputFile);
					response.getOutputStream().println(fileContents);
				}
			}
		}
	}
	/**
	 * Gets the output folder of Structure.
	 * 
	 * @return the output folder
	 */
	public String getStructureOutputFolder() {
		return StructureKBuilder.STRUCTURE_RUN_OUTPUT_DIRECTORY;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Action#getIconFileName()
	 */
	public String getIconFileName() {
		// return ICON_URL;
		return null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Action#getDisplayName()
	 */
	public String getDisplayName() {
		return DISPLAY_NAME;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Action#getUrlName()
	 */
	public String getUrlName() {
		return URL;
	}
}
