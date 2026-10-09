#!/bin/sh
# SGT — confianza en el proxy inverso que esté DELANTE de nginx (Traefik en
# Coolify, balanceador corporativo...). Lo ejecuta el entrypoint de la imagen
# antes de arrancar nginx y genera conf.d/00-proxies-confiables.conf.
#
# NGINX_TRUSTED_PROXIES: redes (CIDR) de ese proxy, separadas por espacio o coma.
#   Vacío (por defecto): nginx es el primer salto y no confía en ninguna
#   cabecera X-Forwarded-* del cliente.
#   Con valor: de esas redes se aceptan la IP real del cliente (X-Forwarded-For,
#   para el rate limiting del backend) y el esquema/puerto públicos
#   (X-Forwarded-Proto/Port, para que Spring no confunda https con CORS).
set -eu
set -f

salida=/etc/nginx/conf.d/00-proxies-confiables.conf
cidrs=$(printf '%s' "${NGINX_TRUSTED_PROXIES:-}" | tr ',' ' ')

hay_proxies=0
for cidr in $cidrs; do
    case "$cidr" in
        *[!0-9a-fA-F:./]*)
            echo "$0: NGINX_TRUSTED_PROXIES contiene un valor no válido: '$cidr'" >&2
            exit 1
            ;;
    esac
    hay_proxies=1
done

{
    echo "# Generado por proxies-confiables.sh al arrancar. No editar."
    if [ "$hay_proxies" = 1 ]; then
        for cidr in $cidrs; do
            echo "set_real_ip_from $cidr;"
        done
        echo "real_ip_header X-Forwarded-For;"
        echo "real_ip_recursive on;"
    fi
    # $realip_remote_addr es quien abrió la conexión (el proxy), no el cliente.
    echo 'geo $realip_remote_addr $sgt_proxy_confiable {'
    echo '    default 0;'
    for cidr in $cidrs; do
        echo "    $cidr 1;"
    done
    echo '}'
} > "$salida"

if [ "$hay_proxies" = 1 ]; then
    echo "$0: proxies confiables: $cidrs"
else
    echo "$0: sin proxies confiables (nginx es el primer salto)"
fi
