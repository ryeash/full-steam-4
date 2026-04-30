package com.fullsteam.model;

import io.micronaut.core.annotation.Introspected;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One skirmish roster slot (human or AI) and its alliance team id (1..mapTeamCount).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Introspected
public class SkirmishSlotConfig {

    @Builder.Default
    private SkirmishSlotKind kind = SkirmishSlotKind.HUMAN;

    /**
     * Alliance / victory team (1-based). Multiple slots may share a team id (e.g. 2v2).
     */
    @Builder.Default
    private int teamId = 1;

    /**
     * For {@link SkirmishSlotKind#AI} slots only; ignored for humans. Defaults to {@link AiDifficulty#NORMAL}.
     */
    @Builder.Default
    private AiDifficulty aiDifficulty = AiDifficulty.NORMAL;
}
