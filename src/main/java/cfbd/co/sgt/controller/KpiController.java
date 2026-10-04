package cfbd.co.sgt.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.response.KpiResponse;
import cfbd.co.sgt.service.CsvExporter;
import cfbd.co.sgt.service.KpiService;
import lombok.RequiredArgsConstructor;

/**
 * KPIs del flujo (PRD E6): sla-despacho, tiempos-ciclo, cola-carga, ruteo.
 * Rango por fechas (inclusive, yyyy-MM-dd); el alcance lo recorta el Service.
 */
@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
public class KpiController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final KpiService kpiService;
    private final CsvExporter csvExporter;

    @PreAuthorize("hasAuthority('kpi.read')")
    @GetMapping("/{familia}")
    public KpiResponse getKpi(@PathVariable String familia,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                              @RequestParam(name = "id_especialidad", required = false) UUID idEspecialidad) {
        return kpiService.calcular(familia, desde, hasta, idEspecialidad);
    }

    @PreAuthorize("hasAuthority('kpi.export')")
    @GetMapping("/{familia}/export.csv")
    public ResponseEntity<String> exportarKpi(@PathVariable String familia,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @RequestParam(name = "id_especialidad", required = false) UUID idEspecialidad) {
        KpiResponse kpi = kpiService.calcular(familia, desde, hasta, idEspecialidad);
        String nombreArchivo = "kpi-" + kpi.familia() + ".csv";
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .body(csvExporter.exportar(kpi));
    }
}
