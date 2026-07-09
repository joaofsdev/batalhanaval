package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.AbilityRotationResult;
import com.softexpert.batalhanaval_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbilityRotationTest {

    @Mock private GameRepository gameRepository;
    @Mock private PlayerAbilityRepository playerAbilityRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private CellRepository cellRepository;
    @Mock private ShipRepository shipRepository;
    @Mock private ShotRepository shotRepository;
    @Mock private StormService stormService;
    @Mock private VictoryService victoryService;

    @InjectMocks private AbilityService abilityService;

    private Game game;
    private UUID gameId;
    private User player1;
    private User player2;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        player1 = new User();
        player1.setId(UUID.randomUUID());
        player2 = new User();
        player2.setId(UUID.randomUUID());

        game = new Game();
        game.setId(gameId);
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setGameMode(GameMode.STORM);
        game.setCurrentTurn(player1);
        game.setCurrentTurnNumber(4);
        game.setNextAbilityRotationTurn(4);
    }

    @Test
    void rotateAbilities_unusedAbility_wasDiscardedIsTrue() {
        PlayerAbility p1Ability = createAbility(player1, AbilityType.RADAR, false, null);
        PlayerAbility p2Ability = createAbility(player2, AbilityType.SHIELD, false, null);

        when(playerAbilityRepository.findByGameId(gameId))
            .thenReturn(List.of(p1Ability, p2Ability));

        List<AbilityRotationResult> results = abilityService.rotateAbilities(game);

        assertThat(results).hasSize(2);

        // Both abilities were unused → wasDiscarded = true
        assertThat(results.get(0).previousWasDiscarded()).isTrue();
        assertThat(results.get(1).previousWasDiscarded()).isTrue();

        // Previous types are preserved in the result
        assertThat(results.get(0).previousAbility()).isEqualTo(AbilityType.RADAR);
        assertThat(results.get(1).previousAbility()).isEqualTo(AbilityType.SHIELD);

        // Abilities are reset to unused
        assertThat(p1Ability.isUsed()).isFalse();
        assertThat(p1Ability.getUsedOnTurn()).isNull();
        assertThat(p2Ability.isUsed()).isFalse();
        assertThat(p2Ability.getUsedOnTurn()).isNull();

        // New ability types are set (non-null)
        assertThat(p1Ability.getAbilityType()).isNotNull();
        assertThat(p2Ability.getAbilityType()).isNotNull();

        verify(playerAbilityRepository, times(2)).save(any(PlayerAbility.class));
    }

    @Test
    void rotateAbilities_usedAbility_wasDiscardedIsFalse() {
        PlayerAbility p1Ability = createAbility(player1, AbilityType.RADAR, true, 2);
        PlayerAbility p2Ability = createAbility(player2, AbilityType.DOUBLE_TORPEDO, true, 3);

        when(playerAbilityRepository.findByGameId(gameId))
            .thenReturn(List.of(p1Ability, p2Ability));

        List<AbilityRotationResult> results = abilityService.rotateAbilities(game);

        assertThat(results).hasSize(2);

        // Both abilities were used → wasDiscarded = false
        assertThat(results.get(0).previousWasDiscarded()).isFalse();
        assertThat(results.get(1).previousWasDiscarded()).isFalse();

        // After rotation, abilities are reset to unused
        assertThat(p1Ability.isUsed()).isFalse();
        assertThat(p1Ability.getUsedOnTurn()).isNull();
        assertThat(p2Ability.isUsed()).isFalse();
        assertThat(p2Ability.getUsedOnTurn()).isNull();
    }

    @Test
    void rotateAbilities_shieldActiveNotConsumed_isOverwrittenNormally() {
        // SHIELD was used (usedOnTurn >= 0) but not consumed (would be -1 if consumed)
        // This means it's "active" — waiting to block a shot
        PlayerAbility shieldAbility = createAbility(player1, AbilityType.SHIELD, true, 3);
        // usedOnTurn = 3 (not -1), so shield is active but not consumed

        PlayerAbility p2Ability = createAbility(player2, AbilityType.RADAR, false, null);

        when(playerAbilityRepository.findByGameId(gameId))
            .thenReturn(List.of(shieldAbility, p2Ability));

        List<AbilityRotationResult> results = abilityService.rotateAbilities(game);

        assertThat(results).hasSize(2);

        // Shield was used (used=true) → wasDiscarded = false (it was used, just not consumed)
        assertThat(results.get(0).previousWasDiscarded()).isFalse();
        assertThat(results.get(0).previousAbility()).isEqualTo(AbilityType.SHIELD);

        // Shield ability is now overwritten — no longer SHIELD, so isShieldActiveAndNotConsumed will fail
        assertThat(shieldAbility.isUsed()).isFalse();
        assertThat(shieldAbility.getUsedOnTurn()).isNull();
        // New type is set (could be anything, including SHIELD again by random chance)
        assertThat(shieldAbility.getAbilityType()).isNotNull();
    }

    @Test
    void rotateAbilities_nextRotationTurnAdvancesBy3() {
        PlayerAbility p1Ability = createAbility(player1, AbilityType.RADAR, false, null);
        PlayerAbility p2Ability = createAbility(player2, AbilityType.SHIELD, false, null);

        when(playerAbilityRepository.findByGameId(gameId))
            .thenReturn(List.of(p1Ability, p2Ability));

        // currentTurnNumber = 4, nextAbilityRotationTurn should become 4 + 3 = 7
        abilityService.rotateAbilities(game);

        assertThat(game.getNextAbilityRotationTurn()).isEqualTo(7);
    }

    @Test
    void rotateAbilities_secondRotation_nextRotationTurnAdvancesCorrectly() {
        game.setCurrentTurnNumber(7);
        game.setNextAbilityRotationTurn(7);

        PlayerAbility p1Ability = createAbility(player1, AbilityType.LINE_BOMBARDMENT, true, 5);
        PlayerAbility p2Ability = createAbility(player2, AbilityType.DOUBLE_TORPEDO, false, null);

        when(playerAbilityRepository.findByGameId(gameId))
            .thenReturn(List.of(p1Ability, p2Ability));

        abilityService.rotateAbilities(game);

        // nextAbilityRotationTurn should be 7 + 3 = 10
        assertThat(game.getNextAbilityRotationTurn()).isEqualTo(10);

        // Mixed: p1 used (wasDiscarded=false), p2 unused (wasDiscarded=true)
    }

    @Test
    void rotateAbilities_returnsCorrectDTOStructure() {
        PlayerAbility p1Ability = createAbility(player1, AbilityType.DOUBLE_TORPEDO, false, null);

        when(playerAbilityRepository.findByGameId(gameId))
            .thenReturn(List.of(p1Ability));

        List<AbilityRotationResult> results = abilityService.rotateAbilities(game);

        assertThat(results).hasSize(1);
        AbilityRotationResult result = results.get(0);

        assertThat(result.playerId()).isEqualTo(player1.getId());
        assertThat(result.previousAbility()).isEqualTo(AbilityType.DOUBLE_TORPEDO);
        assertThat(result.previousWasDiscarded()).isTrue();
        assertThat(result.newAbility()).isNotNull();
        assertThat(result.newAbilityName()).isNotNull();
        assertThat(result.newAbilityDescription()).isNotNull();

        // New ability name/description should match the new type
        assertThat(result.newAbilityName()).isEqualTo(result.newAbility().getDisplayName());
        assertThat(result.newAbilityDescription()).isEqualTo(result.newAbility().getDescription());
    }

    @Test
    void initializeAbilities_setsNextAbilityRotationTurn() {
        game.setCurrentTurnNumber(1);

        abilityService.initializeAbilities(game);

        // Should schedule first rotation at turn 1 + 3 = 4
        assertThat(game.getNextAbilityRotationTurn()).isEqualTo(4);

        // Should create 2 abilities (one per player)
        verify(playerAbilityRepository, times(2)).save(any(PlayerAbility.class));
    }

    @Test
    void initializeAbilities_classicMode_doesNothing() {
        game.setGameMode(GameMode.CLASSIC);
        game.setCurrentTurnNumber(1);
        int originalRotationTurn = game.getNextAbilityRotationTurn();

        abilityService.initializeAbilities(game);

        // Should not create any abilities
        verify(playerAbilityRepository, never()).save(any(PlayerAbility.class));
        // Should not modify nextAbilityRotationTurn
        assertThat(game.getNextAbilityRotationTurn()).isEqualTo(originalRotationTurn);
    }

    // --- Helper ---

    private PlayerAbility createAbility(User user, AbilityType type, boolean used, Integer usedOnTurn) {
        PlayerAbility ability = new PlayerAbility();
        ability.setId(UUID.randomUUID());
        ability.setGame(game);
        ability.setUser(user);
        ability.setAbilityType(type);
        ability.setUsed(used);
        ability.setUsedOnTurn(usedOnTurn);
        return ability;
    }
}
