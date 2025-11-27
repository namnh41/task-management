package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.TaskRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.Task.TaskStatus;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.exception.AccessDeniedException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        User user = getCurrentUser();
        log.info("Creating task for user: {}", user.getUsername());

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING);
        task.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        task.setDeadline(request.getDeadline());
        task.setUser(user);

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    public Page<TaskResponse> getAllTasks(TaskStatus status, Pageable pageable) {
        User user = getCurrentUser();

        if (isAdmin()) {
            if (status != null) {
                return taskRepository.findByStatus(status, pageable).map(this::mapToResponse);
            }
            return taskRepository.findAll(pageable).map(this::mapToResponse);
        } else {
            if (status != null) {
                return taskRepository.findByUserIdAndStatus(user.getId(), status, pageable)
                        .map(this::mapToResponse);
            }
            return taskRepository.findByUserId(user.getId(), pageable).map(this::mapToResponse);
        }
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = getCurrentUser();
        if (!task.getUser().getId().equals(currentUser.getId()) && !isAdmin()) {
            throw new AccessDeniedException("You don't have permission to view this task");
        }

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = getCurrentUser();
        if (!task.getUser().getId().equals(currentUser.getId()) && !isAdmin()) {
            throw new AccessDeniedException("You don't have permission to update this task");
        }

        log.info("Updating task: {}", id);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        task.setDeadline(request.getDeadline());

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = getCurrentUser();
        if (!task.getUser().getId().equals(currentUser.getId()) && !isAdmin()) {
            throw new AccessDeniedException("You don't have permission to delete this task");
        }

        log.info("Deleting task: {}", id);
        taskRepository.delete(task);
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .deadline(task.getDeadline())
                .userId(task.getUser().getId())
                .username(task.getUser().getUsername())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}