package com.dusk0382.cecosesolaprecios.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * DTOs del GraphQL oficial: POST {query:"{ downloadFile }"} →
 * data.downloadFile es un STRING con el JSON real adentro.
 *
 * Defensivo por diseño: es una API interna no documentada. Todo lo que pueda
 * faltar o llegar con tipo raro (ids que a veces son int y a veces string,
 * montos como {"$numberDecimal":"1.68"}) se tolera sin romper el parseo.
 */

@Serializable
data class GraphqlEnvelope(val data: GraphqlData?)

@Serializable
data class GraphqlData(
    @SerialName("downloadFile") val downloadFile: String? = null,
)

@Serializable
data class PayloadOficial(
    val priceList: Wrapper<PayloadPriceList>? = null,
    val officialRate: Wrapper<PayloadOfficialRate>? = null,
    val branches: List<JsonElement>? = null,
    val products: List<Wrapper<PayloadProducto>>? = null,
)

/** Muchos nodos de esta API llegan envueltos en {"_ns": "...", "model": {...}} */
@Serializable
data class Wrapper<T>(val model: T)

@Serializable
data class PayloadPriceList(
    val version: Int? = null,
    @SerialName("productsIncluded") val productsIncluded: Int? = null,
)

@Serializable
data class PayloadOfficialRate(
    val base: String? = null,
    @SerialName("forSales") val forSales: List<RateTarget>? = null,
)

@Serializable
data class RateTarget(
    val destination: String? = null,
    val value: DecimalAmount? = null,
)

/** {"$numberDecimal": "832.49"} */
@Serializable
data class DecimalAmount(
    @SerialName("\$numberDecimal") val numberDecimal: String? = null,
)

fun DecimalAmount?.toDoubleOrNull(): Double? =
    this?.numberDecimal?.replace(',', '.')?.toDoubleOrNull()

@Serializable
data class PayloadProducto(
    val status: String? = null,
    @Serializable(with = FlexIdSerializer::class)
    @SerialName("_id") val id: String? = null,
    val name: String? = null,
    val barcode: String? = null,
    val brand: String? = null,
    val presentation: String? = null,
    val tags: List<String>? = null,
    val images: List<String>? = null,
    @SerialName("pricePublished") val pricePublished: PayloadPrice? = null,
    val updatedAt: String? = null,
)

@Serializable
data class PayloadPrice(
    @SerialName("priceBase") val priceBase: PayloadMoney? = null,
    @SerialName("oldPrice") val oldPrice: PayloadMoney? = null,
    val updatedAt: String? = null,
)

@Serializable
data class PayloadMoney(
    val amount: DecimalAmount? = null,
    val currencyCode: String? = null,
)

/** Acepta ints, strings, nulls o objetos raros sin tumbar el parseo. */
object FlexIdSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val el = (decoder as? kotlinx.serialization.json.JsonDecoder)
            ?.decodeJsonElement() ?: return decoder.decodeString()
        return when (el) {
            is JsonNull -> null
            is JsonPrimitive -> el.content.takeIf { it.isNotBlank() }
            else -> el.toString().takeIf { it.length < 64 }
        }
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: String?) {
        encoder.encodeString(value ?: "")
    }
}

/** status puede venir en cualquier forma; aquí solo nos importa ACTIVE-ish */
fun PayloadProducto.activo(): Boolean =
    status == null || status.equals("ACTIVE", ignoreCase = true)
