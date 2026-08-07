package com.example.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.entity.AllianceDict;
import com.example.api.entity.AllianceMember;
import com.example.api.entity.AllianceShowcase;
import com.example.api.entity.AllianceThanks;
import com.example.common.result.Result;
import com.example.service.dao.AllianceDictDao;
import com.example.service.dao.AllianceMemberDao;
import com.example.service.dao.AllianceShowcaseDao;
import com.example.service.dao.AllianceThanksDao;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alliance")
@RequiredArgsConstructor
public class AllianceController {

    private final AllianceDictDao dictDao;
    private final AllianceMemberDao memberDao;
    private final AllianceThanksDao thanksDao;
    private final AllianceShowcaseDao showcaseDao;

    /** 获取联系方式 */
    @GetMapping("/contact")
    public Result<Map<String, String>> getContact() {
        List<AllianceDict> list = dictDao.selectList(
            new LambdaQueryWrapper<AllianceDict>()
                .eq(AllianceDict::getDictType, "contact")
                .orderByAsc(AllianceDict::getSortOrder));
        Map<String, String> map = new LinkedHashMap<>();
        for (AllianceDict d : list) {
            map.put(d.getDictKey(), d.getDictValue());
        }
        return Result.success(map);
    }

    /** 获取招募信息 */
    @GetMapping("/recruit")
    public Result<Map<String, String>> getRecruit() {
        List<AllianceDict> list = dictDao.selectList(
            new LambdaQueryWrapper<AllianceDict>()
                .eq(AllianceDict::getDictType, "recruit")
                .orderByAsc(AllianceDict::getSortOrder));
        Map<String, String> map = new LinkedHashMap<>();
        for (AllianceDict d : list) {
            map.put(d.getDictKey(), d.getDictValue());
        }
        return Result.success(map);
    }

    /** 获取赛季名单 */
    @GetMapping("/roster/{season}")
    public Result<Map<String, Object>> getRoster(@PathVariable String season) {
        List<AllianceMember> all = memberDao.selectList(
            new LambdaQueryWrapper<AllianceMember>()
                .eq(AllianceMember::getSeason, season)
                .orderByAsc(AllianceMember::getSortOrder));

        Map<String, List<AllianceMember>> grouped = all.stream()
            .collect(Collectors.groupingBy(
                m -> "LEADER".equals(m.getRoleType()) ? "leader"
                   : "VICE_LEADER".equals(m.getRoleType()) ? "vice_leaders"
                   : "CORE_FIGHTER".equals(m.getRoleType()) ? "core_fighters"
                   : "ELDER".equals(m.getRoleType()) ? "elders"
                   : "SPONSOR".equals(m.getRoleType()) ? "sponsors"
                   : "members",
                LinkedHashMap::new, Collectors.toList()));

        // 赛季统计从字典读
        List<AllianceDict> stats = dictDao.selectList(
            new LambdaQueryWrapper<AllianceDict>()
                .eq(AllianceDict::getDictType, "roster_stat")
                .likeRight(AllianceDict::getDictKey, season + "_"));
        Map<String, String> statMap = new LinkedHashMap<>();
        for (AllianceDict d : stats) {
            statMap.put(d.getDictKey(), d.getDictValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("season", season);
        result.put("groups", grouped);
        result.put("stats", statMap);
        result.put("total", all.size());
        return Result.success(result);
    }

    /** 获取鸣谢单位 */
    @GetMapping("/thanks")
    public Result<List<AllianceThanks>> getThanks() {
        return Result.success(thanksDao.selectList(
            new LambdaQueryWrapper<AllianceThanks>()
                .orderByAsc(AllianceThanks::getSortOrder)));
    }

    /** 通用：按类型获取字典 */
    @GetMapping("/dict/{type}")
    public Result<Map<String, String>> getDict(@PathVariable String type) {
        List<AllianceDict> list = dictDao.selectList(
            new LambdaQueryWrapper<AllianceDict>()
                .eq(AllianceDict::getDictType, type)
                .orderByAsc(AllianceDict::getSortOrder));
        Map<String, String> map = new LinkedHashMap<>();
        for (AllianceDict d : list) {
            map.put(d.getDictKey(), d.getDictValue());
        }
        return Result.success(map);
    }

    /** 获取展示位成员（核心成员 / 名人堂） */
    @GetMapping("/showcase/{section}")
    public Result<List<AllianceShowcase>> getShowcase(@PathVariable String section) {
        return Result.success(showcaseDao.selectList(
            new LambdaQueryWrapper<AllianceShowcase>()
                .eq(AllianceShowcase::getSection, section)
                .eq(AllianceShowcase::getIsActive, 1)
                .orderByAsc(AllianceShowcase::getSortOrder)));
    }
}
