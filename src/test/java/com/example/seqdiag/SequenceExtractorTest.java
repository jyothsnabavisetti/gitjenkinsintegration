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
            String a = "public class A { public void m() { new B().n(); } }";
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
        assertTrue(plant.contains("A -> B: n") || plant.contains("A -> B: n"));
    }
}