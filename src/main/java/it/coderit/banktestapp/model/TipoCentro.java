package it.coderit.banktestapp.model;

public enum TipoCentro {
    COSTO("Costo"),
    PROFITTO("Profitto");
    private final String name; 

    private TipoCentro(String name) {
        this.name = name; // il nome è quello tra le virgolette
    }
   
    public String getName() {
        return name; //è il name tra le virgolette
    }

    public static TipoCentro fromName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Il nome non può essere nullo o vuoto");
        }
        for (TipoCentro tipo : TipoCentro.values()) {
            if (tipo.getName().equalsIgnoreCase(name)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("TipoCentro non trovato per il nome: " + name);
    }
}
