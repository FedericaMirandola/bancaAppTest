package it.coderit.banktestapp.dto;

import it.coderit.banktestapp.model.CenterType;
import lombok.Data;


@Data
public class RuleInput {
    
   
    public String keyword;
    
    public CenterType centerType;
    
    public RuleInput() {}

    public RuleInput(String keyword, CenterType centerType) {
        this.keyword = keyword;
        this.centerType = centerType;
    }
}
