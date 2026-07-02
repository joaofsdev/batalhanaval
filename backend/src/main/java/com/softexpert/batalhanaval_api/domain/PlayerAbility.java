package com.softexpert.batalhanaval_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "player_abilities", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ability_game_user", columnNames = {"game_id", "user_id"})
}, indexes = {
    @Index(name = "idx_ability_game", columnList = "game_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PlayerAbility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbilityType abilityType;

    @Column(nullable = false)
    private boolean used;

    @Column
    private Integer usedOnTurn;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerAbility that = (PlayerAbility) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
