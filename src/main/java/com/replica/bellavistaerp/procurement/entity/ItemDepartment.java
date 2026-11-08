package com.replica.bellavistaerp.procurement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "item_departments")
@Getter
@Setter
@NoArgsConstructor
public class ItemDepartment {

   @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}