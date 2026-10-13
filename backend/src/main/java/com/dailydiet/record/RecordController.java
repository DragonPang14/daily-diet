package com.dailydiet.record;

import com.dailydiet.common.ResponseResult;
import com.dailydiet.record.dto.AddRecordRequest;
import com.dailydiet.record.dto.BulkRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @PostMapping("/today/bulk")
    public ResponseResult<?> bulk(Authentication auth, @RequestBody BulkRequest req) {
        Long userId = (Long) auth.getPrincipal();
        recordService.bulkFromRecipe(userId, req.getRecipeId());
        return ResponseResult.success();
    }

    @PostMapping
    public ResponseResult<?> add(Authentication auth, @Valid @RequestBody AddRecordRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recordService.add(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseResult<?> update(Authentication auth, @PathVariable Long id,
                                     @Valid @RequestBody AddRecordRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recordService.update(id, userId, req));
    }

    @DeleteMapping("/{id}")
    public ResponseResult<?> delete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        recordService.delete(id, userId);
        return ResponseResult.success();
    }

    @GetMapping("/today/summary")
    public ResponseResult<?> todaySummary(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recordService.getTodaySummary(userId));
    }

    @GetMapping("/{date}")
    public ResponseResult<?> getByDate(Authentication auth, @PathVariable String date) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recordService.getByDate(userId, LocalDate.parse(date)));
    }
}
