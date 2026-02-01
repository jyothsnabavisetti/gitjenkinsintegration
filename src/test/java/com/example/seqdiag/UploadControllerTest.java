package com.example.seqdiag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "java.awt.headless=true")
public class UploadControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Test
    public void uploadEndpointHandlesZip() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(wac).build();

        // Build a small zip with two java files
        File temp = File.createTempFile("proj", ".zip");
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

        mvc.perform(MockMvcRequestBuilders.multipart("/api/upload").file(mp)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantuml").exists())
                .andExpect(jsonPath("$.pngBase64").exists());
    }
}
