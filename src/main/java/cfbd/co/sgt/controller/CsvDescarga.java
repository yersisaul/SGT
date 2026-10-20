package cfbd.co.sgt.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Respuesta de descarga CSV común a los listados (nombre con la fecha del día). */
final class CsvDescarga {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private CsvDescarga() {
    }

    static ResponseEntity<String> de(String prefijo, String csv) {
        String nombreArchivo = prefijo + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .body(csv);
    }
}
