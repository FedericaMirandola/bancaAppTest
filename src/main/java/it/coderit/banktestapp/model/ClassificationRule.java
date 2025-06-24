package it.coderit.banktestapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "regola_classificazione")
public class ClassificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "parola_chiave", nullable = false, length = 255)
    private String keyword;

    @Column(name = "json_rule", columnDefinition = "TEXT")
    private String jsonRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CenterType center;

    public CenterType getCenterType() {
        return center;
    }

   
}
