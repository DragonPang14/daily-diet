package com.dailydiet.user;

import com.dailydiet.algorithm.BmrCalculator;
import com.dailydiet.algorithm.MuscleEstimator;
import com.dailydiet.algorithm.TdeeCalculator;
import com.dailydiet.common.BizException;
import com.dailydiet.common.enums.GoalType;
import com.dailydiet.user.dto.*;
import com.dailydiet.user.entity.User;
import com.dailydiet.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BmrCalculator bmrCalculator;
    private final MuscleEstimator muscleEstimator;
    private final TdeeCalculator tdeeCalculator;

    public User getById(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException("用户不存在");
        return u;
    }

    @Transactional
    public User updateBasicInfo(Long userId, UserUpdateRequest req) {
        User u = getById(userId);
        u.setGender(req.getGender());
        u.setBirthday(req.getBirthday());
        u.setHeight(req.getHeight());
        u.setCurrentWeight(req.getCurrentWeight());
        u.setTargetWeight(req.getTargetWeight());
        recalculate(u);
        userMapper.updateById(u);
        return u;
    }

    @Transactional
    public User updateTraining(Long userId, TrainingUpdateRequest req) {
        User u = getById(userId);
        u.setSquat1rm(req.getSquat1rm());
        u.setBench1rm(req.getBench1rm());
        u.setDeadlift1rm(req.getDeadlift1rm());
        u.setTrainingLevel(req.getTrainingLevel() != null ? req.getTrainingLevel() : 0);
        recalculate(u);
        userMapper.updateById(u);
        return u;
    }

    @Transactional
    public User updateGoal(Long userId, GoalUpdateRequest req) {
        User u = getById(userId);
        u.setGoalType(req.getGoalType());
        u.setCustomTdee(req.getCustomTdee());

        // 若用户传了 PFC 则用用户值，否则按 goal_type 设默认
        if (req.getPfcCarboPct() != null && req.getPfcProteinPct() != null && req.getPfcFatPct() != null) {
            int sum = req.getPfcCarboPct() + req.getPfcProteinPct() + req.getPfcFatPct();
            if (sum != 100) throw new BizException("PFC 比例之和必须为 100，当前: " + sum);
            u.setPfcCarboPct(req.getPfcCarboPct());
            u.setPfcProteinPct(req.getPfcProteinPct());
            u.setPfcFatPct(req.getPfcFatPct());
        } else {
            int[] pfc = tdeeCalculator.defaultPfcRatio(GoalType.fromCode(req.getGoalType()));
            u.setPfcCarboPct(pfc[0]);
            u.setPfcProteinPct(pfc[1]);
            u.setPfcFatPct(pfc[2]);
        }

        recalculate(u);
        userMapper.updateById(u);
        return u;
    }

    public TargetResponse getTarget(Long userId) {
        User u = getById(userId);
        TargetResponse resp = new TargetResponse();
        resp.setBmr(u.getBmr());
        resp.setTdee(u.getTdee());

        GoalType goal = GoalType.fromCode(u.getGoalType());
        int targetKcal = tdeeCalculator.applyGoal(
                u.getTdee().doubleValue(), goal, u.getCustomTdee()
        );
        resp.setTargetKcal(targetKcal);
        resp.setPfcCarboPct(u.getPfcCarboPct());
        resp.setPfcProteinPct(u.getPfcProteinPct());
        resp.setPfcFatPct(u.getPfcFatPct());

        // PFC 克数
        double carboKcal = targetKcal * u.getPfcCarboPct() / 100.0;
        double proteinKcal = targetKcal * u.getPfcProteinPct() / 100.0;
        double fatKcal = targetKcal * u.getPfcFatPct() / 100.0;
        resp.setTargetCarboG(carboKcal / 4);
        resp.setTargetProteinG(proteinKcal / 4);
        resp.setTargetFatG(fatKcal / 9);
        return resp;
    }

    /**
     * 核心：BMR → 肌肉修正 → TDEE → 目标偏移
     */
    void recalculate(User u) {
        if (u.getGender() == null || u.getBirthday() == null ||
            u.getHeight() == null || u.getCurrentWeight() == null) {
            return;
        }

        double bmr = bmrCalculator.mifflinStJeor(
                u.getGender(),
                u.getCurrentWeight().doubleValue(),
                u.getHeight().doubleValue(),
                u.getBirthday()
        );

        double muscleFactor = muscleEstimator.correct(
                u.getGender(),
                u.getCurrentWeight().doubleValue(),
                u.getSquat1rm(),
                u.getBench1rm(),
                u.getDeadlift1rm()
        );

        double correctedBmr = bmr * muscleFactor;
        int tLevel = u.getTrainingLevel() != null ? u.getTrainingLevel() : 0;
        double tdee = tdeeCalculator.calculate(correctedBmr, tLevel);

        u.setBmr(BigDecimal.valueOf(Math.round(bmr * 100) / 100.0));
        u.setMuscleFactor(BigDecimal.valueOf(muscleFactor));
        u.setTdee(BigDecimal.valueOf(Math.round(tdee * 100) / 100.0));
    }
}
