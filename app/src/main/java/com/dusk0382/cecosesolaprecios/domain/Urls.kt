package com.dusk0382.cecosesolaprecios.domain

/**
 * Las URLs de imagen que trae la API oficial vienen en `http://`, incluyendo los
 * dos hosts de Cecosesola. Medido contra el fixture real: 453 de 527 referencias
 * de imagen son http, en ~55 hosts distintos, y el `network_security_config` solo
 * estaba allowlistado.
 *
 * Con el allowlist anterior, 307 imágenes cargaban y **146 no**, sin explicación
 * visible: la tarjeta dibujaba el contenedor tonal vacío y nada más.
 *
 * La corrección no es ampliar la lista de hosts, sino dejar de necesitar cleartext.
 * Los dos hosts de Cecosesola responden por https (200) y los http solo redirigen
 * (308), así que subir el esquema funciona. Y de paso cierra un agujero real: con
 * cleartext permitido, un atacante en el camino podía inyectar tanto las imágenes
 * como —porque el host del GraphQL es el mismo— el payload de precios entero.
 *
 * Una URL ya en https, o que no sea http, se devuelve tal cual.
 */
fun aHttps(url: String): String =
    if (url.startsWith("http://", ignoreCase = true)) {
        "https://" + url.substring("http://".length)
    } else {
        url
    }
