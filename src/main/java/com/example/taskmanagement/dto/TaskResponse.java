package com.example.taskmanagement.dto;

import com.example.taskmanagement.entity.Task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private Integer priority;

    private LocalDateTime deadline;

    private Long userId;

    private String username;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}