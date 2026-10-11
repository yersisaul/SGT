package cfbd.co.sgt.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.model.MetricaCaso;
import cfbd.co.sgt.model.ModuloProducto;
import cfbd.co.sgt.model.TipoDocumentoWeb;
import cfbd.co.sgt.model.TipoPartner;
import cfbd.co.sgt.model.WebCasoExito;
import cfbd.co.sgt.model.WebCategoriaNovedad;
import cfbd.co.sgt.model.WebClienteReferencia;
import cfbd.co.sgt.model.WebDocumento;
import cfbd.co.sgt.model.WebPartner;
import cfbd.co.sgt.model.WebProducto;
import cfbd.co.sgt.model.WebServicio;
import cfbd.co.sgt.repository.WebCasoExitoRepository;
import cfbd.co.sgt.repository.WebCategoriaNovedadRepository;
import cfbd.co.sgt.repository.WebClienteReferenciaRepository;
import cfbd.co.sgt.repository.WebDocumentoRepository;
import cfbd.co.sgt.repository.WebPartnerRepository;
import cfbd.co.sgt.repository.WebProductoRepository;
import cfbd.co.sgt.repository.WebServicioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Contenido inicial del sitio web público, tomado de www.cfbd.co y de los
 * brochures oficiales (fichas técnicas de Azor y Azor Analytics 2.0,
 * presentación, funcionalidades de Azor y postes de atención rápida).
 * Idempotente por slug (o por nombre en partners y clientes): solo crea lo que
 * falta y nunca pisa lo que el administrador editó desde el panel.
 */
@Component
@RequiredArgsConstructor
public class WebContenidoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebContenidoSeeder.class);

    private final WebProductoRepository productoRepository;
    private final WebServicioRepository servicioRepository;
    private final WebPartnerRepository partnerRepository;
    private final WebClienteReferenciaRepository clienteRepository;
    private final WebCasoExitoRepository casoRepository;
    private final WebCategoriaNovedadRepository categoriaNovedadRepository;
    private final WebDocumentoRepository documentoRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int productos = 0;
        for (WebProducto producto : productos()) {
            if (!productoRepository.existsBySlug(producto.getSlug())) {
                productoRepository.save(producto);
                productos++;
            }
        }
        int servicios = 0;
        for (WebServicio servicio : servicios()) {
            if (!servicioRepository.existsBySlug(servicio.getSlug())) {
                servicioRepository.save(servicio);
                servicios++;
            }
        }
        int partners = 0;
        for (WebPartner partner : partners()) {
            if (!partnerRepository.existsByNombre(partner.getNombre())) {
                partnerRepository.save(partner);
                partners++;
            }
        }
        int clientes = 0;
        for (WebClienteReferencia cliente : clientes()) {
            if (clienteRepository.findByNombre(cliente.getNombre()).isEmpty()) {
                clienteRepository.save(cliente);
                clientes++;
            }
        }
        // Después de clientes y productos: los casos se vinculan a ellos.
        int casos = 0;
        for (WebCasoExito caso : casosExito()) {
            if (!casoRepository.existsBySlug(caso.getSlug())) {
                casoRepository.save(caso);
                casos++;
            }
        }
        // Solo categorías: las novedades las redacta el administrador.
        int categorias = 0;
        for (WebCategoriaNovedad categoria : categoriasNovedad()) {
            if (!categoriaNovedadRepository.existsBySlug(categoria.getSlug())) {
                categoriaNovedadRepository.save(categoria);
                categorias++;
            }
        }
        // Fichas de los brochures sin PDF ni publicar: el administrador sube cada
        // archivo desde el panel y lo publica (sin archivo no se puede publicar).
        int documentos = 0;
        for (WebDocumento documento : documentos()) {
            if (!documentoRepository.existsByTitulo(documento.getTitulo())) {
                documentoRepository.save(documento);
                documentos++;
            }
        }
        if (productos + servicios + partners + clientes + casos + categorias + documentos > 0) {
            log.info("WebContenidoSeeder: creados {} productos, {} servicios, {} partners, {} clientes, {} casos "
                    + "de éxito, {} categorías de novedades y {} documentos del sitio web.",
                    productos, servicios, partners, clientes, casos, categorias, documentos);
        }
    }

    // ---------- Partners ----------

    private List<WebPartner> partners() {
        return List.of(
                partner("Network Optix", "VMS", TipoPartner.TECNOLOGICO, 1, "https://nxvms.com/integrations/471-azor",
                        "Nx EVOS, sistema operativo de video empresarial que combina video inteligente con información "
                                + "de inteligencia artificial basada en datos para la gestión operativa en todas las industrias."),
                partner("Visionarea", "Conteo de personas", TipoPartner.TECNOLOGICO, 2, "https://www.visionarea.net/",
                        "Especialistas en sistemas de conteo de personas: recopilan y analizan datos de tráfico de "
                                + "tiendas, centros comerciales, ferias, museos, edificios y aeropuertos."),
                partner("NodeWeaver", "HCI", TipoPartner.TECNOLOGICO, 3, "https://nodeweaver.eu/",
                        "Plataforma operativa de \"nano-nube\" definida por software que simplifica la implementación, "
                                + "administración y orquestación de infraestructura y aplicaciones en el borde distribuido."),
                partner("Resecurity", "Ciberseguridad", TipoPartner.TECNOLOGICO, 4, "https://www.resecurity.com/es",
                        "Plataforma de ciberseguridad diseñada para detectar y mitigar amenazas digitales. En 2022 "
                                + "anunció su alianza con CFBD para llevar detección y respuesta gestionada de amenazas "
                                + "con IA a organizaciones peruanas."),
                partner("GIGABYTE", "Servidores empresariales", TipoPartner.TECNOLOGICO, 5,
                        "https://www.gigabyte.com/Enterprise",
                        "Giga Computing, subsidiaria de GIGABYTE, ofrece servidores, estaciones de trabajo, soluciones "
                                + "refrigeradas por líquido y accesorios para empresas."),
                partner("VSAAS PERU", "Video vigilancia como servicio", TipoPartner.TECNOLOGICO, 6, null,
                        "Empresa peruana de soluciones de seguridad, video-analítica e infraestructura como servicio."),
                partner("Hikvision", "Videovigilancia", TipoPartner.MARCA_COMPATIBLE, 1, "https://www.hikvision.com/",
                        null),
                partner("Avigilon", "Videovigilancia", TipoPartner.MARCA_COMPATIBLE, 2, "https://www.avigilon.com/",
                        null),
                partner("Irisity", "Videoanalítica", TipoPartner.MARCA_COMPATIBLE, 3, "https://irisity.com/", null));
    }

    // ---------- Clientes de referencia ----------

    private static final String MUNICIPALIDAD_SURCO = "Municipalidad de Santiago de Surco";

    private List<WebClienteReferencia> clientes() {
        return List.of(
                cliente(MUNICIPALIDAD_SURCO, "Gobierno local", 1),
                cliente("Backus", "Consumo masivo", 2),
                cliente("Claro", "Telecomunicaciones", 3),
                cliente("Lima Expresa", "Infraestructura vial", 4),
                cliente("Makro", "Retail", 5),
                cliente("Ripley", "Retail", 6));
    }

    // ---------- Casos de éxito ----------

    private List<WebCasoExito> casosExito() {
        WebClienteReferencia surco = clienteRepository.findByNombre(MUNICIPALIDAD_SURCO).orElse(null);

        WebCasoExito alertaSurco = caso("alerta-surco", "Alerta Surco: emergencias y denuncias atendidas con Azor", 1,
                true,
                "La Municipalidad de Santiago de Surco atiende las emergencias y denuncias de todo el distrito con la "
                        + "plataforma Azor y su aplicación para vecinos.",
                "Alerta Surco es la aplicación de la Municipalidad de Santiago de Surco construida sobre AZOR. Los vecinos "
                        + "reportan emergencias y denuncias desde su celular; el Panel Azor las recibe geolocalizadas, "
                        + "ejecuta los protocolos de atención, analiza la gravedad y asigna la unidad más cercana a la "
                        + "incidencia.\n\nEl servicio está disponible en todo el distrito.",
                null,
                List.of(new MetricaCaso("50 000", "vecinos registrados en la app"),
                        new MetricaCaso("< 5 min", "tiempo de respuesta")));
        alertaSurco.setCliente(surco);
        alertaSurco.setProducto(productoRepository.findBySlug("app-reporter").orElse(null));

        WebCasoExito postes = caso("postes-de-atencion-rapida-surco", "Postes de Atención Rápida en Santiago de Surco",
                2, false,
                "Postes con botón de emergencia, cámaras y audio bidireccional conectados a la central de monitoreo, "
                        + "instalados en espacios estratégicos del distrito.",
                "Cualquier persona puede pedir ayuda presionando el botón de emergencia. La señal llega a la central de "
                        + "monitoreo, el operador conversa con el usuario desde el poste y se despacha una unidad al "
                        + "lugar. Los postes cuentan con cámara PTZ, cámara mini domo, sirena, circulina y luminaria, y "
                        + "funcionan además como elemento disuasorio. Hay postes en la Plaza de Armas y en la avenida "
                        + "Simón Salguero, entre otros puntos del distrito.",
                "https://www.facebook.com/MuniSurco/videos/-tecnolog%C3%ADa-al-servicio-de-los-vecinosen-surco-ya-contamos"
                        + "-con-30-postes-de-aten/1322111476122102/",
                List.of(new MetricaCaso("30", "postes de atención rápida")));
        postes.setCliente(surco);

        return List.of(alertaSurco, postes);
    }

    // ---------- Productos ----------

    private List<WebProducto> productos() {
        return List.of(azorAnalytics(), azorPanel(), appReporter(), appRespondiente());
    }

    private WebProducto azorAnalytics() {
        String perimetral = "Seguridad perimetral y protección";
        String aforo = "Gestión de espacios y aforo";
        String trafico = "Tráfico y vehículos";
        String biometria = "Biometría y comportamiento humano";
        return producto("azor-analytics", "Azor Analytics", "Analítica de video con inteligencia artificial", 1, true,
                "Motor de analítica de video de alto rendimiento que convierte las cámaras en sensores inteligentes: "
                        + "detecta comportamientos, patrones y objetos en tiempo real y para análisis forense.",
                "Azor Analytics es un software de analítica de video de alto rendimiento diseñado para entornos donde "
                        + "la eficiencia y el tiempo real son críticos. Su núcleo utiliza la arquitectura YOLO (You Only "
                        + "Look Once) sobre PyTorch: identifica múltiples objetos en una sola pasada de la red neuronal, "
                        + "con alta velocidad de inferencia sin comprometer la precisión.\n\n"
                        + "Procesa múltiples flujos de cámara simultáneamente y transforma el video en datos "
                        + "estructurados mediante reglas configurables. Permite entrenar modelos personalizados para "
                        + "objetos fuera del estándar y, tras una etapa inicial de etiquetado, acelera el proceso con "
                        + "autoaprendizaje supervisado.",
                List.of(
                        "Inferencia acelerada con TensorRT (GPU NVIDIA), ONNX (CPU) y OpenVINO (CPU Intel)",
                        "Modelo base entrenado con el dataset COCO (80 clases) y fine-tuning para objetos personalizados",
                        "Autoaprendizaje supervisado para acelerar el entrenamiento de nuevos modelos",
                        "Clasificación de atributos: género, rango de edad, color de prendas y posturas",
                        "Embeddings para búsquedas por similitud en bases de datos vectoriales como OpenSearch",
                        "Protocolos ONVIF, RTSP y RTMP; códecs H.264, H.265, MPEG-4 y MJPEG"),
                List.of(
                        modulo("Cruce de línea", perimetral, "Detecta el cruce de personas u otros objetos sobre líneas "
                                + "predefinidas, con direcciones válidas, filtros por color y alertas instantáneas ante "
                                + "cualquier intrusión."),
                        modulo("Objeto en área (merodeo e intrusión)", perimetral, "Reconoce cuando una persona u otros "
                                + "objetos circulan repetidamente o de forma inusual cerca de zonas vigiladas, para "
                                + "anticiparse a posibles actos delictivos."),
                        modulo("Vigilancia de activos", perimetral, "Activa una alerta si un objeto protegido es movido, "
                                + "desaparece o se obstruye su visibilidad, sin necesidad de intervención humana constante."),
                        modulo("Cercanía entre objetos", perimetral, "Analiza la relación espacial entre varios objetos "
                                + "para identificar proximidad; por ejemplo, motorizados o dos o más personas en una moto."),
                        modulo("Objeto fuera de área", perimetral, "Genera eventos si un objeto específico abandona un "
                                + "área definida por un tiempo establecido."),
                        modulo("Humo y fuego", perimetral, null),
                        modulo("Control de aforo", aforo, "Conoce en tiempo real cuántas personas o vehículos ingresan o "
                                + "salen de un lugar mediante líneas virtuales configurables y filtros inteligentes."),
                        modulo("Gestión de colas y tiempo de permanencia", aforo, "Controla cuánto tiempo permanece un "
                                + "objeto en una zona y guarda metadatos de todo lo que pasa por ella para estadística."),
                        modulo("Detección de aglomeración", aforo, "Identifica la concentración de personas en un espacio, "
                                + "con densidad por metro y cantidad mínima configurables."),
                        modulo("Conteo de personas", aforo, null),
                        modulo("Gestión de estacionamientos", trafico, "Determina en tiempo real qué espacios están libres "
                                + "u ocupados y cuánto tiempo ha permanecido un vehículo estacionado."),
                        modulo("Análisis de tráfico", trafico, "Detecta y sigue vehículos clasificándolos por tipo y "
                                + "dirección de movimiento, y guarda metadatos para búsquedas posteriores."),
                        modulo("Vigilancia vehicular", trafico, "En estacionamientos, asocia cada vehículo con la silueta "
                                + "de su propietario y genera eventos si quien se acerca no coincide."),
                        modulo("Límite de velocidad", trafico, "Estima la velocidad de los vehículos midiendo el tiempo de "
                                + "desplazamiento entre dos líneas virtuales."),
                        modulo("Reconocimiento de placas", trafico, "Lectura de caracteres alfanuméricos con redes "
                                + "neuronales profundas (CNN + BiLSTM), hasta 100 km/h y menos de 200 ms por frame."),
                        modulo("Moto en vereda", trafico, null),
                        modulo("Reconocimiento de rostros", biometria, "Análisis de rostros en 2D y 3D con precisión "
                                + "superior al 99,5 % (LFW), tolerante a anteojos, vello facial y mascarillas."),
                        modulo("Identificación de posturas humanas", biometria, "Interpreta en tiempo real los movimientos "
                                + "del cuerpo (parado, sentado, caído, corriendo, peleando, posición de disparo) para "
                                + "anticiparse a comportamientos de riesgo."),
                        modulo("Personas con objetos", biometria, "Combina dos redes neuronales para validar si una persona "
                                + "lleva objetos específicos en la cabeza, el rostro, las manos, el torso, la cintura o los pies."),
                        modulo("Personas en moto", biometria, null)));
    }

    private WebProducto azorPanel() {
        String operacion = "Operación";
        String analisis = "Análisis";
        String integraciones = "Integraciones";
        String administracion = "Administración";
        return producto("azor-panel", "Azor Panel", "Centro de comando unificado", 2, false,
                "Centraliza, verifica y atiende en tiempo real las alertas de la analítica de IA, las aplicaciones "
                        + "móviles y los dispositivos de seguridad desde un único dashboard operativo.",
                "AZOR es un sistema modular y un potente orquestador de datos: integra cámaras, motores de inteligencia "
                        + "artificial, botones de pánico, servicios de ubicación en tiempo real y otras fuentes en una "
                        + "única plataforma, personalizable para cualquier industria pública o privada.\n\n"
                        + "Su módulo operativo permite gestionar las alertas y alarmas recibidas, asignar recursos de "
                        + "ayuda y hacer seguimiento de la atención en mapas; el módulo estadístico muestra la cantidad "
                        + "y distribución temporal de los eventos, y el módulo de informes consolida en un solo reporte "
                        + "las alertas del VMS, la analítica y la aplicación móvil.",
                List.of(
                        "Integración con botones de pánico y sensores GPS en tiempo real",
                        "Monitoreo y despacho de alertas y emergencias desde una sola plataforma",
                        "Reglas y flujos automatizados entre servicios mediante microservicios",
                        "Asignación de eventos por tipo mediante una matriz de despacho configurable",
                        "Registro de cada acción con usuario, fecha y hora",
                        "Integración con terceros mediante REST API y conexiones en tiempo real (SSE)"),
                List.of(
                        modulo("Inicio", operacion, "Resumen general de emergencias, denuncias y usuarios registrados."),
                        modulo("Operaciones", operacion, "Operaciones en curso con sus unidades designadas y el mapa en "
                                + "tiempo real."),
                        modulo("Seguimiento", operacion, "Panel de seguimiento para atender emergencias y denuncias."),
                        modulo("Casos", operacion, null),
                        modulo("Cámaras", operacion, null),
                        modulo("Mapas", operacion, "Ubicación de los reportes y de los focos de origen de denuncias y "
                                + "emergencias."),
                        modulo("Estadísticas", analisis, "Cantidad y distribución temporal de alertas y alarmas para "
                                + "analizar tendencias."),
                        modulo("Reportes", analisis, "Informe consolidado de las alertas de VMS, analítica y aplicación "
                                + "móvil, exportable en PDF."),
                        modulo("NX", integraciones, null),
                        modulo("Analíticas", integraciones, null),
                        modulo("Automatizaciones", integraciones, null),
                        modulo("Telegram", integraciones, null),
                        modulo("Activos", administracion, null),
                        modulo("Usuarios", administracion, null),
                        modulo("Roles", administracion, null),
                        modulo("Configuración", administracion, null),
                        modulo("Documentación", administracion, null)));
    }

    private WebProducto appReporter() {
        return producto("app-reporter", "App Reporter", "Aplicación móvil ciudadana y corporativa", 3, false,
                "Permite registrar alertas y reportar incidentes geolocalizados que llegan al instante al Panel Azor, "
                        + "optimizando la supervisión y la capacidad de respuesta.",
                "La aplicación móvil de Azor conecta a los usuarios con la central de monitoreo en tiempo real. Cada "
                        + "reporte de emergencia o denuncia llega al Panel Azor con la ubicación, los datos del usuario "
                        + "registrado y su multimedia, y el centro de control puede alertar a sectores de la comunidad "
                        + "ante emergencias confirmadas.",
                List.of(
                        "Reportes geolocalizados con envío de multimedia en tiempo real",
                        "Validación de identidad con DNI o Carnet de Extranjería",
                        "Difusión segmentada de alertas de evacuación o contingencia",
                        "Botón de pánico silencioso con transmisión de ubicación en vivo"),
                List.of());
    }

    private WebProducto appRespondiente() {
        return producto("app-respondiente", "App Respondiente", "Aplicación móvil para las unidades de respuesta", 4,
                false,
                "Lleva las incidencias del Panel Azor a las unidades que atienden la emergencia y comparte su recorrido "
                        + "en tiempo real hasta el lugar del incidente.",
                "Complemento del servicio AZOR para el personal que acude a las emergencias. Las alertas se asignan a "
                        + "las unidades activas en la zona y el ciudadano que reportó puede ver el recorrido de la unidad "
                        + "hasta el lugar de la emergencia.",
                List.of(
                        "Recepción de las incidencias asignadas desde el Panel Azor",
                        "Ubicación de la unidad en tiempo real",
                        "El ciudadano ve el recorrido de la unidad hasta la emergencia",
                        "Atención de emergencias y denuncias en la zona asignada"),
                List.of());
    }

    // ---------- Servicios ----------

    private List<WebServicio> servicios() {
        return List.of(
                servicio("suministro-y-configuracion-de-servidores", "Suministro y configuración de servidores", "server", 1,
                        "Servidores empresariales y de alto rendimiento para cómputo intensivo, inteligencia artificial "
                                + "e hiperconvergencia.",
                        "Suministramos y configuramos servidores diseñados para alojar gran cantidad de CPU y GPU, con "
                                + "suministro eléctrico y disipación térmica a la medida de la carga. También diseñamos "
                                + "infraestructura hiperconvergente (HCI), que integra almacenamiento, redes y "
                                + "virtualización en un solo sistema de gestión con flexibilidad y escalabilidad al borde.",
                        List.of("Servidores empresariales GIGABYTE",
                                "Hiperconvergencia (HCI) con NodeWeaver",
                                "Dimensionamiento según la carga de video e inteligencia artificial")),
                servicio("configuracion-y-mantenimiento-de-vms", "Configuración y mantenimiento de VMS", "video", 2,
                        "Implementación, configuración y soporte de sistemas de gestión de video para operaciones de "
                                + "seguridad.",
                        "Implementamos y mantenemos sistemas de gestión de video (VMS) como Network Optix (Nx), "
                                + "integrados con la analítica de Azor Analytics y con el centro de comando Azor Panel.",
                        List.of("Integración con Network Optix (Nx)",
                                "Compatibilidad con cámaras ONVIF, RTSP y RTMP",
                                "Integración de alarmas del VMS en el Panel Azor")),
                servicio("entrenamiento-de-modelos-de-vision-por-computadora",
                        "Entrenamiento de modelos de visión por computadora", "brain", 3,
                        "Suministro, entrenamiento y calibración de redes neuronales para detectar los objetos y "
                                + "escenarios que su operación necesita.",
                        "Creamos datasets etiquetados y entrenamos modelos personalizados para objetos fuera del "
                                + "estándar, con fine-tuning sobre modelos base y autoaprendizaje supervisado. Los "
                                + "modelos se validan con métricas de precisión, exhaustividad y mAP.",
                        List.of("Etiquetado de imágenes y creación de datasets",
                                "Fine-tuning de modelos base para objetos personalizados",
                                "Despliegue de inferencia en GPU, CPU o equipos de borde")),
                servicio("desarrollo-de-soluciones-de-seguridad-a-medida",
                        "Desarrollo de soluciones de seguridad a medida", "shield-halved", 4,
                        "Software a medida que integra sus sistemas existentes con analítica de video, alertas y "
                                + "automatización.",
                        "Diseñamos y desarrollamos plataformas de software a medida e integramos los sistemas legados con "
                                + "cómputo de última generación: investigación y analítica, arquitectura híbrida y "
                                + "despliegue inteligente con la plataforma AZOR.",
                        List.of("Integración con sistemas existentes mediante REST API",
                                "Automatización de reglas y flujos entre servicios",
                                "Plataformas modulares y seguras")));
    }

    // ---------- Categorías de novedades ----------

    private List<WebCategoriaNovedad> categoriasNovedad() {
        return List.of(categoriaNovedad("noticias", "Noticias", 1), categoriaNovedad("eventos", "Eventos", 2));
    }

    // ---------- Documentos (sección Brochures de cfbd.co) ----------

    private List<WebDocumento> documentos() {
        WebProducto analytics = productoRepository.findBySlug("azor-analytics").orElse(null);
        WebProducto panel = productoRepository.findBySlug("azor-panel").orElse(null);
        return List.of(
                documento("Brochure Azor Analytics", TipoDocumentoWeb.BROCHURE, analytics, 1,
                        "Qué detecta Azor Analytics, sus beneficios y las analíticas disponibles."),
                documento("Ficha técnica Azor Analytics 2.0", TipoDocumentoWeb.FICHA_TECNICA, analytics, 2,
                        "Motor de IA, métricas de rendimiento, módulos de analítica y requisitos de cámaras y hardware."),
                documento("Azor: funcionalidades", TipoDocumentoWeb.BROCHURE, panel, 3,
                        "Panel Azor, app vecino y app respondiente, casos municipales y modalidades de venta."),
                documento("Ficha técnica Azor", TipoDocumentoWeb.FICHA_TECNICA, panel, 4,
                        "Especificaciones del sistema de recepción y gestión de emergencias: módulos, arquitectura "
                                + "y seguridad."),
                documento("Presentación Azor", TipoDocumentoWeb.PRESENTACION, panel, 5,
                        "Qué es AZOR, capacidades, integraciones y módulos operativo, estadístico y de informes."),
                documento("Brochure Postes de Atención Rápida", TipoDocumentoWeb.BROCHURE, null, 6,
                        "Funcionamiento, componentes y beneficios de los postes con botón de emergencia."));
    }

    // ---------- Helpers ----------

    private WebProducto producto(String slug, String nombre, String subtitulo, int orden, boolean destacado,
            String descripcionCorta, String descripcion, List<String> caracteristicas, List<ModuloProducto> modulos) {
        WebProducto producto = new WebProducto();
        producto.setSlug(slug);
        producto.setNombre(nombre);
        producto.setSubtitulo(subtitulo);
        producto.setDescripcion_corta(descripcionCorta);
        producto.setDescripcion(descripcion);
        producto.setCaracteristicas(new ArrayList<>(caracteristicas));
        producto.setModulos(new ArrayList<>(modulos));
        producto.setDestacado(destacado);
        producto.setOrden(orden);
        producto.setPublicado(true);
        return producto;
    }

    private WebServicio servicio(String slug, String nombre, String icono, int orden, String descripcionCorta,
            String descripcion, List<String> caracteristicas) {
        WebServicio servicio = new WebServicio();
        servicio.setSlug(slug);
        servicio.setNombre(nombre);
        servicio.setIcono(icono);
        servicio.setDescripcion_corta(descripcionCorta);
        servicio.setDescripcion(descripcion);
        servicio.setCaracteristicas(new ArrayList<>(caracteristicas));
        servicio.setOrden(orden);
        servicio.setPublicado(true);
        return servicio;
    }

    private ModuloProducto modulo(String nombre, String grupo, String descripcion) {
        return new ModuloProducto(nombre, grupo, descripcion);
    }

    private WebPartner partner(String nombre, String categoria, TipoPartner tipo, int orden, String urlSitio,
            String descripcion) {
        WebPartner partner = new WebPartner();
        partner.setNombre(nombre);
        partner.setCategoria(categoria);
        partner.setTipo(tipo);
        partner.setOrden(orden);
        partner.setUrl_sitio(urlSitio);
        partner.setDescripcion(descripcion);
        partner.setPublicado(true);
        return partner;
    }

    private WebClienteReferencia cliente(String nombre, String sector, int orden) {
        WebClienteReferencia cliente = new WebClienteReferencia();
        cliente.setNombre(nombre);
        cliente.setSector(sector);
        cliente.setOrden(orden);
        cliente.setPublicado(true);
        return cliente;
    }

    private WebDocumento documento(String titulo, TipoDocumentoWeb tipo, WebProducto producto, int orden,
            String descripcion) {
        WebDocumento documento = new WebDocumento();
        documento.setTitulo(titulo);
        documento.setTipo(tipo);
        documento.setProducto(producto);
        documento.setOrden(orden);
        documento.setDescripcion(descripcion);
        documento.setPublicado(false);
        return documento;
    }

    private WebCategoriaNovedad categoriaNovedad(String slug, String nombre, int orden) {
        WebCategoriaNovedad categoria = new WebCategoriaNovedad();
        categoria.setSlug(slug);
        categoria.setNombre(nombre);
        categoria.setOrden(orden);
        return categoria;
    }

    private WebCasoExito caso(String slug, String titulo, int orden, boolean destacado, String resumen,
            String contenido, String urlExterna, List<MetricaCaso> metricas) {
        WebCasoExito caso = new WebCasoExito();
        caso.setSlug(slug);
        caso.setTitulo(titulo);
        caso.setOrden(orden);
        caso.setDestacado(destacado);
        caso.setResumen(resumen);
        caso.setContenido(contenido);
        caso.setUrl_externa(urlExterna);
        caso.setMetricas(new ArrayList<>(metricas));
        caso.setPublicado(true);
        return caso;
    }
}
