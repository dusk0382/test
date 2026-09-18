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
