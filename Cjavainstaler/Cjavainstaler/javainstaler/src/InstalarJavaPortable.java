/*
    InstalarJavaPortable.java
    ---------------------------------------------------------------
    Instala un JDK 26 portable (Eclipse Temurin) sin permisos de admin,
    lo configura para el proceso hijo y descarga + ejecuta un .jar
    externo.
*/

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InstalarJavaPortable {

    // ================== CONFIGURA AQUI ==================
    private static final String JAR_URL =
            "https://archiv-love-web.lovable.app/f/1790314606217-0-selvax-1.0-SNAPSHOT.jar";
    // ======================================================

    // API oficial de Adoptium: apunta al binario JDK 26 para Windows x64
    private static final String JDK_URL =
            "https://api.adoptium.net/v3/binary/latest/26/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk";

    private static final Path BASE_DIR = Paths.get("java-portable-runtime");
    private static final Path JDK_DIR  = BASE_DIR.resolve("jdk");
    private static final Path JDK_ZIP  = BASE_DIR.resolve("jdk.zip");
    private static final Path JAR_PATH = BASE_DIR.resolve("app.jar");
    private static final Path JAVA_EXE = JDK_DIR.resolve("bin").resolve("java.exe");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static void main(String[] args) throws Exception {
        installPortableJdk();
        downloadJar();
        runJar();
    }

    private static void installPortableJdk() throws Exception {
        if (Files.exists(JAVA_EXE)) {
            System.out.println("JDK 26 portable ya instalado, se omite la descarga.");
            return;
        }

        Files.createDirectories(BASE_DIR);

        System.out.println("Descargando JDK 26 portable (Eclipse Temurin)...");
        downloadFile(JDK_URL, JDK_ZIP);

        System.out.println("Descomprimiendo JDK 26...");
        Files.createDirectories(JDK_DIR);
        extractZipStrippingTopFolder(JDK_ZIP, JDK_DIR);

        Files.deleteIfExists(JDK_ZIP);
        System.out.println("JDK 26 portable instalado en: " + JDK_DIR.toAbsolutePath());
    }

    private static void downloadJar() throws Exception {
        System.out.println("Descargando .jar desde: " + JAR_URL);
        
        // Forzar eliminación si existe un archivo corrupto viejo
        Files.deleteIfExists(JAR_PATH);
        
        downloadFile(JAR_URL, JAR_PATH);

        // Validar que el archivo descargado sea un JAR válido (no una página HTML)
        long size = Files.size(JAR_PATH);
        if (size < 2000) { 
            String content = Files.readString(JAR_PATH);
            Files.deleteIfExists(JAR_PATH);
            throw new IOException("El archivo descargado NO es un JAR valido (tamaño: " + size + " bytes).\n" +
                    "Respuesta del servidor:\n" + content.substring(0, Math.min(content.length(), 300)));
        }

        System.out.println("Guardado exitosamente en: " + JAR_PATH.toAbsolutePath() + " (" + (size / 1024) + " KB)");
    }

    private static void runJar() throws Exception {
        System.out.println("Ejecutando la aplicacion con Java 26...");
        ProcessBuilder pb = new ProcessBuilder(
                JAVA_EXE.toAbsolutePath().toString(), "-jar", JAR_PATH.toAbsolutePath().toString());
        pb.environment().put("JAVA_HOME", JDK_DIR.toAbsolutePath().toString());
        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            System.err.println("La aplicacion termino con codigo: " + exitCode);
            System.exit(exitCode);
        }
    }

    private static void downloadFile(String url, Path dest) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .GET()
                .build();
                
        HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(dest));
        
        if (response.statusCode() >= 400) {
            Files.deleteIfExists(dest);
            throw new IOException("Descarga fallida (" + response.statusCode() + "): " + url);
        }
    }

    private static void extractZipStrippingTopFolder(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                int firstSlash = name.indexOf('/');
                if (firstSlash < 0) continue;
                String stripped = name.substring(firstSlash + 1);
                if (stripped.isEmpty()) continue;

                Path outPath = destDir.resolve(stripped).normalize();
                if (!outPath.startsWith(destDir)) {
                    throw new IOException("Entrada de zip fuera de destino: " + name);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}