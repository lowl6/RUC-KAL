package cn.edu.ruc.kal.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class ProjectDtos {

    public record RoleDto(String roleName, Integer count, String skills) {}

    public record CreateReq(
            @NotBlank String projectName,
            String oneLiner,
            @NotNull String projectType,
            String competitionShort,
            String competitionTarget,
            LocalDate competitionDeadline,
            LocalDate teamDeadline,
            Integer currentMembers,
            Integer neededCount,
            Integer weeklyHours,
            String detail,
            List<String> tags,
            List<RoleDto> roles
    ) {}

    public record View(
            String projectId,
            String projectName,
            String oneLiner,
            String projectType,
            String competitionShort,
            String competitionTarget,
            LocalDate competitionDeadline,
            LocalDate teamDeadline,
            Integer currentMembers,
            Integer neededCount,
            Integer weeklyHours,
            String detail,
            List<String> tags,
            String creatorId,
            Integer viewCount,
            Integer applyCount,
            String status,
            List<RoleDto> roles
    ) {}
}
