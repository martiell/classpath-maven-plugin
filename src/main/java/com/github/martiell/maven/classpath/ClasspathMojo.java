package com.github.martiell.maven.classpath;

import static java.util.Collections.singletonList;
import static java.util.Collections.sort;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * Writes the path of each compile/runtime dependency of a project to a file,
 * where each path is relative to the root of a maven repository.
 * <p>
 * For example, if the project depends on commons-lang 2.6 and commons-math 2.2,
 * then the file written will contain the line:
 * <pre>
 * commons-lang/commons-lang/2.6/commons-lang-2.6.jar
 * org/apache/commons/commons-math/2.2/commons-math-2.2.jar
 * </pre>
 */
@Mojo(name = "generate",
        defaultPhase = GENERATE_RESOURCES,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ClasspathMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/generated-resources/classpath")
    private File outputDirectory;

    @Parameter(defaultValue = "classpath.txt")
    private String file;

    @Parameter(property = "sort", defaultValue = "false")
    private boolean sort;

    @Component
    private ArtifactRepositoryLayout layout;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> paths = buildClasspath();
        writeClasspath(paths);
        addGeneratedResourceToBuild();
    }

    private List<String> buildClasspath() {
        List<Artifact> artifacts = project.getRuntimeArtifacts();
        List<String> paths = new ArrayList<String>(artifacts.size());
        for (Artifact artifact : artifacts) {
            paths.add(layout.pathOf(artifact));
        }

        if (sort) {
            sort(paths);
        }
        return paths;
    }

    private void writeClasspath(List<String> paths)
            throws MojoFailureException, MojoExecutionException {
        File output = new File(outputDirectory, file);
        File directory = output.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new MojoFailureException("Could not create directory: " + directory);
        }

        try {
            writeClasspathFile(output, paths);
        } catch (IOException ex) {
            throw new MojoFailureException("Could not write to " + file, ex);
        }
    }

    private void writeClasspathFile(File output, List<String> classpath)
            throws IOException {
        OutputStream os = null;
        Writer writer = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(output));
            writer = new OutputStreamWriter(os, "UTF-8");
            for (String path : classpath) {
                writer.append(path).append('\n');
            }
        } finally {
            IOUtil.close(writer);
            IOUtil.close(os);
        }
    }

    private void addGeneratedResourceToBuild() {
        Resource resource = new Resource();
        resource.setDirectory(outputDirectory.getPath());
        resource.setIncludes(singletonList(file));
        project.addResource(resource);
    }

}
