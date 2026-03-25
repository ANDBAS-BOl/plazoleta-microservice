package com.pragma.powerup.plazoleta.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "employee_restaurant", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"idEmpleado", "restaurant_id"})
})
@Getter
@Setter
public class EmployeeRestaurantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idEmpleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private RestaurantEntity restaurant;
}
