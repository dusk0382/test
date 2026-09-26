package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.ui.theme.Paleta
import com.dusk0382.cecosesolaprecios.ui.theme.ratioContraste
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El diseño se juzga con cuentas, no a ojo (DESIGN.md §6.2).
 *
 * "Tema oscuro que apenas pasa el contraste" es uno de los tell clásicos de UI
 * generada, y además es un problema real: en un Helio G25 con sol encima, un
 * 3.4:1 en texto normal no se lee.
 *
 * Los pares de abajo son los que la app **realmente pinta**. Si alguien cambia un
 * color y deja de pasar AA, CI se pone rojo antes de que salga a la calle.
 */
class TemaContrasteTest {

    /** Cuerpo de texto: WCAG AA exige 4.5:1. */
    private val AA_TEXTO = 4.5

    /** Texto grande (>=24sp, o >=18.66sp en negrita) e interfaz: 3:1. */
    private val AA_GRANDE = 3.0

    private fun verificar(nombre: String, primerPlano: Long, fondo: Long, minimo: Double) {
        val ratio = ratioContraste(primerPlano, fondo)
        assertTrue(
            "$nombre: ${"%.2f".format(ratio)}:1 (se exige $minimo:1)",
            ratio >= minimo,
        )
    }

    @Test
    fun `tema claro cumple AA en los pares que usa la app`() {
        // Texto sobre superficies
        verificar("onSurface sobre surface", Paleta.SobreSuperficieClaro, Paleta.SuperficieClaro, AA_TEXTO)
        verificar("onSurfaceVariant sobre surface", Paleta.SobreSuperficieVarianteClaro, Paleta.SuperficieClaro, AA_TEXTO)
        verificar("onSurface sobre surfaceContainerHigh", Paleta.SobreSuperficieClaro, Paleta.SuperficieAltaClaro, AA_TEXTO)
        verificar("onSurfaceVariant sobre surfaceContainerHigh", Paleta.SobreSuperficieVarianteClaro, Paleta.SuperficieAltaClaro, AA_TEXTO)
        verificar("onSurface sobre surfaceContainerHighest", Paleta.SobreSuperficieClaro, Paleta.SuperficieMaximaClaro, AA_TEXTO)

        // Acción primaria: relleno naranja con tinta oscura, NO blanco
        verificar("onPrimary sobre primary (botón)", Paleta.TintaSobreMarca, Paleta.NaranjaMarca, AA_TEXTO)
        verificar("onPrimaryContainer sobre primaryContainer", Paleta.SobreMarcaContenedorClaro, Paleta.MarcaContenedorClaro, AA_TEXTO)

        // Precio y apoyos
        verificar("acento de precio sobre surface", Paleta.AcentoPrecioClaro, Paleta.SuperficieClaro, AA_TEXTO)
        verificar("acento de precio sobre surfaceContainerHighest", Paleta.AcentoPrecioClaro, Paleta.SuperficieMaximaClaro, AA_TEXTO)
        verificar("precio que sube", Paleta.SubeClaro, Paleta.SuperficieClaro, AA_TEXTO)
        verificar("precio que baja", Paleta.BajaClaro, Paleta.SuperficieClaro, AA_TEXTO)

        // Secundario/terciario definidos (antes caían al violeta por defecto)
        verificar("onSecondary sobre secondary", Paleta.SobreSecundarioClaro, Paleta.SecundarioClaro, AA_TEXTO)
        verificar("onSecondaryContainer sobre secondaryContainer", Paleta.SobreSecundarioContenedorClaro, Paleta.SecundarioContenedorClaro, AA_TEXTO)
        verificar("onErrorContainer sobre errorContainer", Paleta.SobreErrorContenedorClaro, Paleta.ErrorContenedorClaro, AA_TEXTO)

        // Bordes e interfaz: 3:1
        verificar("outline sobre surface (borde)", Paleta.ContornoClaro, Paleta.SuperficieClaro, AA_GRANDE)
    }

    @Test
    fun `tema oscuro cumple AA en los pares que usa la app`() {
        verificar("onSurface sobre surface", Paleta.SobreSuperficieOscuro, Paleta.SuperficieOscura, AA_TEXTO)
        verificar("onSurfaceVariant sobre surface", Paleta.SobreSuperficieVarianteOscuro, Paleta.SuperficieOscura, AA_TEXTO)
        verificar("onSurface sobre surfaceContainerHigh", Paleta.SobreSuperficieOscuro, Paleta.SuperficieAltaOscura, AA_TEXTO)
        verificar("onSurfaceVariant sobre surfaceContainerHigh", Paleta.SobreSuperficieVarianteOscuro, Paleta.SuperficieAltaOscura, AA_TEXTO)
        verificar("onSurface sobre surfaceContainerHighest", Paleta.SobreSuperficieOscuro, Paleta.SuperficieMaximaOscura, AA_TEXTO)

        verificar("onPrimary sobre primary (botón)", Paleta.TintaSobreMarca, Paleta.AcentoPrecioOscuro, AA_TEXTO)
        verificar("onPrimaryContainer sobre primaryContainer", Paleta.SobreMarcaContenedorOscuro, Paleta.MarcaContenedorOscuro, AA_TEXTO)

        verificar("acento de precio sobre surface", Paleta.AcentoPrecioOscuro, Paleta.SuperficieOscura, AA_TEXTO)
        verificar("acento de precio sobre surfaceContainerHighest", Paleta.AcentoPrecioOscuro, Paleta.SuperficieMaximaOscura, AA_TEXTO)
        verificar("precio que sube", Paleta.SubeOscuro, Paleta.SuperficieOscura, AA_TEXTO)
        verificar("precio que baja", Paleta.BajaOscuro, Paleta.SuperficieOscura, AA_TEXTO)

        verificar("onSecondary sobre secondary", Paleta.SobreSecundarioOscuro, Paleta.SecundarioOscuro, AA_TEXTO)
        verificar("onSecondaryContainer sobre secondaryContainer", Paleta.SobreSecundarioContenedorOscuro, Paleta.SecundarioContenedorOscuro, AA_TEXTO)
        verificar("onErrorContainer sobre errorContainer", Paleta.SobreErrorContenedorOscuro, Paleta.ErrorContenedorOscuro, AA_TEXTO)

        verificar("outline sobre surface (borde)", Paleta.ContornoOscuro, Paleta.SuperficieOscura, AA_GRANDE)
    }

    @Test
    fun `el naranja de marca nunca lleva texto blanco`() {
        // Este test existe porque el bug ya ocurrió: el boton "Agregar al carrito"
        // pinta blanco sobre #FD4902 = 3.43:1, que falla AA para texto normal.
        // Se deja escrito para que nadie lo revierta "porque se veia lindo".
        val blancoSobreMarca = ratioContraste(0xFFFFFFFFL, Paleta.NaranjaMarca)
        assertTrue(
            "blanco sobre el naranja de marca da ${"%.2f".format(blancoSobreMarca)}:1; por eso onPrimary es TintaSobreMarca",
            blancoSobreMarca < AA_TEXTO,
        )
        verificar("tinta sobre el naranja de marca", Paleta.TintaSobreMarca, Paleta.NaranjaMarca, AA_TEXTO)
    }

    @Test
    fun `el precio nunca usa el naranja de marca como color de texto`() {
        // El naranja de marca #FD4902 es relleno, no texto (DESIGN.md §3.11). El
        // precio se pinta con LocalColoresPrecio.acento (#B93300 en claro, 5.66:1).
        //
        // Estos dos pares no estaban en el test y por eso la app pudo llevar meses
        // con el precio del detalle y del carrito en 3.26:1 con el CI en verde: la
        // lista de pares se congeló cuando se escribió y nadie la volvió a abrir.
        val precio = Paleta.AcentoPrecioClaro
        val superficie = Paleta.SuperficieClaro
        val mayor = Paleta.SuperficieMaximaClaro

        verificar("acento de precio sobre surface", precio, superficie, AA_TEXTO)
        verificar("acento de precio sobre surfaceContainerHighest", precio, mayor, AA_TEXTO)

        // Sobre superficie, el naranja de marca da 3.26:1: **alcanza** el 3:1 de
        // texto grande pero **falla** el 4.5:1 de cuerpo. Por eso la regla no puede
        // ser "nunca uses primary como texto" (un titular gigante sí lo aguantaría)
        // sino "primary no es color de texto": depende del tamaño, y eso es
        // exactamente la clase de detalle que se pierde cuando el color se elige a
        // ojo y no por rol.
        assertTrue(
            "el naranja de marca como texto de precio da " +
                "${"%.2f".format(ratioContraste(Paleta.NaranjaMarca, superficie))}:1 sobre " +
                "surface: no llega al 4.5:1 de cuerpo, usá LocalColoresPrecio.acento",
            ratioContraste(Paleta.NaranjaMarca, superficie) < AA_TEXTO,
        )

        // Sobre el contenedor de la imagen es peor: 2.75:1 no llega ni al 3:1 de un
        // icono, así que el corazón de favorito con `primary` era casi invisible.
        assertTrue(
            "el naranja de marca sobre el contenedor de la imagen da " +
                "${"%.2f".format(ratioContraste(Paleta.NaranjaMarca, mayor))}:1: " +
                "no alcanza ni para un icono de 20 dp",
            ratioContraste(Paleta.NaranjaMarca, mayor) < AA_GRANDE,
        )

        // Y el token que sí se usa tiene que cumplir holgadamente en los dos fondos.
        assertTrue(
            "el acento de precio debería ir sobrado sobre superficie, no justo",
            ratioContraste(precio, superficie) > 5.0,
        )
    }

    @Test
    fun `el acento de precio se lee igual en tema claro y oscuro`() {
        // El precio es el elemento más grande de cada pantalla. Si el token no
        // llegara a 4.5:1 en oscuro, la app "anda bien en claro" y es ilegible de
        // noche, que es el escenario real de una feria.
        verificar(
            "acento de precio sobre surface (claro)",
            Paleta.AcentoPrecioClaro, Paleta.SuperficieClaro, AA_TEXTO,
        )
        verificar(
            "acento de precio sobre surface (oscuro)",
            Paleta.AcentoPrecioOscuro, Paleta.SuperficieOscura, AA_TEXTO,
        )
        verificar(
            "acento de precio sobre surfaceContainerHighest (oscuro)",
            Paleta.AcentoPrecioOscuro, Paleta.SuperficieMaximaOscura, AA_TEXTO,
        )
    }

    @Test
    fun `las superficies distinguen niveles sin depender de sombras`() {
        // La jerarquia se hace con tono, no con bordes de color ni sombras
        // (DESIGN.md §3.2): si los niveles de superficie son iguales, no hay jerarquia.
        assertTrue(
            "surfaceContainerHigh debe diferenciarse de surface en claro",
            Paleta.SuperficieAltaClaro != Paleta.SuperficieClaro,
        )
        assertTrue(
            "surfaceContainerHighest debe diferenciarse de surfaceContainerHigh en claro",
            Paleta.SuperficieMaximaClaro != Paleta.SuperficieAltaClaro,
        )
        assertTrue(
            "surfaceContainerHigh debe diferenciarse de surface en oscuro",
            Paleta.SuperficieAltaOscura != Paleta.SuperficieOscura,
        )
        assertTrue(
            "surfaceContainerHighest debe diferenciarse de surfaceContainerHigh en oscuro",
            Paleta.SuperficieMaximaOscura != Paleta.SuperficieAltaOscura,
        )
    }
}
