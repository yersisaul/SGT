package cfbd.co.sgt.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiResponse;

class CsvExporterTest {

    private final CsvExporter exporter = new CsvExporter();

    @Test
    void exportaCabeceraFilasYNeutralizaFormulas() {
        // AC-036: UTF-8 con BOM, mismas cifras y celdas "=..." neutralizadas.
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("especialidad", "=HYPERLINK(\"http://x\")");
        fila.put("en_cola", 4L);
        fila.put("antiguedad_max", 6.0);
        KpiResponse kpi = new KpiResponse("cola-carga", "Cola y carga", Instant.EPOCH, Instant.EPOCH, List.of("Global"),
                true, List.of(),
                List.of(new KpiColumnaResponse("especialidad", "Especialidad", ""),
                        new KpiColumnaResponse("en_cola", "En cola", ""),
                        new KpiColumnaResponse("antiguedad_max", "Más antigua", "h")),
                List.of(fila));

        String csv = exporter.exportar(kpi);

        assertThat(csv).startsWith("﻿\"Especialidad\",\"En cola\",\"Más antigua (h)\"\r\n");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://x\"\")\",4,6.0");
    }

    @Test
    void valoresNulosQuedanVacios() {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("especialidad", "Desarrollo");
        fila.put("cumplimiento", null);
        KpiResponse kpi = new KpiResponse("sla-despacho", "SLA", Instant.EPOCH, Instant.EPOCH, List.of("Global"), false,
                List.of(), List.of(new KpiColumnaResponse("especialidad", "Especialidad", ""),
                        new KpiColumnaResponse("cumplimiento", "Cumplimiento", "%")),
                List.of(fila));
        assertThat(exporter.exportar(kpi)).endsWith("\"Desarrollo\",\r\n");
    }
}
