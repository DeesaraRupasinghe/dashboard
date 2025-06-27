package com.example.chartview.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/charts")
public class ChartController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/test")
        public String testEndpoint() {
        return "API working!";
    }

    @GetMapping("/multi-line")
    public Map<String, Object> getMultiTableChart(
            @RequestParam List<Integer> chartIds) {

        List<Map<String, Object>> datasets = new ArrayList<>();
        String xAxisCommon = "";
        List<String> labels = new ArrayList<>();

        for (int chartId : chartIds) {
            String configSql = "SELECT * FROM chart_config WHERE id = ?";
            Map<String, Object> config = jdbcTemplate.queryForMap(configSql, chartId);

            String tableName = config.get("table_name").toString();
            String xAxis = config.get("x_axis").toString();
            String yAxis = config.get("y_axis").toString();
            String chartName = config.get("chart_name").toString();

            if (xAxisCommon.isEmpty()) xAxisCommon = xAxis;

            String dataSql = String.format("SELECT %s, %s FROM %s ORDER BY %s", xAxis, yAxis, tableName, xAxis);
            List<Map<String, Object>> data = jdbcTemplate.queryForList(dataSql);

            List<Object> xValues = new ArrayList<>();
            List<Number> yValues = new ArrayList<>();

            for (Map<String, Object> row : data) {
                xValues.add(row.get(xAxis));
                yValues.add(Double.parseDouble(row.get(yAxis).toString()));
            }

            datasets.add(Map.of(
                    "label", chartName,
                    "data", yValues
            ));

            if (labels.isEmpty()) {
                for (Object x : xValues) {
                    labels.add(x.toString());
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", labels);
        response.put("datasets", datasets);
        return response;
    }



    @GetMapping("/{chartId}")
    public Map<String, Object> getChartData(@PathVariable int chartId) {
        String configSql = "SELECT * FROM chart_config WHERE id = ?";
        Map<String, Object> config = jdbcTemplate.queryForMap(configSql, chartId);

        String tableName = config.get("table_name").toString();
        String xAxis = config.get("x_axis").toString();
        String yAxis = config.get("y_axis").toString();
        String chartType = config.get("chart_type").toString();

        String dataSql = String.format("SELECT %s, %s FROM %s ORDER BY %s", xAxis, yAxis, tableName, xAxis);
        List<Map<String, Object>> data = jdbcTemplate.queryForList(dataSql);

        Map<String, Object> response = new HashMap<>();
        response.put("chartName", config.get("chart_name"));
        response.put("chartType", chartType);
        response.put("xAxis", xAxis);
        response.put("yAxis", yAxis);
        response.put("data", data);

        return response;
    }
}

