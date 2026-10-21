package com.dailydiet.user;

import com.dailydiet.common.ResponseResult;
import com.dailydiet.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseResult<?> me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(userService.getById(userId));
    }

    @PutMapping
    public ResponseResult<?> update(Authentication auth, @Valid @RequestBody UserUpdateRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(userService.updateBasicInfo(userId, req));
    }

    @PutMapping("/training")
    public ResponseResult<?> updateTraining(Authentication auth, @Valid @RequestBody TrainingUpdateRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(userService.updateTraining(userId, req));
    }

    @PutMapping("/goal")
    public ResponseResult<?> updateGoal(Authentication auth, @Valid @RequestBody GoalUpdateRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(userService.updateGoal(userId, req));
    }

    @GetMapping("/target")
    public ResponseResult<?> target(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(userService.getTarget(userId));
    }
}
