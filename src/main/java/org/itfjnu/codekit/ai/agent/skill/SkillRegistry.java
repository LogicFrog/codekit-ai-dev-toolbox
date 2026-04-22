package org.itfjnu.codekit.ai.agent.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SkillRegistry {

    private final List<Skill> skills;

    public Optional<Skill> findByName(String name) {
        Map<String, Skill> map = skills.stream()
                .collect(Collectors.toMap(Skill::name, Function.identity(), (a, b) -> a));

        return Optional.ofNullable(map.get(name));
    }
 }
