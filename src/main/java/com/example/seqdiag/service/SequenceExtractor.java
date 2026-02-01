package com.example.seqdiag.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class SequenceExtractor {

    public String extractPlantUmlFromZip(MultipartFile zipFile, boolean includeExternal) throws Exception {
        File temp = Files.createTempDirectory("upload").toFile();
        File zipOnDisk = new File(temp, "project.zip");
        try (FileOutputStream fos = new FileOutputStream(zipOnDisk)) {
            fos.write(zipFile.getBytes());
        }

        List<File> javaFiles = unzipAndCollectJavaFiles(zipOnDisk, temp);

        List<Call> calls = new ArrayList<>();
        Set<String> participants = new HashSet<>();

        // Configure symbol solver to resolve types within the unzipped project
        com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver combinedSolver = new com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver();
        combinedSolver.add(new com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver());
        combinedSolver.add(new com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver(temp));
        com.github.javaparser.symbolsolver.JavaSymbolSolver symbolSolver = new com.github.javaparser.symbolsolver.JavaSymbolSolver(combinedSolver);
        com.github.javaparser.ParserConfiguration cfg = new com.github.javaparser.ParserConfiguration().setSymbolResolver(symbolSolver);
        JavaParser parser = new JavaParser(cfg);

        // Build a set of project classes (qualified + simple names) from the .java sources
        Set<String> projectQualifiedNames = new HashSet<>();
        Set<String> projectSimpleNames = new HashSet<>();
        for (File f : javaFiles) {
            try (InputStream in = Files.newInputStream(f.toPath())) {
                CompilationUnit cu = parser.parse(in).getResult().orElse(null);
                if (cu == null) continue;
                String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
                for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    String simple = cls.getNameAsString();
                    projectSimpleNames.add(simple);
                    String fq = pkg.isEmpty() ? simple : pkg + "." + simple;
                    projectQualifiedNames.add(fq);
                }
            }
        }

        // Second pass: extract calls but only include callees that are project classes (defined in .java sources)
        for (File f : javaFiles) {
            try (InputStream in = Files.newInputStream(f.toPath())) {
                CompilationUnit cu = parser.parse(in).getResult().orElse(null);
                if (cu == null) continue;
                for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    String className = cls.getNameAsString();
                    participants.add(className);
                    for (MethodDeclaration md : cls.getMethods()) {
                        String caller = className;
                        if (!md.getBody().isPresent()) continue;
                        for (MethodCallExpr mce : md.findAll(MethodCallExpr.class)) {
                            String methodName = mce.getNameAsString();
                            String calleeSimple = caller; // fallback
                            boolean isProjectClass = false;
                            try {
                                com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration rmd = mce.resolve();
                                String calleeQualified = rmd.declaringType().getQualifiedName();
                                String simple = calleeQualified.contains(".") ? calleeQualified.substring(calleeQualified.lastIndexOf('.') + 1) : calleeQualified;
                                if (projectQualifiedNames.contains(calleeQualified) || projectSimpleNames.contains(simple)) {
                                    calleeSimple = simple;
                                    isProjectClass = true;
                                }
                            } catch (Exception ex) {
                                // fallback to scope text
                                String scopeText = mce.getScope().map(Object::toString).orElse("");
                                String simple = scopeText.contains(".") ? scopeText.substring(scopeText.lastIndexOf('.') + 1) : scopeText;
                                if (projectSimpleNames.contains(simple)) {
                                    calleeSimple = simple;
                                    isProjectClass = true;
                                }
                            }

                            // handle non-project (sdk/third-party) classes
                            if (!isProjectClass) {
                                if (includeExternal) {
                                    calleeSimple = "External";
                                } else {
                                    continue;
                                }
                            }

                            participants.add(calleeSimple);
                            calls.add(new Call(caller, calleeSimple, methodName));
                        }
                    }
                }
            }
        }

        return PlantUmlGenerator.generate(participants, calls);
    }

    private List<File> unzipAndCollectJavaFiles(File zipOnDisk, File destDir) throws Exception {
        List<File> javaFiles = new ArrayList<>();
        ZipFile zf = new ZipFile(zipOnDisk);
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File out = new File(destDir, entry.getName());
            if (entry.isDirectory()) {
                out.mkdirs();
                continue;
            } else {
                out.getParentFile().mkdirs();
                try (InputStream in = zf.getInputStream(entry); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
                }
            }
            if (out.getName().endsWith(".java")) javaFiles.add(out);
        }
        zf.close();
        return javaFiles;
    }

    public byte[] renderPlantUmlToPng(String plantUml) throws Exception {
        // Use PlantUML SourceStringReader to generate PNG
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            net.sourceforge.plantuml.SourceStringReader reader = new net.sourceforge.plantuml.SourceStringReader(plantUml);
            // generateImage returns the number of bytes written or null
            reader.outputImage(os);
            return os.toByteArray();
        }
    }

    static class Call {
        String caller;
        String callee;
        String method;

        Call(String caller, String callee, String method) {
            this.caller = caller;
            this.callee = callee;
            this.method = method;
        }
    }
}