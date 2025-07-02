package it.coderit.banktestapp.dto;

import it.coderit.banktestapp.model.CenterType;
import lombok.Data;

@Data
public class RuleInput {
    public String keyword;
    public CenterType center;
    public String jsonRule;
}
