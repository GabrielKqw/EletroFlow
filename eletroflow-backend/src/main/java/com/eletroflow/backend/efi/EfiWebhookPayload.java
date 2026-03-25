package com.eletroflow.backend.efi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EfiWebhookPayload(List<PixItem> pix) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PixItem(
            String endToEndId,
            String txid,
            BigDecimal valor,
            OffsetDateTime horario,
            GnExtras gnExtras
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GnExtras(Pagador pagador) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagador(String cpf, String cnpj) {
    }
}
