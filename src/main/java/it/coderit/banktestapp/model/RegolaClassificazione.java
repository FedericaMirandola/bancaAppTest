package it.coderit.banktestapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "regola_classificazione")
public class RegolaClassificazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parola_chiave", nullable = false, length = 255)
    private String parolaChiave;

    @Column(name = "json_rule", columnDefinition = "TEXT")
    private String jsonRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCentro centro;

    public TipoCentro getTipoCentro() {
        return centro;
    }

   
}
