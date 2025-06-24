package it.coderit.banktestapp.model;

public enum CenterType {
    COSTO("Costo"),
    PROFITTO("Profitto");
    private final String name; 

    private CenterType(String name) {
        this.name = name; // il nome è quello tra le virgolette
    }
   
    public String getName() {
        return name; //è il name tra le virgolette
    }

    public static CenterType fromName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Il nome non può essere nullo o vuoto");
        }
        for (CenterType tipo : CenterType.values()) {
            if (tipo.getName().equalsIgnoreCase(name)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("CenterType non trovato per il nome: " + name);
    }
}
