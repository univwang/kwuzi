package com.kob.backend.consumer.utils;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private Integer id;
    private Integer color; //0黑 1白
    private List<Integer> steps;
    private Integer botId; //-1亲自出马
    private String botCode;
    public Timer timer;


    public String getStepsString() {
        StringBuilder res = new StringBuilder();
        if(steps == null || steps.size() == 0) {
            return "";
        }
        //间隔符-分割
        for(int d: steps) {
            res.append(d).append("-");
        }
        res.deleteCharAt(res.length() - 1);
        return res.toString();
    }
}
