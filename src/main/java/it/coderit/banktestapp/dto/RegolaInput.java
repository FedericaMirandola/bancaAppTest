package it.coderit.banktestapp.dto;

import it.coderit.banktestapp.model.TipoCentro;
import lombok.Data;

@Data
public class RegolaInput {
    public String parolaChiave;
    public TipoCentro centro;
    public String jsonRule;
}
