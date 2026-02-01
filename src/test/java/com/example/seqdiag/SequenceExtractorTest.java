package com.example.seqdiag;

import com.example.seqdiag.service.SequenceExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SequenceExtractorTest {

    @Test
    public void testSimpleTwoClassCall() throws Exception {
        // Create a zip with two simple java files
        File temp = File.createTempFile("proj", ".zip");
        temp.delete();
        temp.deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp))) {
            zos.putNextEntry(new ZipEntry("A.java"));
            String a = "public class A { private B b = new B(); public void m() { b.n(); } }";
            zos.write(a.getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("B.java"));
            String b = "public class B { public void n() { } }";
            zos.write(b.getBytes());
            zos.closeEntry();
        }

        byte[] bytes = java.nio.file.Files.readAllBytes(temp.toPath());
        MockMultipartFile mp = new MockMultipartFile("file", "proj.zip", "application/zip", bytes);

        SequenceExtractor extractor = new SequenceExtractor();
        String plant = extractor.extractPlantUmlFromZip(mp);
        // Ensure the project call is detected
        assertTrue(plant.contains("A -> B: n") || plant.contains("A -> B: n"));
    }

    @Test
    public void testExternalClassesAreIgnored() throws Exception {
        // Create a zip where B calls System.out.println - an external SDK call
        File temp = File.createTempFile("proj", ".zip");
        temp.delete();
        temp.deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp))) {
            zos.putNextEntry(new ZipEntry("A.java"));
            String a = "public class A { private B b = new B(); public void m() { b.n(); } }";
            zos.write(a.getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("B.java"));
            String b = "public class B { public void n() { System.out.println(\"hi\"); } }";
            zos.write(b.getBytes());
            zos.closeEntry();
        }

        byte[] bytes = java.nio.file.Files.readAllBytes(temp.toPath());
        MockMultipartFile mp = new MockMultipartFile("file", "proj.zip", "application/zip", bytes);

        SequenceExtractor extractor = new SequenceExtractor();
        String plant = extractor.extractPlantUmlFromZip(mp);
        // External SDK classes like PrintStream should not appear in the generated PlantUML
        assertTrue(!plant.contains("PrintStream"));
        // Project calls should still be present
        assertTrue(plant.contains("A -> B: n") || plant.contains("A -> B: n"));
    }
}