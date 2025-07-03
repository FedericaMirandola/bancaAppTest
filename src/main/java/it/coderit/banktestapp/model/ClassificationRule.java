package it.coderit.banktestapp.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Data
@Table(name = "classification_rule")
public class ClassificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "center_type", nullable = false)
    private CenterType centerType;

    public ClassificationRule() {}

    public ClassificationRule(String keyword, CenterType centerType) {
        this.keyword = keyword;
        this.centerType = centerType;
    }

    public CenterType getCenterType() {
        return centerType;
    }

   
}
