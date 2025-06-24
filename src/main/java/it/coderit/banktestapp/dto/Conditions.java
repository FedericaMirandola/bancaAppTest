package it.coderit.banktestapp.dto;

import java.util.List;

import lombok.Data;

@Data
public class Conditions {
    
    public String field;
    public List<String> keywords;
}
