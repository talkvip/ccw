package clojuredev;

import java.util.ArrayList;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import clojure.util.ClojurePlugin;

/**
 * Borrowing heavily from the old Scala Eclipse plugin.
 * 
 * @author cmarshal
 * 
 */
public class ClojureProjectNature implements IProjectNature {

    static public final String NATURE_ID = "clojuredev.nature";

    private IProject project;

    public void configure() throws CoreException {
        IProjectDescription desc = getProjectDescription();
        
        if (desc == null) {
        	return;
        }

        ICommand[] spec = desc.getBuildSpec();	// get project build specification

        if (builderPresent(spec, ClojureBuilder.BUILDER_ID)) {
        	return;
        }
        
        if (!builderPresent(spec, JavaCore.BUILDER_ID)) {
        	ClojuredevPlugin.logError("Java Builder not found");
        	return;
        }
        
        ICommand clojureCommand = desc.newCommand();
        clojureCommand.setBuilderName(ClojureBuilder.BUILDER_ID);
        
        // Add clojure builder before all other builders (thus before
        // Java builder if present)
        ICommand[] newSpec = new ICommand[spec.length + 1];
        newSpec[0] = clojureCommand;
        System.arraycopy(spec, 0, newSpec, 1, spec.length);

        desc.setBuildSpec(newSpec);

        project.setDescription(desc, IResource.FORCE, null);
        
        setupClojureProjectClassPath();
    }
    
    private void setupClojureProjectClassPath() throws CoreException {
        ClojureProject clojureProject = ClojureCore
                .getClojureProject(project);
        IJavaProject javaProject = clojureProject
                .getJavaProject();
        
        if (!hasClojureLibOnClasspath(javaProject)) {
	        IClasspathEntry[] entriesOld = javaProject.getRawClasspath();
	        IClasspathEntry[] entriesNew = new IClasspathEntry[entriesOld.length + 1];
	        
	        System.arraycopy(entriesOld, 0, entriesNew, 0, entriesOld.length);
	
	        entriesNew[entriesOld.length + 0] = JavaCore.newLibraryEntry(Path
	                .fromPortableString(ClojurePlugin.getDefault().binPath()), null, null);
	
	        javaProject.setRawClasspath(entriesNew, null);
	        javaProject.save(null, true);
        }
    }
    
    private boolean hasClojureLibOnClasspath(IJavaProject javaProject) throws JavaModelException {
    	return javaProject.findElement(new Path("clojure/lang")) != null;
    }

    public void deconfigure() throws CoreException {
        IProjectDescription desc = getProjectDescription();
        
        if (desc == null) {
        	return;
        }
        
        ICommand[] spec = desc.getBuildSpec();
        
        if (!builderPresent(spec, ClojureBuilder.BUILDER_ID)) {
        	// builder was not found, so no need to remove it
        	return;
        }

        // builder was found so remove it
        ArrayList<ICommand> newSpec = new ArrayList<ICommand>(spec.length - 1);
        for (ICommand command: spec) {
        	if (!command.getBuilderName().equals(ClojureBuilder.BUILDER_ID)) {
        		newSpec.add(command);
        	}
        }
        
        // set back the project description
        desc.setBuildSpec(newSpec.toArray(new ICommand[0]));
        try {
            project.setDescription(desc, null);
        }
        catch (CoreException e) {
            ClojuredevPlugin.logError("Could not set project description", e);
        }
    }

    private boolean builderPresent(ICommand[] builders, String builderName) {
	    for (ICommand builder: builders) {
	    	if (builder.getBuilderName().equals(builderName)) {
	            return true;
	        }
	    }
	    return false;
    }

    
    /**
     * @return the project description or null if the project
     *         is null, closed, or an error occured while getting description
     */
    private IProjectDescription getProjectDescription() {
        if (project == null) {
            ClojuredevPlugin.logError(
                    "Could not add or remove clojure nature: project is null", null);
            return null;
        }
        
        
        // closed clojure projects cannot be modified
        if (!project.isOpen()) {
            ClojuredevPlugin.logWarning(
                    "Nature modification asked on a closed project!");
            return null;
        }
        
        try {
            return project.getDescription();
        } catch (CoreException e) {
            ClojuredevPlugin.logError("Could not get project description", e);
            return null;
        }
    }
    
    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

}