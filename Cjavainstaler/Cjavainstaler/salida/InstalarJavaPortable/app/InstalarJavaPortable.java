/*
    InstalarJavaPortable.java
    ---------------------------------------------------------------
    Instala un JDK portable (Eclipse Temurin) sin permisos de admin,
    lo configura para el proceso hijo y descarga + ejecuta un .jar
    externo.

    100% Java estandar (HttpClient + java.util.zip), sin depender
    de curl, tar ni librerias externas. Requiere tener ya instalado
    un JRE/JDK 11+ para EJECUTAR este instalador (es un bootstrapper:
    usa el Java que ya tengas para instalar un JDK portable completo
    y lanzar tu aplicacion con el).

    Compilar:  javac InstalarJavaPortable.java
    Ejecutar:  java InstalarJavaPortable

    Basado en la idea de https://github.com/Vinetos/java-portable
    (ese repo esta archivado y su descarga de Oracle JDK ya no
    funciona; aqui se usa el JDK portable oficial de Eclipse
    Temurin, que viene en .zip listo para usar).

    Nota: tal como esta, apunta al JDK de Windows x64. Si necesitas
    Linux/Mac, cambia JDK_URL por la URL .zip equivalente de
    https://api.adoptium.net y el nombre del ejecutable ("java" en
    vez de "java.exe").
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
    // Cambia esta URL por el link directo de descarga de TU .jar
    // (por ejemplo, el asset de un Release de GitHub).
    private static final String JAR_URL =
            "https://github.com/usuario/mi-repo/releases/download/v1.0/java.jar";
    // ======================================================

    // API oficial de Adoptium: siempre apunta al ultimo JDK 17 LTS
    // portable (.zip) para Windows x64. No requiere login ni cookies.
    private static final String JDK_URL =
            "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk";

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
            System.out.println("JDK portable ya instalado, se omite la descarga.");
            return;
        }

        Files.createDirectories(BASE_DIR);

        System.out.println("Descargando JDK portable (Eclipse Temurin 17)...");
        downloadFile(JDK_URL, JDK_ZIP);

        System.out.println("Descomprimiendo JDK...");
        Files.createDirectories(JDK_DIR);
        extractZipStrippingTopFolder(JDK_ZIP, JDK_DIR);

        Files.deleteIfExists(JDK_ZIP);
        System.out.println("JDK portable instalado en: " + JDK_DIR.toAbsolutePath());
    }

    private static void downloadJar() throws Exception {
        System.out.println("Descargando .jar desde: " + JAR_URL);
        downloadFile(JAR_URL, JAR_PATH);
        System.out.println("Guardado en: " + JAR_PATH.toAbsolutePath());
    }

    private static void runJar() throws Exception {
        System.out.println("Ejecutando la aplicacion...");
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
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(dest));
        if (response.statusCode() >= 400) {
            throw new IOException("Descarga fallida (" + response.statusCode() + "): " + url);
        }
    }

    /** Extrae un zip quitando la primera carpeta del path de cada entrada
     *  (equivalente a "tar --strip-components=1"), asi el contenido de
     *  jdk-17.x+y/... queda directo dentro de destDir. */
    private static void extractZipStrippingTopFolder(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                int firstSlash = name.indexOf('/');
                if (firstSlash < 0) continue; // era el propio folder raiz
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