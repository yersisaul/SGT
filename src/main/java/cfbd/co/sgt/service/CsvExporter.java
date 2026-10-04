package cfbd.co.sgt.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.KpiColumnaResponse;
import cfbd.co.sgt.dto.response.KpiResponse;

/**
 * Serializa un KPI a CSV (RFC 4180, UTF-8 con BOM para que Excel respete los
 * acentos). Neutraliza las celdas de texto que empiezan con = + - @ para
 * evitar inyección de fórmulas al abrirlo en una hoja de cálculo.
 */
@Component
public class CsvExporter {

    private static final String BOM = "﻿";
    private static final String SEPARADOR = ",";
    private static final String FIN_LINEA = "\r\n";
    private static final String PREFIJOS_FORMULA = "=+-@\t\r";

    public String exportar(KpiResponse kpi) {
        StringBuilder csv = new StringBuilder(BOM);
        List<KpiColumnaResponse> columnas = kpi.columnas();
        csv.append(columnas.stream().map(this::cabecera).map(this::celda).collect(Collectors.joining(SEPARADOR)))
                .append(FIN_LINEA);
        for (Map<String, Object> fila : kpi.filas()) {
            csv.append(columnas.stream()
                            .map(columna -> valor(fila.get(columna.clave())))
                            .collect(Collectors.joining(SEPARADOR)))
                    .append(FIN_LINEA);
        }
        return csv.toString();
    }

    private String cabecera(KpiColumnaResponse columna) {
        return columna.unidad().isEmpty() ? columna.etiqueta() : columna.etiqueta() + " (" + columna.unidad() + ")";
    }

    private String valor(Object valor) {
        if (valor == null) {
            return "";
        }
        return valor instanceof Number ? valor.toString() : celda(valor.toString());
    }

    private String celda(String texto) {
        String seguro = !texto.isEmpty() && PREFIJOS_FORMULA.indexOf(texto.charAt(0)) >= 0 ? "'" + texto : texto;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }
}
